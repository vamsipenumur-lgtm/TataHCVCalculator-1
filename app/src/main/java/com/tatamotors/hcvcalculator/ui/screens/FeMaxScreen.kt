package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatamotors.hcvcalculator.data.FeState
import com.tatamotors.hcvcalculator.data.toD
import com.tatamotors.hcvcalculator.logic.FeMaxLogic
import com.tatamotors.hcvcalculator.pdf.buildFePdf
import com.tatamotors.hcvcalculator.ui.components.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FeMaxScreen(state: FeState) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showErrors by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }   // forces recompose after reset

    val years = state.years.toD(5.0)
    val yrsInt = years.roundToInt().coerceAtLeast(1)
    val mileageErr = if (state.mileage.toD() <= 0.0 && state.mileage.isNotEmpty()) "Must be greater than 0" else null
    val inputs = FeMaxLogic.Inputs(state.monthlyKm.toD(), state.mileage.toD(), state.diesel.toD(), years)
    val base = FeMaxLogic.baseline(inputs)
    val table = FeMaxLogic.savingsTable(inputs)
    val selRow = table.find { it.improvementPct == state.selectedPct }

    ResetConfirmDialog(
        show = showReset,
        onConfirm = { state.reset(); showReset = false; tick++
            scope.launch { snackbar.showSnackbar("Calculator reset") } },
        onDismiss = { showReset = false }
    )

    if (showSummary && selRow != null) {
        CustomerSummaryDialog(
            title = "FE Series — Fuel Savings",
            customerName = state.customer.name,
            headlineCaption = "Savings per year at ${selRow.improvementPct}% better FE",
            headlineAmount = selRow.savePerYear,
            stats = listOf(
                SummaryStat("Diesel saved / month", "${num(selRow.litresSavedPerMonth)} L"),
                SummaryStat("Savings / month", inr(selRow.savePerMonth)),
                SummaryStat("Savings / year", inr(selRow.savePerYear), highlight = true),
                SummaryStat("Savings in $yrsInt years", inr(selRow.saveOverYears), highlight = true)
            ),
            onDismiss = { showSummary = false }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(pad)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenActionsRow(
            onReset = { showReset = true },
            onShowCustomer = if (selRow != null) ({ showSummary = true }) else null
        )

        CustomerCard(state.customer, showErrors = showErrors)

        SectionCard("STEP 1 — Vehicle & usage") {
            OutlinedTextField(
                value = state.vehicle, onValueChange = { state.vehicle = it },
                label = { Text("Vehicle / Tonnage") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            NumField("Monthly running", state.monthlyKm, { state.monthlyKm = it }, suffix = "km", grouped = true)
            NumField("Current mileage", state.mileage, { state.mileage = it }, suffix = "km/l", error = mileageErr)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Diesel price", state.diesel, { state.diesel = it }, Modifier.weight(1f), suffix = "\u20B9/l")
                NumField("Years of operation", state.years, { state.years = it }, Modifier.weight(1f), suffix = "yrs")
            }
        }

        SectionCard("STEP 2 — Current monthly fuel cost") {
            ResultRow("Diesel consumed / month", num(base.litresPerMonth, 0) + " L")
            ResultRow("Monthly fuel cost", inr(base.costPerMonth))
            ResultRow("Yearly fuel cost", inr(base.costPerYear), highlight = true)
        }

        SectionCard("STEP 3 — Savings with the new FE Series truck") {
            Text(
                "FE Series delivers 7–10% better fuel efficiency. Slide to show the customer:",
                style = MaterialTheme.typography.bodySmall
            )
            // Discrete 1..12 slider; value snapped via roundToInt so every step (incl. 6% & 8%) is selectable
            Slider(
                value = state.selectedPct.toFloat(),
                onValueChange = { state.selectedPct = it.roundToInt().coerceIn(1, 12) },
                valueRange = 1f..12f,
                steps = 10
            )
            Text(
                "Mileage improvement: ${state.selectedPct}%" +
                        (if (state.selectedPct in 7..10) "  (FE Series range)" else ""),
                fontWeight = FontWeight.SemiBold
            )
            selRow?.let { r ->
                HeroFigure(
                    caption = "Savings per year at ${r.improvementPct}% better FE",
                    amount = r.savePerYear,
                    sub = "${num(r.litresSavedPerMonth, 0)} litres saved every month  •  " +
                            "$yrsInt-year savings: ${inrWords(r.saveOverYears)}"
                )
                ResultRow("New mileage", num(r.newMileage, 2) + " km/l")
                ResultRow("Savings / month", inr(r.savePerMonth))
                ResultRow("Savings in $yrsInt years", inr(r.saveOverYears), highlight = true)
            }
        }

        SectionCard("Full table (1% – 12%)") {
            Row(Modifier.fillMaxWidth()) {
                TableHeader("FE ↑", 0.6f); TableHeader("km/l", 0.8f)
                TableHeader("₹ / month", 1.2f); TableHeader("₹ / year", 1.4f)
                TableHeader("₹ / $yrsInt yrs", 1.4f)
            }
            table.forEach { r ->
                val isSel = r.improvementPct == state.selectedPct
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSel) Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                            else Modifier
                        )
                        .padding(vertical = 3.dp, horizontal = 2.dp)
                ) {
                    TableCell("${r.improvementPct}%", 0.6f, isSel)
                    TableCell(num(r.newMileage, 2), 0.8f, isSel)
                    TableCell(num(r.savePerMonth, 0), 1.2f, isSel)
                    TableCell(num(r.savePerYear, 0), 1.4f, isSel)
                    TableCell(num(r.saveOverYears, 0), 1.4f, isSel)
                }
            }
        }

        SharePreviewButton(
            label = "Share PDF estimate with customer",
            title = "Tata FE Series - Fuel Savings Estimate",
            buildPdf = { buildFePdf(context, state) },
            imageName = "FE_MaX_Estimate.jpg",
            enabled = state.customer.name.isNotBlank(),
            onBlocked = { showErrors = true }
        )

        SaveEstimateButton(
            type = "FEMAX", customer = state.customer,
            onMissingName = { showErrors = true },
            onSaved = { scope.launch { snackbar.showSnackbar("Estimate saved to dashboard") } }
        ) { state.toJson().toString() }

        Text(
            "Note: Actual fuel efficiency depends on road conditions, load, driver habits and maintenance. Indicative planning tool only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableHeader(t: String, w: Float) {
    Text(
        t, modifier = Modifier.weight(w), fontSize = 11.sp,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(t: String, w: Float, bold: Boolean) {
    Text(
        t, modifier = Modifier.weight(w), fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}
