package es.racionest1d.data

import androidx.room.Room
import es.racionest1d.domain.decimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecipeSummaryTest {
    @Test fun summaryShowsCarbPortionsAndIngredientNames() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = RationsRepository(db)
            val chickpeas = repository.saveIngredient(Ingredient(name = "Garbanzo", category = "Legumbres",
                variant = "Hervido", state = "Hervido", weightBasis = "Peso cocido",
                gramsPerPortion = "55", source = "Prueba", sourceDate = "2026-09-27"))
            val tomato = repository.saveIngredient(Ingredient(name = "Tomate", category = "Hortalizas",
                variant = "Fresco", state = "Crudo", weightBasis = "Peso comestible",
                gramsPerPortion = "300", source = "Prueba", sourceDate = "2026-09-27"))
            repository.saveRecipe(Recipe(name = "Ensalada", portions = "2"), listOf(
                DraftIngredient(db.ingredients().get(chickpeas)!!, "110"),
                DraftIngredient(db.ingredients().get(tomato)!!, "150")
            ))

            val summary = repository.recipeSummaries.first().single()
            assertEquals(listOf("Garbanzo", "Tomate"), summary.ingredientNames)
            assertEquals(0, decimal("2.5")!!.compareTo(summary.total!!.portions))
            assertEquals(0, decimal("1.25")!!.compareTo(summary.perPortion!!.portions))
            assertFalse(summary.requiresReview)
        } finally { db.close() }
    }
}
