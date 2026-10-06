package es.racionest1d.data

import androidx.room.withTransaction
import es.racionest1d.domain.CarbCalculator
import es.racionest1d.domain.Nutrition
import es.racionest1d.domain.Reference
import es.racionest1d.domain.decimal
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

data class DraftIngredient(val ingredient: Ingredient, val amount: String)
data class RecipeSummary(
    val recipe: Recipe,
    val ingredientNames: List<String>,
    val total: Nutrition?,
    val perPortion: Nutrition?,
    val requiresReview: Boolean
)
data class DraftMealItem(
    val kind: String,
    val sourceId: Long?,
    val name: String,
    val mode: String,
    val amount: String,
    val unit: String,
    val carbs: BigDecimal?,
    val source: String,
    val originalCarbs: BigDecimal? = null,
    val usingSavedSnapshot: Boolean = false
)

class RationsRepository(val db: AppDatabase) {
    val ingredients = db.ingredients().observeAll()
    val recipes = db.recipes().observeAll()
    val recipeSummaries = db.recipes().observeAllWithIngredients().combine(ingredients) { entries, available ->
        val availableIds = available.map { it.id }.toSet()
        entries.map { entry ->
            val total = recipeNutrition(entry.ingredients)
            val portions = decimal(entry.recipe.portions)?.takeIf { it > BigDecimal.ZERO }
            RecipeSummary(
                recipe = entry.recipe,
                ingredientNames = entry.ingredients.map { it.nameSnapshot }.distinct(),
                total = total,
                perPortion = if (portions == null) null else CarbCalculator.portion(total, portions, BigDecimal.ONE),
                requiresReview = entry.ingredients.any { line ->
                    line.ingredientId == null || line.ingredientId !in availableIds
                }
            )
        }
    }
    val meals = db.meals().observeAll()

    suspend fun syncCatalog() = db.withTransaction {
        val existing = db.ingredients().builtIns()
        val desired = Seed.ingredients.associateBy { it.name to it.variant }
        val current = existing.size == desired.size && existing.all { row ->
            val expected = desired[row.name to row.variant]
            expected != null && row.sourceVersion == Seed.version &&
                row.gramsPerPortion == expected.gramsPerPortion &&
                row.carbsPerHundred == null && row.unit == "g" &&
                row.dataStatus == expected.dataStatus
        }
        if (!current) {
            val favoriteNames = existing.filter { it.favorite }.map { it.name }.toSet()
            db.ingredients().deleteBuiltIns()
            db.ingredients().insertAll(Seed.ingredients.map { it.copy(favorite = it.name in favoriteNames) })
        }
    }

    fun reference(i: Ingredient): Reference = when {
        i.gramsPerPortion != null -> decimal(i.gramsPerPortion)?.let { Reference.GramsPerPortion(it) } ?: Reference.Missing
        i.carbsPerHundred != null -> decimal(i.carbsPerHundred)?.let { Reference.CarbsPerHundred(it) } ?: Reference.Missing
        else -> Reference.Missing
    }

    fun nutrition(i: Ingredient, amount: String): Nutrition? = decimal(amount)?.let { CarbCalculator.ingredient(it, reference(i)) }

    fun recipeNutrition(lines: List<RecipeIngredient>): Nutrition? = CarbCalculator.sum(lines.map { it.carbsSnapshot?.let(::decimal)?.let(::Nutrition) })

    suspend fun recipeNutrition(recipeId: Long, latestIngredients: Boolean = false): Nutrition? {
        val lines = db.recipes().ingredients(recipeId)
        if (!latestIngredients) return recipeNutrition(lines)
        return CarbCalculator.sum(lines.map { line ->
            val current = line.ingredientId?.let { db.ingredients().get(it) }
            if (current != null) nutrition(current, line.amount)
            else line.carbsSnapshot?.let(::decimal)?.let(::Nutrition)
        })
    }

    suspend fun saveIngredient(i: Ingredient): Long {
        require(i.name.isNotBlank())
        require(i.gramsPerPortion == null || (decimal(i.gramsPerPortion) ?: BigDecimal.ZERO) > BigDecimal.ZERO)
        require(i.carbsPerHundred == null || (decimal(i.carbsPerHundred) ?: BigDecimal(-1)) >= BigDecimal.ZERO)
        return if (i.id == 0L) db.ingredients().insert(i) else { db.ingredients().update(i); i.id }
    }

