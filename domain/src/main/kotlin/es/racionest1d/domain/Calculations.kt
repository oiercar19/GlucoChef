package es.racionest1d.domain

import java.math.BigDecimal
import java.math.MathContext

private val precision = MathContext.DECIMAL128
private val ten = BigDecimal.TEN
private val hundred = BigDecimal("100")

/** All values are grams, except a product measured in ml, where amount follows that unit. */
data class Nutrition(val carbs: BigDecimal, val portions: BigDecimal = carbs.divide(ten, precision))

sealed interface Reference {
    data class GramsPerPortion(val amount: BigDecimal) : Reference
    data class CarbsPerHundred(val amount: BigDecimal) : Reference
    data object Missing : Reference
}

object CarbCalculator {
    fun ingredient(amount: BigDecimal, reference: Reference): Nutrition? {
        require(amount >= BigDecimal.ZERO) { "La cantidad no puede ser negativa" }
        val carbs = when (reference) {
            is Reference.GramsPerPortion -> {
                require(reference.amount > BigDecimal.ZERO) { "La equivalencia debe ser positiva" }
                amount.divide(reference.amount, precision).multiply(ten)
            }
            is Reference.CarbsPerHundred -> {
                require(reference.amount >= BigDecimal.ZERO) { "Los HC no pueden ser negativos" }
                amount.multiply(reference.amount).divide(hundred, precision)
            }
            Reference.Missing -> return null
        }
        return Nutrition(carbs)
    }

    /** Missing values propagate; an incomplete meal must never look like zero carbohydrate. */
    fun sum(values: List<Nutrition?>): Nutrition? =
        if (values.any { it == null }) null else Nutrition(values.filterNotNull().fold(BigDecimal.ZERO) { total, item -> total + item.carbs })

    fun portion(total: Nutrition?, portionsInRecipe: BigDecimal, portionsConsumed: BigDecimal): Nutrition? {
        require(portionsInRecipe > BigDecimal.ZERO && portionsConsumed >= BigDecimal.ZERO)
        return total?.let { Nutrition(it.carbs.multiply(portionsConsumed).divide(portionsInRecipe, precision)) }
    }

    fun byFinishedWeight(total: Nutrition?, finishedWeight: BigDecimal?, consumedWeight: BigDecimal): Nutrition? {
        require(consumedWeight >= BigDecimal.ZERO)
        require(finishedWeight != null && finishedWeight > BigDecimal.ZERO) { "Registra el peso final comestible" }
        return total?.let { Nutrition(it.carbs.multiply(consumedWeight).divide(finishedWeight, precision)) }
    }
}

fun decimal(value: String): BigDecimal? = value.trim().replace(',', '.').toBigDecimalOrNull()
