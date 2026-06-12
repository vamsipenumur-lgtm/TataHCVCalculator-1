package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tatamotors.hcvcalculator.data.HptState
import com.tatamotors.hcvcalculator.data.toD
import com.tatamotors.hcvcalculator.logic.HptLogic
import com.tatamotors.hcvcalculator.pdf.shareHptPdf
import com.tatamotors.hcvcalculator.ui.components.*
import kotlin.math.roundToInt

@Composable
fun HptScreen(state: HptState) {
    val context = LocalContext.current
    val points = HptLogic.TONNAGE_POINTS
    val yrsInt = state.years.toD(5.0).roundToInt().coerceAtLeast(1)

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CustomerCard(state.customer)

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
                helper = "Share of running done with full load")
            NumField("Yearly running", state.yearlyKm, { state.yearlyKm = it }, suffix = "km")
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

        Button(
            onClick = { shareHptPdf(context, state) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share PDF estimate with customer")
        }

        SaveEstimateButton(type = "HPT", customer = state.customer) { state.toJson().toString() }

        Text(
            "Every extra tonne carried is direct revenue — same trip, same fuel, more earnings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
