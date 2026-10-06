package es.racionest1d.domain

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class CalculationsTest {
    private fun d(text: String) = BigDecimal(text)
    private fun assertCarbs(expected: String, actual: Nutrition?) = assertEquals(0, d(expected).compareTo(actual!!.carbs))

    @Test fun exampleEquivalences() {
        assertCarbs("10", CarbCalculator.ingredient(d("50"), Reference.GramsPerPortion(d("50"))))
        assertCarbs("15", CarbCalculator.ingredient(d("75"), Reference.GramsPerPortion(d("50"))))
        assertCarbs("20", CarbCalculator.ingredient(d("110"), Reference.GramsPerPortion(d("55"))))
        assertCarbs("5", CarbCalculator.ingredient(d("150"), Reference.GramsPerPortion(d("300"))))
        assertCarbs("5", CarbCalculator.ingredient(d("75"), Reference.GramsPerPortion(d("150"))))
    }

    @Test fun recipePortionsAndMeal() {
        val recipe = CarbCalculator.sum(listOf(
            CarbCalculator.ingredient(d("110"), Reference.GramsPerPortion(d("55"))),
            CarbCalculator.ingredient(d("150"), Reference.GramsPerPortion(d("300"))),
            CarbCalculator.ingredient(d("75"), Reference.GramsPerPortion(d("150")))
        ))
        assertCarbs("30", recipe)
        assertCarbs("15", CarbCalculator.portion(recipe, d("2"), d("1")))
        assertCarbs("7.5", CarbCalculator.byFinishedWeight(recipe, d("400"), d("100")))
        assertCarbs("45", CarbCalculator.sum(listOf(recipe, CarbCalculator.ingredient(d("75"), Reference.GramsPerPortion(d("50"))))))
    }

    @Test fun labelAndUnknown() {
        assertCarbs("12.5", CarbCalculator.ingredient(d("50"), Reference.CarbsPerHundred(d("25"))))
        assertNull(CarbCalculator.ingredient(d("50"), Reference.Missing))
        assertNull(CarbCalculator.sum(listOf(Nutrition(d("10")), null)))
    }

    @Test fun invalidInputAndPreparationState() {
        assertThrows(IllegalArgumentException::class.java) { CarbCalculator.ingredient(d("-1"), Reference.GramsPerPortion(d("50"))) }
        assertThrows(IllegalArgumentException::class.java) { CarbCalculator.ingredient(d("1"), Reference.GramsPerPortion(d("0"))) }
        assertThrows(IllegalArgumentException::class.java) { CarbCalculator.byFinishedWeight(Nutrition(d("10")), null, d("10")) }
        val raw = CarbCalculator.ingredient(d("38"), Reference.GramsPerPortion(d("13")))!!
        val boiled = CarbCalculator.ingredient(d("38"), Reference.GramsPerPortion(d("38")))!!
        assertNotEquals(raw.carbs, boiled.carbs)
    }

    @Test fun historicalSnapshotIsUnaffectedByNewReference() {
        val saved = CarbCalculator.ingredient(d("100"), Reference.GramsPerPortion(d("50")))!!
        val current = CarbCalculator.ingredient(d("100"), Reference.GramsPerPortion(d("40")))!!
        assertCarbs("20", saved)
        assertCarbs("25", current)
    }
}
