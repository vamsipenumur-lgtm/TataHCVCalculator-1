package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tatamotors.hcvcalculator.data.HptState
import com.tatamotors.hcvcalculator.data.toD
import com.tatamotors.hcvcalculator.logic.HptLogic
import com.tatamotors.hcvcalculator.pdf.buildHptPdf
import com.tatamotors.hcvcalculator.ui.components.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HptScreen(state: HptState) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showErrors by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    val points = HptLogic.TONNAGE_POINTS
    val yrsInt = state.years.toD(5.0).roundToInt().coerceAtLeast(1)
    val loadedErr = if (state.loadedPct.isNotEmpty() && state.loadedPct.toD() > 100.0) "Cannot exceed 100%" else null

    val result = HptLogic.calculate(
        HptLogic.Inputs(
            additionalPayloadT = state.addlPayload.toD(),
            loadedRunningPct = state.loadedPct.toD(),
            yearlyKm = state.yearlyKm.toD(),
            freightRatePerTonKm = state.rate.toD(),
            totalYears = state.years.toD(5.0)
        )
    )
    val point = points[state.tonnageIdx.coerceIn(0, points.size - 1)]

    ResetConfirmDialog(
        show = showReset,
        onConfirm = { state.reset(); showReset = false
            scope.launch { snackbar.showSnackbar("Calculator reset") } },
        onDismiss = { showReset = false }
    )

    if (showSummary) {
        CustomerSummaryDialog(
            title = "High Payload — Revenue Advantage",
            customerName = state.customer.name,
            headlineCaption = "Annual revenue advantage",
            headlineAmount = result.annualAdvantage,
            stats = listOf(
                SummaryStat("Tonnage point", "${point.label} (vs ${point.baseModel})"),
                SummaryStat("Additional payload", "${state.addlPayload.toD()} T"),
                SummaryStat("Annual advantage", inr(result.annualAdvantage), highlight = true),
                SummaryStat("Lifetime ($yrsInt yrs)", inr(result.lifetimeAdvantage), highlight = true)
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
                onShowCustomer = { showSummary = true }
            )

            CustomerCard(state.customer, showErrors = showErrors)

            SectionCard("Vehicle selection") {
                DropdownField(
                    label = "Tonnage point",
                    options = points.map { "${it.label}  (vs base ${it.baseModel}, +${it.additionalPayloadT}T payload)" },
                    selectedIndex = state.tonnageIdx,
                    onSelect = { i ->
                        state.tonnageIdx = i
                        state.addlPayload = points[i].additionalPayloadT.toString()
                    }
                )
                NumField(
                    "Additional payload", state.addlPayload, { state.addlPayload = it },
                    suffix = "tonnes",
                    helper = "Auto-filled from tonnage point (${point.label} vs ${point.baseModel}); editable if needed"
                )
            }

            SectionCard("Operating inputs") {
                NumField("Loaded running", state.loadedPct, { state.loadedPct = it }, suffix = "%",
                    helper = "Share of running done with full load", error = loadedErr)
                NumField("Yearly running", state.yearlyKm, { state.yearlyKm = it }, suffix = "km", grouped = true)
                NumField("Freight rate", state.rate, { state.rate = it }, suffix = "\u20B9 / tonne-km")
                NumField("Years of operation", state.years, { state.years = it }, suffix = "years")
            }

            HeroFigure(
                caption = "Annual revenue advantage",
                amount = result.annualAdvantage,
                sub = "Extra freight earned from +${state.addlPayload.toD()}T payload"
            )
            HeroFigure(
                caption = "Lifetime revenue advantage ($yrsInt years)",
                amount = result.lifetimeAdvantage
            )

            SharePreviewButton(
                label = "Share PDF estimate with customer",
                title = "Tata High Payload Truck - Revenue Advantage",
                buildPdf = { buildHptPdf(context, state) },
                imageName = "Revenue_MaX_Estimate.jpg",
                enabled = state.customer.name.isNotBlank(),
                onBlocked = { showErrors = true }
            )

            SaveEstimateButton(
                type = "HPT", customer = state.customer,
                onMissingName = { showErrors = true },
                onSaved = { scope.launch { snackbar.showSnackbar("Estimate saved to dashboard") } }
            ) { state.toJson().toString() }

            Text(
                "Every extra tonne carried is direct revenue — same trip, same fuel, more earnings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
