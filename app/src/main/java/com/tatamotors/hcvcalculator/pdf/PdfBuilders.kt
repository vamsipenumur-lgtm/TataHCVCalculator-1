package com.tatamotors.hcvcalculator.pdf

import android.content.Context
import com.tatamotors.hcvcalculator.R
import com.tatamotors.hcvcalculator.data.BrtState
import com.tatamotors.hcvcalculator.data.FeState
import com.tatamotors.hcvcalculator.data.HptState
import com.tatamotors.hcvcalculator.data.toD
import com.tatamotors.hcvcalculator.logic.BrtLogic
import com.tatamotors.hcvcalculator.logic.FeMaxLogic
import com.tatamotors.hcvcalculator.logic.HptLogic
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val inLocale = Locale("en", "IN")
private fun inr(v: Double, dec: Int = 0): String {
    val nf = NumberFormat.getNumberInstance(inLocale)
    nf.maximumFractionDigits = dec
    return "Rs " + nf.format(v)
}
private fun num(v: Double, dec: Int = 0): String {
    val nf = NumberFormat.getNumberInstance(inLocale)
    nf.maximumFractionDigits = dec
    return nf.format(v)
}
private fun inrWords(v: Double): String {
    val a = abs(v)
    return when {
        a >= 1_00_00_000 -> "Rs " + num(v / 1_00_00_000, 2) + " Cr"
        a >= 1_00_000 -> "Rs " + num(v / 1_00_000, 2) + " Lakh"
        else -> inr(v)
    }
}
private fun today(): String = SimpleDateFormat("dd MMM yyyy", inLocale).format(Date())

private fun customerPairs(name: String, location: String, route: String, extra: List<Pair<String, String>> = emptyList()) =
    buildList {
        add("Customer name" to name.ifBlank { "—" })
        add("Location" to location.ifBlank { "—" })
        add("Route of operation" to route.ifBlank { "—" })
        add("Date" to today())
        addAll(extra)
    }

/** FE MaX — themed on "Go Further with Every Drop" (FE Series creative). */
fun shareFePdf(context: Context, s: FeState) {
    val inputs = FeMaxLogic.Inputs(s.monthlyKm.toD(), s.mileage.toD(), s.diesel.toD(), s.years.toD(5.0))
    val base = FeMaxLogic.baseline(inputs)
    val table = FeMaxLogic.savingsTable(inputs)
    val sel = table.find { it.improvementPct == s.selectedPct } ?: return
    val yrs = inputs.years.toInt()

    val pdf = PdfShare.Pdf(context)
    pdf.startPage()
    pdf.taglineBand(
        "Go Further with Every Drop",
        "FE Series Trucks  |  7-10% Better Fuel-Efficiency  |  Next-Gen Turbocharger  |  Aerodynamic Cabin"
    )
    pdf.bannerImage(R.drawable.pdf_banner_fe, maxHeight = 150f)
    pdf.sectionTitle("Customer details")
    pdf.kvBlock(
        customerPairs(
            s.customer.name, s.customer.location, s.customer.route,
            listOf(
                "Vehicle / Tonnage" to s.vehicle,
                "Monthly running" to num(inputs.monthlyKm) + " km",
                "Current mileage" to num(inputs.currentMileage, 2) + " km/l",
                "Diesel price" to inr(inputs.dieselPrice) + " / litre",
                "Years of operation" to "$yrs years"
            )
        )
    )
    pdf.sectionTitle("Current fuel cost")
    pdf.table(
        headers = listOf("Diesel consumed / month", "Monthly fuel cost", "Yearly fuel cost"),
        rows = listOf(listOf(num(base.litresPerMonth) + " L", inr(base.costPerMonth), inr(base.costPerYear))),
        colWeights = listOf(0.34f, 0.33f, 0.33f)
    )
    pdf.heroBand(
        "Savings at ${sel.improvementPct}% better fuel efficiency",
        inrWords(sel.savePerYear) + " every year",
        "${num(sel.litresSavedPerMonth)} litres saved/month  |  ${inr(sel.savePerMonth)}/month  |  " +
                "$yrs-year savings: ${inrWords(sel.saveOverYears)}"
    )
    pdf.sectionTitle("Savings table (1% - 12% FE improvement)")
    pdf.table(
        headers = listOf("FE improvement", "New mileage (km/l)", "Saved L/month", "Savings / month", "Savings / year", "Savings / $yrs yrs"),
        rows = table.map {
            listOf("${it.improvementPct}%", num(it.newMileage, 2), num(it.litresSavedPerMonth),
                inr(it.savePerMonth), inr(it.savePerYear), inr(it.saveOverYears))
        },
        colWeights = listOf(0.13f, 0.16f, 0.14f, 0.19f, 0.19f, 0.19f),
        boldRows = setOf(table.indexOfFirst { it.improvementPct == sel.improvementPct })
    )
    pdf.note("FE Series delivers 7-10% better fuel efficiency vs MY2025 and earlier models. T&C apply.")
    pdf.finishAndShare("FE_MaX_Estimate.pdf", "Tata FE Series - Fuel Savings Estimate")
}

