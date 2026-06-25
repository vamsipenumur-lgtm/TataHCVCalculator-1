package com.tatamotors.hcvcalculator.logic

/**
 * FE MaX — FE Series Trucks (Fuel Savings) Calculator.
 * Mirrors FE_Series_FE_MaX.xlsx exactly:
 *   litres/month   = monthlyKm / currentMileage
 *   monthly cost   = litres x dieselPrice
 *   For each improvement p (1%..12%):
 *     newMileage   = currentMileage x (1 + p)
 *     litresSaved  = monthlyKm/currentMileage - monthlyKm/newMileage
 *     save/month   = litresSaved x dieselPrice
 *     save/year    = save/month x 12 ; over N years = x 12 x years
 */
object FeMaxLogic {

    data class Inputs(
        val monthlyKm: Double,
        val currentMileage: Double,
        val dieselPrice: Double,
        val years: Double = 5.0
    )

    data class BaseLine(
        val litresPerMonth: Double,
        val costPerMonth: Double,
        val costPerYear: Double
    )

    data class SavingsRow(
        val improvementPct: Int,
        val newMileage: Double,
        val litresSavedPerMonth: Double,
        val savePerMonth: Double,
        val savePerYear: Double,
        val saveOverYears: Double
    )

    fun baseline(i: Inputs): BaseLine {
        val litres = if (i.currentMileage > 0) i.monthlyKm / i.currentMileage else 0.0
        val monthCost = litres * i.dieselPrice
        return BaseLine(litres, monthCost, monthCost * 12)
    }

    fun savingsTable(i: Inputs, fromPct: Int = 1, toPct: Int = 12): List<SavingsRow> {
        if (i.currentMileage <= 0) return emptyList()
        val baseLitres = i.monthlyKm / i.currentMileage
        return (fromPct..toPct).map { p ->
            val newMileage = i.currentMileage * (1 + p / 100.0)
            val litresSaved = baseLitres - i.monthlyKm / newMileage
            val perMonth = litresSaved * i.dieselPrice
            SavingsRow(
                improvementPct = p,
                newMileage = newMileage,
                litresSavedPerMonth = litresSaved,
                savePerMonth = perMonth,
                savePerYear = perMonth * 12,
                saveOverYears = perMonth * 12 * i.years
            )
        }
    }
}
