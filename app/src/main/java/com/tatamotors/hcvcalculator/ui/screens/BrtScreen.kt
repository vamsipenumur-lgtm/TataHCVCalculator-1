package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatamotors.hcvcalculator.data.BrtState
import com.tatamotors.hcvcalculator.data.ProductState
import com.tatamotors.hcvcalculator.data.TyreGroupState
import com.tatamotors.hcvcalculator.data.toD
import com.tatamotors.hcvcalculator.logic.BrtLogic
import com.tatamotors.hcvcalculator.pdf.shareBrtPdf
import com.tatamotors.hcvcalculator.ui.components.*
import com.tatamotors.hcvcalculator.ui.theme.AccentGreen
import com.tatamotors.hcvcalculator.ui.theme.WarnRed
import kotlin.math.roundToInt

@Composable
fun BrtScreen(state: BrtState) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }

    val comparison = BrtLogic.compare(
        state.product1.toModel(), state.product2.toModel(), state.compareYears.toD(5.0)
    )

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text(state.product1.name.take(14), maxLines = 1, fontSize = 12.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text(state.product2.name.take(14), maxLines = 1, fontSize = 12.sp) })
            Tab(selected = tab == 2, onClick = { tab = 2 },
                text = { Text("RESULTS", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (tab) {
                0 -> ProductForm(state.product1)
                1 -> ProductForm(state.product2)
                else -> ResultsPane(state, comparison) { shareBrtPdf(context, state) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProductForm(p: ProductState) {
    OutlinedTextField(
        value = p.name, onValueChange = { p.name = it },
        label = { Text("Product name") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    SectionCard("Vehicle & finance") {
        NumField("Vehicle price", p.vehiclePrice, { p.vehiclePrice = it }, suffix = "\u20B9")
        NumField("Body price", p.bodyPrice, { p.bodyPrice = it }, suffix = "\u20B9")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Funding", p.fundingPct, { p.fundingPct = it }, Modifier.weight(1f), suffix = "%")
            NumField("Interest p.a.", p.interestPct, { p.interestPct = it }, Modifier.weight(1f), suffix = "%")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Tenure", p.tenureYears, { p.tenureYears = it }, Modifier.weight(1f), suffix = "yrs")
            NumField("Moratorium", p.moratorium, { p.moratorium = it }, Modifier.weight(1f), suffix = "months")
        }
    }

    SectionCard("Route & operations") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Rated payload", p.payload, { p.payload = it }, Modifier.weight(1f), suffix = "T")
            NumField("Trips / month", p.tripsPerMonth, { p.tripsPerMonth = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Primary load", p.primaryLoad, { p.primaryLoad = it }, Modifier.weight(1f), suffix = "T")
            NumField("Return load", p.returnLoad, { p.returnLoad = it }, Modifier.weight(1f), suffix = "T")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Primary lead", p.primaryLead, { p.primaryLead = it }, Modifier.weight(1f), suffix = "km")
            NumField("Return lead", p.returnLead, { p.returnLead = it }, Modifier.weight(1f), suffix = "km")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Empty running / trip", p.emptyKm, { p.emptyKm = it }, Modifier.weight(1f), suffix = "km")
            NumField("Operative months", p.operativeMonths, { p.operativeMonths = it }, Modifier.weight(1f))
        }
    }

    SectionCard("Freight rates") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Primary rate", p.primaryRate, { p.primaryRate = it }, Modifier.weight(1f), suffix = "\u20B9/T-km")
            NumField("Return rate", p.secondaryRate, { p.secondaryRate = it }, Modifier.weight(1f), suffix = "\u20B9/T-km")
        }
    }

    SectionCard("Fuel & DEF") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Trip mileage", p.mileage, { p.mileage = it }, Modifier.weight(1f), suffix = "km/l")
            NumField("Fuel price", p.fuelPrice, { p.fuelPrice = it }, Modifier.weight(1f), suffix = "\u20B9/l")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("DEF consumption", p.defPct, { p.defPct = it }, Modifier.weight(1f), suffix = "% of diesel")
            NumField("DEF cost", p.defCost, { p.defCost = it }, Modifier.weight(1f), suffix = "\u20B9/l")
        }
    }

    SectionCard("Tyres (per axle group)") {
        Text(
            "Add one row per axle group. Tyre cost/km = Σ (count × cost ÷ life). " +
                    "Works for any configuration — rigid trucks, tractor-trailers, multi-axle.",
            style = MaterialTheme.typography.bodySmall
        )
        p.tyreGroups.forEachIndexed { idx, g ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = g.label, onValueChange = { g.label = it },
                            label = { Text("Axle group") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (p.tyreGroups.size > 1) {
                            IconButton(onClick = { p.tyreGroups.removeAt(idx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = WarnRed)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("Tyres", g.count, { g.count = it }, Modifier.weight(0.8f))
                        NumField("Cost/tyre", g.cost, { g.cost = it }, Modifier.weight(1.1f), suffix = "\u20B9")
                        NumField("Life", g.life, { g.life = it }, Modifier.weight(1.1f), suffix = "km")
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { p.tyreGroups.add(TyreGroupState("Trailer axle", "4", "25000", "100000")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add axle group")
        }
        val perKm = BrtLogic.tyreCostPerKm(p.tyreGroups.map { it.toModel() })
        ResultRow("Tyre cost per km", inr(perKm, 2), highlight = true)
    }

    SectionCard("Other running & fixed costs") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Maintenance", p.maintPerKm, { p.maintPerKm = it }, Modifier.weight(1f), suffix = "\u20B9/km")
            NumField("Toll", p.tollPerKm, { p.tollPerKm = it }, Modifier.weight(1f), suffix = "\u20B9/km")
        }
        NumField("Additional expenses per tonne", p.addlPerTon, { p.addlPerTon = it }, suffix = "\u20B9/T")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Crew salary", p.crewSalary, { p.crewSalary = it }, Modifier.weight(1f), suffix = "\u20B9/month")
            NumField("Admin / year", p.adminPerYear, { p.adminPerYear = it }, Modifier.weight(1f), suffix = "\u20B9")
        }
        NumField(
            "Insurance & taxes", p.insurancePct, { p.insurancePct = it },
            suffix = "% of vehicle+body cost", helper = "BRT default: 4%"
        )
    }
}

@Composable
private fun ResultsPane(state: BrtState, c: BrtLogic.Comparison, onSharePdf: () -> Unit) {
    val yrsInt = state.compareYears.toD(5.0).roundToInt().coerceAtLeast(1)
    val p1 = state.product1; val p2 = state.product2
    val tmlWins = c.extraProfitPerYear >= 0

    CustomerCard(state.customer)

    HeroFigure(
        caption = if (tmlWins) "Extra profit per year with ${p1.name}"
        else "Profit gap per year vs ${p2.name}",
        amount = c.extraProfitPerYear,
        sub = "Over $yrsInt years of operation: ${inrWords(c.extraProfitOverYears)}"
    )
    NumField("Years of operation", state.compareYears, { state.compareYears = it }, suffix = "years")

    SectionCard("Side-by-side comparison (per year)") {
        CompareHeader(p1.name, p2.name)
        CompareRow("Initial cost (vehicle + body)", c.product1.initialCost, c.product2.initialCost, lowerBetter = true)
        CompareRow("Distance run / year (km)", c.product1.distancePerYear, c.product2.distancePerYear, isCurrency = false)
        CompareRow("Tons carried / year", c.product1.payloadTonsPerYear, c.product2.payloadTonsPerYear, isCurrency = false)
        CompareRow("Freight earned", c.product1.freightPerYear, c.product2.freightPerYear, bold = true)
        HorizontalDivider()
        CompareRow("Fuel cost", c.product1.fuelCostPerYear, c.product2.fuelCostPerYear, lowerBetter = true)
        CompareRow("DEF cost", c.product1.defCostPerYear, c.product2.defCostPerYear, lowerBetter = true)
        CompareRow("Tyre cost", c.product1.tyreCostPerYear, c.product2.tyreCostPerYear, lowerBetter = true)
        CompareRow("Maintenance", c.product1.maintenanceCostPerYear, c.product2.maintenanceCostPerYear, lowerBetter = true)
        CompareRow("Toll & addl. expenses", c.product1.tollAndAddlPerYear, c.product2.tollAndAddlPerYear, lowerBetter = true)
        CompareRow("Total running cost", c.product1.totalRunningCost, c.product2.totalRunningCost, lowerBetter = true, bold = true)
        HorizontalDivider()
        CompareRow("Fixed cost (crew+ins+admin)", c.product1.totalFixedCost, c.product2.totalFixedCost, lowerBetter = true)
        CompareRow("EMI / year", c.product1.emiPerYear, c.product2.emiPerYear, lowerBetter = true)
        CompareRow("TOTAL OPERATING COST", c.product1.totalOperatingCost, c.product2.totalOperatingCost, lowerBetter = true, bold = true)
        HorizontalDivider()
        CompareRow("Cost per km", c.product1.costPerKm, c.product2.costPerKm, lowerBetter = true, decimals = 2)
        CompareRow("Cost per ton-km", c.product1.costPerTonKm, c.product2.costPerTonKm, lowerBetter = true, decimals = 2)
        HorizontalDivider()
        CompareRow("OPERATING PROFIT / YEAR", c.product1.operatingProfitPerYear, c.product2.operatingProfitPerYear, bold = true)
        CompareRow("Profit / month", c.product1.operatingProfitPerMonth, c.product2.operatingProfitPerMonth)
        Text(
            "Green = better parameter (lower cost / higher revenue & tons). Equal values are not highlighted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }

    Button(onClick = onSharePdf, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Share PDF comparison with customer")
    }

    SaveEstimateButton(type = "BRT", customer = state.customer) { state.toJson().toString() }

    Text(
        "Insurance taken as % of initial cost (BRT default 4%). EMI uses standard reducing-balance PMT over (tenure − moratorium) months. Indicative planning tool.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun CompareHeader(n1: String, n2: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("", Modifier.weight(1.4f))
        Text(n1, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, maxLines = 2)
        Text(n2, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

/**
 * Direction-aware highlighting:
 *  - lowerBetter = true  -> lower value (costs, EMI) shown green
 *  - lowerBetter = false -> higher value (revenue, tons, profit) shown green
 *  - Equal values        -> neither side highlighted
 */
@Composable
private fun CompareRow(
    label: String,
    v1: Double,
    v2: Double,
    isCurrency: Boolean = true,
    lowerBetter: Boolean = false,
    bold: Boolean = false,
    decimals: Int = 0
) {
    val p1Better = v1 != v2 && ((lowerBetter && v1 < v2) || (!lowerBetter && v1 > v2))
    val p2Better = v1 != v2 && !p1Better
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1.4f), fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            if (isCurrency) inr(v1, decimals) else num(v1, decimals),
            Modifier.weight(1f), fontSize = 12.sp,
            fontWeight = if (bold || p1Better) FontWeight.Bold else FontWeight.Normal,
            color = if (p1Better) AccentGreen else MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (isCurrency) inr(v2, decimals) else num(v2, decimals),
            Modifier.weight(1f), fontSize = 12.sp,
            fontWeight = if (bold || p2Better) FontWeight.Bold else FontWeight.Normal,
            color = if (p2Better) AccentGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
