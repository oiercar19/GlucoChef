package es.racionest1d.data

import androidx.room.Room
import es.racionest1d.domain.decimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistorySnapshotTest {
    @Test fun oldMealStaysFixedWhenIngredientAndRecipeChange() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = RationsRepository(db)
            val bananaId = repository.saveIngredient(Ingredient(name = "Plátano", category = "Fruta",
                variant = "Crudo", state = "Crudo", weightBasis = "Peso comestible",
                gramsPerPortion = "50", source = "Prueba", sourceDate = "2026-09-27"))
            val banana = db.ingredients().get(bananaId)!!
            val recipeId = repository.saveRecipe(Recipe(name = "Plátano", portions = "1"),
                listOf(DraftIngredient(banana, "50")))
            val mealItem = repository.mealLine("RECIPE", recipeId, "PORTIONS", "1")
            val mealId = repository.saveMeal("Desayuno", "", false, listOf(mealItem))
            assertEquals(0, decimal("10")!!.compareTo(decimal(db.meals().get(mealId)!!.meal.carbsSnapshot!!)!!))

            repository.saveIngredient(banana.copy(gramsPerPortion = "40"))
            assertEquals(0, decimal("10")!!.compareTo(repository.repeatMeal(mealId, false).single().carbs))
            assertEquals(0, decimal("12.5")!!.compareTo(repository.repeatMeal(mealId, true).single().carbs))

            repository.saveRecipe(db.recipes().get(recipeId)!!.copy(portions = "2"),
                listOf(DraftIngredient(db.ingredients().get(bananaId)!!, "50")))
            assertEquals(0, decimal("10")!!.compareTo(decimal(db.meals().get(mealId)!!.meal.carbsSnapshot!!)!!))
            assertEquals(0, decimal("6.25")!!.compareTo(repository.repeatMeal(mealId, true).single().carbs))
        } finally { db.close() }
    }
}
