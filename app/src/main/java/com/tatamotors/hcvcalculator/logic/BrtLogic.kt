package com.tatamotors.hcvcalculator.logic

import kotlin.math.pow

/**
 * BRT — Business Return Template. Two-product comparison.
 * Mirrors Full_BRT.xlsx with one deliberate generalisation:
 *
 * TYRE COST (generalised):
 *   The Excel hardcodes axle layouts per column, e.g.
 *     C30: =((H21*2)/H23)+(H22*8/H24)                   -> 2 front + 8 drive (10-tyre truck)
 *     E30: =(2*J22/J23)+(4*J22/J24)+(2*J21/J25)+(4*J21/J26)  -> tractor-trailer split
 *   Here each product carries its own list of TyreGroup(count, costPerTyre, lifeKm) so any
 *   axle configuration works:
 *     tyreCostPerKm = SUM over groups of (count x costPerTyre / lifeKm)
 *
 * Everything else follows the sheet:
 *   tripsPerYear        = tripsPerMonth x operativeMonths                       (C13)
 *   distancePerTrip     = primaryLead + returnLead + emptyKm                    (H16)
 *   distancePerYear     = distancePerTrip x tripsPerMonth x operativeMonths     (C24, C25)
 *   tonKmPerYear        = (primaryLead x primaryLoad + returnLead x returnLoad) x trips  (C14)
 *   freightPerYear      = (primaryLoad x primaryLead x primaryRate
 *                          + returnLoad x returnLead x secondaryRate) x trips   (E15 pattern)
 *   payloadTonsPerYear  = (primaryLoad + returnLoad) x trips                    (C23)
 *   fuelLitres          = distancePerYear / mileage ; fuelCost = litres x price (C27)
 *   defCost             = fuelLitres x defPct x defCostPerLitre                 (C28, C29)
 *   maintenanceCost     = maintPerKm x distancePerYear                          (C32)
 *   tollAndAddl         = tollPerKm x distancePerYear + addlPerTon x payloadTons (C33)
 *   fixedCost           = crewSalary x 12 + insurancePct x initialCost + admin  (C18, C19, C20)
 *   EMI (PMT)           = ABS(PMT(rate/12, tenureMonths - moratorium, financed)) x 12 (C35)
 *   operatingProfit     = freight - (running + fixed + EMI)                     (C39)
 */
object BrtLogic {

    data class TyreGroup(
        val label: String,        // e.g. "Front axle", "Drive axle", "Trailer tandem"
        val count: Int,           // number of tyres in this group
        val costPerTyre: Double,  // Rs per new tyre
        val lifeKm: Double        // life of this group's tyres in km
    )

    data class ProductInputs(
        val name: String,
        // Vehicle & finance
        val vehiclePrice: Double,
        val bodyPrice: Double,
        val fundingPct: Double,        // 0..100 (% of initial cost financed)
        val interestPctPa: Double,     // 0..100 annual flat reducing rate used by PMT
        val tenureYears: Double,
        val moratoriumMonths: Double,
        // Operations
        val payloadTons: Double,
        val primaryLoadTons: Double,
        val returnLoadTons: Double,
        val primaryLeadKm: Double,
        val returnLeadKm: Double,
        val emptyKm: Double,
        val tripsPerMonth: Double,
        val operativeMonths: Double,
        // Revenue
        val primaryRatePerTonKm: Double,
        val secondaryRatePerTonKm: Double,
        // Fuel & DEF
        val mileageKmpl: Double,
        val fuelPricePerLitre: Double,
        val defConsumptionPct: Double, // 0..100 (% of diesel volume)
        val defCostPerLitre: Double,
        // Other running costs
        val maintenancePerKm: Double,
        val tollPerKm: Double,
        val addlExpensePerTon: Double,
        // Fixed costs
        val crewSalaryPerMonth: Double,
        val adminExpensesPerYear: Double,
        val insurancePctOfCost: Double, // sheet uses 4%
        // Tyres — generalised
        val tyreGroups: List<TyreGroup>
    )

    data class ProductResult(
        val initialCost: Double,
        val financeAmount: Double,
        val tripsPerYear: Double,
        val distancePerTrip: Double,
        val distancePerMonth: Double,
        val distancePerYear: Double,
        val tonKmPerYear: Double,
        val payloadTonsPerYear: Double,
        val freightPerYear: Double,
        val freightPerTrip: Double,
        // Fixed
        val crewSalaryPerYear: Double,
        val insurancePerYear: Double,
        val adminPerYear: Double,
        val totalFixedCost: Double,
        // Running
        val fuelLitresPerYear: Double,
        val fuelCostPerYear: Double,
        val defLitresPerYear: Double,
        val defCostPerYear: Double,
        val tyreCostPerKm: Double,
        val tyreCostPerYear: Double,
        val maintenanceCostPerYear: Double,
        val tollAndAddlPerYear: Double,
        val totalRunningCost: Double,
        // Finance & totals
        val emiPerMonth: Double,
        val emiPerYear: Double,
        val totalOperatingCost: Double,
        val costPerKm: Double,
        val costPerTonKm: Double,
        val operatingProfitPerYear: Double,
        val operatingProfitPerMonth: Double
    )