/** Revenue MaX — themed on "Carry More. Earn More" (Higher Payload Trucks creative). */
fun shareHptPdf(context: Context, s: HptState) {
    val point = HptLogic.TONNAGE_POINTS[s.tonnageIdx.coerceIn(0, HptLogic.TONNAGE_POINTS.size - 1)]
    val res = HptLogic.calculate(
        HptLogic.Inputs(s.addlPayload.toD(), s.loadedPct.toD(), s.yearlyKm.toD(), s.rate.toD(), s.years.toD(5.0))
    )
    val yrs = s.years.toD(5.0).toInt()

    val pdf = PdfShare.Pdf(context)
    pdf.startPage()
    pdf.taglineBand(
        "Carry More. Earn More",
        "Higher Payload Trucks  |  Designed for versatile loads. Enhanced for maximum capacity."
    )
    pdf.bannerImage(R.drawable.pdf_banner_hpt, maxHeight = 110f)
    pdf.sectionTitle("Customer details")
    pdf.kvBlock(
        customerPairs(
            s.customer.name, s.customer.location, s.customer.route,
            listOf("Tonnage point" to "${point.label} (vs base ${point.baseModel})")
        )
    )
    pdf.sectionTitle("Revenue advantage calculation")
    pdf.table(
        headers = listOf("Parameter", "Value"),
        rows = listOf(
            listOf("Additional payload", "${s.addlPayload.toD()} tonnes"),
            listOf("Loaded running", "${s.loadedPct.toD()} %"),
            listOf("Yearly running", num(s.yearlyKm.toD()) + " km"),
            listOf("Freight rate", "Rs ${s.rate.toD()} per tonne-km"),
            listOf("Years of operation", "$yrs years")
        ),
        colWeights = listOf(0.5f, 0.5f)
    )
    pdf.heroBand(
        "Annual revenue advantage",
        inrWords(res.annualAdvantage) + " / year",
        "Lifetime advantage over $yrs years: ${inrWords(res.lifetimeAdvantage)}  (${inr(res.lifetimeAdvantage)})"
    )
    pdf.note("Every extra tonne carried is direct revenue - same trip, same route, more earnings.")
    pdf.finishAndShare("Revenue_MaX_Estimate.pdf", "Tata High Payload Truck - Revenue Advantage")
}

