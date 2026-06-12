package com.tatamotors.hcvcalculator.logic

/**
 * Revenue MaX — High Payload Truck revenue advantage.
 * Mirrors HPT_Revenue_MaX.xlsx:
 *   Annual revenue advantage  = additionalPayload x loadedRunningFraction x yearlyKm x freightRatePerTonKm
 *   Lifetime advantage        = annual x totalYears
 *
 * Verified against sheet examples:
 *   30T/37T/44T: 1.8 x 1.0 x 65,000 x 2.5 = Rs. 2,92,500 / yr ; 5 yr = 14,62,500
 *   49T:         1.3 x 1.0 x 65,000 x 2.5 = Rs. 2,11,250 / yr ; 5 yr = 10,56,250
 */
object HptLogic {

    data class TonnagePoint(
        val label: String,          // e.g. "30T"
        val baseModel: String,      // e.g. "28T"
        val additionalPayloadT: Double
    )

    // From the "Drop Down Options" block in the sheet.
    val TONNAGE_POINTS = listOf(
        TonnagePoint("30T", "28T", 1.8),
        TonnagePoint("37T", "35T", 1.8),
        TonnagePoint("44T", "42T", 1.8),
        TonnagePoint("49T", "48T", 1.3)
    )

    data class Inputs(
        val additionalPayloadT: Double,
        val loadedRunningPct: Double,   // 0..100
        val yearlyKm: Double,
        val freightRatePerTonKm: Double,
        val totalYears: Double
    )

    data class Result(
        val annualAdvantage: Double,
        val lifetimeAdvantage: Double
    )

    fun calculate(i: Inputs): Result {
        val annual = i.additionalPayloadT * (i.loadedRunningPct / 100.0) *
                i.yearlyKm * i.freightRatePerTonKm
        return Result(annual, annual * i.totalYears)
    }
}