    suspend fun saveRecipe(recipe: Recipe, draft: List<DraftIngredient>): Long = db.withTransaction {
        require(recipe.name.isNotBlank() && draft.isNotEmpty())
        require((decimal(recipe.portions) ?: BigDecimal.ZERO) > BigDecimal.ZERO)
        require(recipe.finishedWeight == null || (decimal(recipe.finishedWeight) ?: BigDecimal.ZERO) > BigDecimal.ZERO)
        val id = if (recipe.id == 0L) db.recipes().insert(recipe) else {
            db.recipes().update(recipe.copy(updatedAt = System.currentTimeMillis()))
            db.recipes().clearIngredients(recipe.id)
            recipe.id
        }
        db.recipes().insertIngredients(draft.map { (ingredient, amount) ->
            val n = nutrition(ingredient, amount)
            RecipeIngredient(recipeId = id, ingredientId = ingredient.id, nameSnapshot = ingredient.name,
                variantSnapshot = ingredient.variant, amount = amount, unit = ingredient.unit,
                gramsPerPortionSnapshot = ingredient.gramsPerPortion, carbsPerHundredSnapshot = ingredient.carbsPerHundred,
                carbsSnapshot = n?.carbs?.toPlainString(), statusSnapshot = ingredient.dataStatus,
                sourceSnapshot = ingredient.source)
        })
        id
    }

    suspend fun getRecipe(id: Long) = db.recipes().getWithIngredients(id)

    suspend fun duplicateRecipe(id: Long): Long {
        val original = getRecipe(id) ?: error("Plato no encontrado")
        val draft = original.ingredients.map { line ->
            DraftIngredient(db.ingredients().get(line.ingredientId ?: -1) ?: Ingredient(
                name = line.nameSnapshot, category = "Otros", variant = line.variantSnapshot, state = "",
                weightBasis = "", unit = line.unit, gramsPerPortion = line.gramsPerPortionSnapshot,
                carbsPerHundred = line.carbsPerHundredSnapshot, dataStatus = line.statusSnapshot,
                source = line.sourceSnapshot, sourceDate = ""), line.amount)
        }
        return saveRecipe(original.recipe.copy(id = 0, name = original.recipe.name + " (copia)", favorite = false,
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()), draft)
    }

    suspend fun deleteRecipe(id: Long) = db.recipes().delete(id)
    suspend fun deleteMeal(id: Long) = db.meals().delete(id)
    suspend fun getIngredient(id: Long) = db.ingredients().get(id)
    suspend fun getMeal(id: Long) = db.meals().get(id)

    suspend fun mealLine(kind: String, sourceId: Long, mode: String, amount: String, latestIngredients: Boolean = false): DraftMealItem {
        val quantity = decimal(amount) ?: error("Cantidad no válida")
        require(quantity >= BigDecimal.ZERO)
        return if (kind == "INGREDIENT") {
            val i = db.ingredients().get(sourceId) ?: error("Ingrediente no encontrado")
            DraftMealItem(kind, sourceId, "${i.name} · ${i.variant}", "AMOUNT", amount, i.unit,
                nutrition(i, amount)?.carbs, i.source)
        } else {
            val r = db.recipes().get(sourceId) ?: error("Plato no encontrado")
            val total = recipeNutrition(sourceId, latestIngredients)
            val n = if (mode == "GRAMS") CarbCalculator.byFinishedWeight(total, decimal(r.finishedWeight ?: ""), quantity)
                else CarbCalculator.portion(total, decimal(r.portions) ?: BigDecimal.ONE, quantity)
            DraftMealItem(kind, sourceId, r.name, mode, amount, if (mode == "GRAMS") "g" else "porciones",
                n?.carbs, "Plato guardado: ${r.name}")
        }
    }

    suspend fun saveMeal(title: String, notes: String, template: Boolean, lines: List<DraftMealItem>): Long = db.withTransaction {
        require(lines.isNotEmpty())
        val total = CarbCalculator.sum(lines.map { it.carbs?.let(::Nutrition) })
        val id = db.meals().insert(Meal(title = title.ifBlank { if (template) "Plantilla" else "Comida" },
            notes = notes, isTemplate = template, carbsSnapshot = total?.carbs?.toPlainString()))
        db.meals().insertItems(lines.map { line -> MealItem(mealId = id, kind = line.kind,
            sourceId = line.sourceId, nameSnapshot = line.name, mode = line.mode, amount = line.amount,
            unit = line.unit, carbsSnapshot = line.carbs?.toPlainString(), sourceSnapshot = line.source) })
        lines.filter { it.kind == "RECIPE" }.mapNotNull { it.sourceId }.distinct().forEach { recipeId ->
            db.recipes().get(recipeId)?.let { db.recipes().update(it.copy(lastUsedAt = System.currentTimeMillis())) }
        }
        id
    }

    suspend fun repeatMeal(id: Long, updated: Boolean): List<DraftMealItem> {
        val meal = db.meals().get(id) ?: return emptyList()
        return meal.items.map { item ->
            val original = item.carbsSnapshot?.let(::decimal)
            if (!updated || item.sourceId == null) DraftMealItem(item.kind, item.sourceId, item.nameSnapshot,
                item.mode, item.amount, item.unit, original, item.sourceSnapshot, original, true)
            else runCatching { mealLine(item.kind, item.sourceId, item.mode, item.amount, latestIngredients = true).copy(originalCarbs = original) }
                .getOrElse { DraftMealItem(item.kind, item.sourceId, item.nameSnapshot, item.mode,
                    item.amount, item.unit, original, item.sourceSnapshot, original) }
        }
    }
}