    data class Comparison(
        val product1: ProductResult,
        val product2: ProductResult,
        val extraProfitPerYear: Double,    // product1 - product2 (TML vs competition)
        val extraProfitPerMonth: Double,
        val extraProfitOverYears: Double,
        val years: Double
    )

    /** Standard annuity EMI; with zero interest falls back to straight division. */
    fun emi(principal: Double, annualRatePct: Double, months: Double): Double {
        if (months <= 0) return 0.0
        val r = annualRatePct / 100.0 / 12.0
        if (r == 0.0) return principal / months
        val f = (1 + r).pow(months)
        return principal * r * f / (f - 1)
    }

    fun tyreCostPerKm(groups: List<TyreGroup>): Double =
        groups.sumOf { g -> if (g.lifeKm > 0) g.count * g.costPerTyre / g.lifeKm else 0.0 }

    fun calculate(p: ProductInputs): ProductResult {
        val initialCost = p.vehiclePrice + p.bodyPrice
        val financeAmount = initialCost * (p.fundingPct / 100.0)
        val tripsPerYear = p.tripsPerMonth * p.operativeMonths
        val distPerTrip = p.primaryLeadKm + p.returnLeadKm + p.emptyKm
        val distPerMonth = distPerTrip * p.tripsPerMonth
        val distPerYear = distPerMonth * p.operativeMonths
        val tonKm = (p.primaryLeadKm * p.primaryLoadTons + p.returnLeadKm * p.returnLoadTons) * tripsPerYear
        val payloadTonsYr = (p.primaryLoadTons + p.returnLoadTons) * tripsPerYear
        val freightYr = (p.primaryLoadTons * p.primaryLeadKm * p.primaryRatePerTonKm +
                p.returnLoadTons * p.returnLeadKm * p.secondaryRatePerTonKm) * tripsPerYear

        val crewYr = p.crewSalaryPerMonth * 12
        val insuranceYr = initialCost * (p.insurancePctOfCost / 100.0)
        val fixed = crewYr + insuranceYr + p.adminExpensesPerYear

        val litres = if (p.mileageKmpl > 0) distPerYear / p.mileageKmpl else 0.0
        val fuelCost = litres * p.fuelPricePerLitre
        val defLitres = litres * (p.defConsumptionPct / 100.0)
        val defCost = defLitres * p.defCostPerLitre
        val tyrePerKm = tyreCostPerKm(p.tyreGroups)
        val tyreYr = tyrePerKm * distPerYear
        val maintYr = p.maintenancePerKm * distPerYear
        val tollAddl = p.tollPerKm * distPerYear + p.addlExpensePerTon * payloadTonsYr
        val running = fuelCost + defCost + tyreYr + maintYr + tollAddl

        val tenureMonths = (p.tenureYears * 12 - p.moratoriumMonths).coerceAtLeast(0.0)
        val emiMonth = emi(financeAmount, p.interestPctPa, tenureMonths)
        val emiYear = emiMonth * 12

        val totalOp = running + fixed + emiYear
        val profit = freightYr - totalOp

        return ProductResult(
            initialCost = initialCost,
            financeAmount = financeAmount,
            tripsPerYear = tripsPerYear,
            distancePerTrip = distPerTrip,
            distancePerMonth = distPerMonth,
            distancePerYear = distPerYear,
            tonKmPerYear = tonKm,
            payloadTonsPerYear = payloadTonsYr,
            freightPerYear = freightYr,
            freightPerTrip = if (tripsPerYear > 0) freightYr / tripsPerYear else 0.0,
            crewSalaryPerYear = crewYr,
            insurancePerYear = insuranceYr,
            adminPerYear = p.adminExpensesPerYear,
            totalFixedCost = fixed,
            fuelLitresPerYear = litres,
            fuelCostPerYear = fuelCost,
            defLitresPerYear = defLitres,
            defCostPerYear = defCost,
            tyreCostPerKm = tyrePerKm,
            tyreCostPerYear = tyreYr,
            maintenanceCostPerYear = maintYr,
            tollAndAddlPerYear = tollAddl,
            totalRunningCost = running,
            emiPerMonth = emiMonth,
            emiPerYear = emiYear,
            totalOperatingCost = totalOp,
            costPerKm = if (distPerYear > 0) totalOp / distPerYear else 0.0,
            costPerTonKm = if (tonKm > 0) totalOp / tonKm else 0.0,
            operatingProfitPerYear = profit,
            operatingProfitPerMonth = profit / 12
        )
    }

    fun compare(p1: ProductInputs, p2: ProductInputs, years: Double): Comparison {
        val r1 = calculate(p1)
        val r2 = calculate(p2)
        val extraYr = r1.operatingProfitPerYear - r2.operatingProfitPerYear
        return Comparison(
            product1 = r1,
            product2 = r2,
            extraProfitPerYear = extraYr,
            extraProfitPerMonth = extraYr / 12,
            extraProfitOverYears = extraYr * years,
            years = years
        )
    }

    /** Sensible starting point for a 10-tyre rigid truck (matches the sheet's C/D columns). */
    fun defaultTyreGroups(): List<TyreGroup> = listOf(
        TyreGroup("Front axle", 2, 25000.0, 100000.0),
        TyreGroup("Drive/Rear axles", 8, 25000.0, 100000.0)
    )
}
