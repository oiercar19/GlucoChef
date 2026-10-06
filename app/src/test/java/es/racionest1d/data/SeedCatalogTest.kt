package es.racionest1d.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SeedCatalogTest {
    @Test fun catalogContainsAllUserPortionGroups() {
        val entries = Seed.ingredients
        assertEquals(66, entries.size)
        assertEquals(
            mapOf("15" to 10, "20" to 7, "30" to 1, "45" to 2, "50" to 6,
                "100" to 13, "150" to 3, "200" to 6, "250" to 4, "300" to 14),
            entries.groupingBy { it.gramsPerPortion }.eachCount()
        )
        assertEquals(entries.size, entries.map { it.name to it.variant }.toSet().size)
        assertTrue(entries.all {
            it.unit == "g" && it.carbsPerHundred == null &&
                BigDecimal(it.gramsPerPortion!!) > BigDecimal.ZERO
        })
        assertEquals("15", entries.single { it.name == "Arroz" && it.variant == "Crudo" }.gramsPerPortion)
        assertEquals("45", entries.single { it.name == "Arroz" && it.variant == "Cocido" }.gramsPerPortion)
        assertEquals("100", entries.single { it.name == "Soja" && it.variant == "Cruda" }.gramsPerPortion)
        assertEquals("200", entries.single { it.name == "Soja" && it.variant == "Cocida" }.gramsPerPortion)
        assertEquals("250", entries.single { it.name == "Leche" }.gramsPerPortion)
    }

    @Test fun updateReplacesOldCatalogAndKeepsPersonalDataAndHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val oldId = db.ingredients().insert(Ingredient(
                name = "Tomate", category = "Hortalizas", variant = "Fresco",
                state = "Crudo", weightBasis = "Peso comestible", gramsPerPortion = "250",
                source = "Catálogo anterior", sourceDate = "2026-09-27", favorite = true
            ))
            val customId = db.ingredients().insert(Ingredient(
                name = "Mi alimento", category = "Otros", variant = "Casa",
                state = "Preparado", weightBasis = "Peso comestible", source = "Usuario",
                sourceDate = "", custom = true, carbsPerHundred = "12"
            ))
            val recipeId = db.recipes().insert(Recipe(name = "Mi plato", portions = "1"))
            db.recipes().insertIngredients(listOf(RecipeIngredient(
                recipeId = recipeId, ingredientId = oldId, nameSnapshot = "Tomate",
                variantSnapshot = "Fresco", amount = "250", unit = "g",
                gramsPerPortionSnapshot = "250", carbsPerHundredSnapshot = null,
                carbsSnapshot = "10", statusSnapshot = "VERIFIED", sourceSnapshot = "Catálogo anterior"
            )))
            val mealId = db.meals().insert(Meal(title = "Mi comida", carbsSnapshot = "10"))
            db.meals().insertItems(listOf(MealItem(
                mealId = mealId, kind = "INGREDIENT", sourceId = oldId,
                nameSnapshot = "Tomate", mode = "AMOUNT", amount = "250", unit = "g",
                carbsSnapshot = "10", sourceSnapshot = "Catálogo anterior"
            )))

            val repository = RationsRepository(db)
            repository.syncCatalog()
            assertEquals(67, db.ingredients().count())
            assertNull(db.ingredients().get(oldId))
            assertEquals("12", db.ingredients().get(customId)?.carbsPerHundred)
            assertEquals("300", db.ingredients().builtIns().single { it.name == "Tomate" }.gramsPerPortion)
            assertTrue(db.ingredients().builtIns().single { it.name == "Tomate" }.favorite)
            assertNotNull(db.recipes().get(recipeId))
            assertNotNull(db.meals().get(mealId))
            assertEquals(0, BigDecimal("10").compareTo(repository.recipeNutrition(recipeId, true)!!.carbs))
            assertTrue(repository.recipeSummaries.first().single().requiresReview)

            val newTomatoId = db.ingredients().builtIns().single { it.name == "Tomate" }.id
            repository.syncCatalog()
            assertEquals(67, db.ingredients().count())
            assertEquals(newTomatoId, db.ingredients().builtIns().single { it.name == "Tomate" }.id)
        } finally { db.close() }
    }
}