/** BRT — themed on "Ab Profit Hoga Aur Bhi Zyaada" (profit creative). */
fun shareBrtPdf(context: Context, s: BrtState) {
    val c = BrtLogic.compare(s.product1.toModel(), s.product2.toModel(), s.compareYears.toD(5.0))
    val r1 = c.product1; val r2 = c.product2
    val n1 = s.product1.name; val n2 = s.product2.name
    val yrs = c.years.toInt()

    val pdf = PdfShare.Pdf(context)
    pdf.startPage()
    pdf.taglineBand(
        "Ab Profit Hoga Aur Bhi Zyaada",
        "Business Return Template  |  A practical approach to make transport business decisions"
    )
    pdf.bannerImage(R.drawable.pdf_banner_brt, maxHeight = 110f)
    pdf.sectionTitle("Customer details")
    pdf.kvBlock(customerPairs(s.customer.name, s.customer.location, s.customer.route))

    val winnerName = if (c.extraProfitPerYear >= 0) n1 else n2
    pdf.heroBand(
        "Extra operating profit per year with $winnerName",
        inrWords(abs(c.extraProfitPerYear)) + " / year",
        "Over $yrs years: ${inrWords(abs(c.extraProfitOverYears))}  (${inr(abs(c.extraProfitOverYears))})"
    )

    // green cell: 1 = product1 better, 2 = product2 better; equal -> none
    fun better(v1: Double, v2: Double, lowerBetter: Boolean): Int =
        when {
            v1 == v2 -> -1
            (lowerBetter && v1 < v2) || (!lowerBetter && v1 > v2) -> 1
            else -> 2
        }

    data class Row(val label: String, val v1: Double, val v2: Double,
                   val lowerBetter: Boolean, val currency: Boolean = true,
                   val bold: Boolean = false, val dec: Int = 0)

    val rows = listOf(
        Row("Initial cost (vehicle + body)", r1.initialCost, r2.initialCost, lowerBetter = true),
        Row("Distance run / year (km)", r1.distancePerYear, r2.distancePerYear, lowerBetter = false, currency = false),
        Row("Tons carried / year", r1.payloadTonsPerYear, r2.payloadTonsPerYear, lowerBetter = false, currency = false),
        Row("Freight earned / year", r1.freightPerYear, r2.freightPerYear, lowerBetter = false, bold = true),
        Row("Fuel cost / year", r1.fuelCostPerYear, r2.fuelCostPerYear, lowerBetter = true),
        Row("DEF cost / year", r1.defCostPerYear, r2.defCostPerYear, lowerBetter = true),
        Row("Tyre cost / year", r1.tyreCostPerYear, r2.tyreCostPerYear, lowerBetter = true),
        Row("Maintenance / year", r1.maintenanceCostPerYear, r2.maintenanceCostPerYear, lowerBetter = true),
        Row("Toll & addl. expenses / year", r1.tollAndAddlPerYear, r2.tollAndAddlPerYear, lowerBetter = true),
        Row("Total running cost / year", r1.totalRunningCost, r2.totalRunningCost, lowerBetter = true, bold = true),
        Row("Fixed cost / year", r1.totalFixedCost, r2.totalFixedCost, lowerBetter = true),
        Row("EMI / year", r1.emiPerYear, r2.emiPerYear, lowerBetter = true),
        Row("TOTAL OPERATING COST / year", r1.totalOperatingCost, r2.totalOperatingCost, lowerBetter = true, bold = true),
        Row("Cost per km", r1.costPerKm, r2.costPerKm, lowerBetter = true, dec = 2),
        Row("Cost per ton-km", r1.costPerTonKm, r2.costPerTonKm, lowerBetter = true, dec = 2),
        Row("OPERATING PROFIT / YEAR", r1.operatingProfitPerYear, r2.operatingProfitPerYear, lowerBetter = false, bold = true),
        Row("Operating profit / month", r1.operatingProfitPerMonth, r2.operatingProfitPerMonth, lowerBetter = false)
    )

    val greenMap = HashMap<Int, Int>()
    rows.forEachIndexed { i, row ->
        when (better(row.v1, row.v2, row.lowerBetter)) {
            1 -> greenMap[i] = 1
            2 -> greenMap[i] = 2
        }
    }

    pdf.sectionTitle("Side-by-side business comparison")
    pdf.table(
        headers = listOf("Parameter", n1, n2),
        rows = rows.map { row ->
            listOf(
                row.label,
                if (row.currency) inr(row.v1, row.dec) else num(row.v1, row.dec),
                if (row.currency) inr(row.v2, row.dec) else num(row.v2, row.dec)
            )
        },
        colWeights = listOf(0.46f, 0.27f, 0.27f),
        boldRows = rows.mapIndexedNotNull { i, r -> if (r.bold) i else null }.toSet(),
        greenCellPerRow = greenMap
    )

    pdf.note("Green values indicate the better parameter (lower cost / higher revenue & tons). Equal values are not highlighted.")
    pdf.note("EMI: reducing-balance PMT over (tenure - moratorium) months. Insurance taken as % of initial cost.")
    pdf.finishAndShare("BRT_Comparison.pdf", "Tata Motors - Business Return Comparison")
}
