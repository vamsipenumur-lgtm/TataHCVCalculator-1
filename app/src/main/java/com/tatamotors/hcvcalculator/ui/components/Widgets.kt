package com.tatamotors.hcvcalculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tatamotors.hcvcalculator.ui.theme.AccentGreen
import com.tatamotors.hcvcalculator.ui.theme.TataBlue
import kotlin.math.max

// ---------------------------------------------------------------- Item 10: collapsible section

/** A SectionCard whose body can be collapsed/expanded by tapping the header. */
@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            }
        }
    }
}

// ---------------------------------------------------------------- Item 21: delta badge

/** Small pill showing the advantage, e.g. "▲ ₹2.15 L better". Green for good, red for worse. */
@Composable
fun DeltaBadge(amountBetter: Double, unit: String = "", good: Boolean = amountBetter >= 0) {
    if (amountBetter == 0.0) return
    val color = if (good) AccentGreen else MaterialTheme.colorScheme.error
    val arrow = if (amountBetter >= 0) "\u25B2" else "\u25BC"
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$arrow ${inrWords(kotlin.math.abs(amountBetter))}$unit",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color
        )
    }
}

// ---------------------------------------------------------------- Item 17: comparison bar chart

data class BarItem(val label: String, val value: Double, val color: Color)

/** Horizontal two-bar (or n-bar) comparison chart, animated. */
@Composable
fun ComparisonBars(title: String, items: List<BarItem>, valueFmt: (Double) -> String = { inrWords(it) }) {
    val maxV = max(1.0, items.maxOf { it.value })
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        items.forEach { it ->
            val frac by animateFloatAsState(
                targetValue = (it.value / maxV).toFloat().coerceIn(0f, 1f),
                label = "bar"
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(valueFmt(it.value), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(it.color)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Item 18: cost-breakdown donut

data class DonutSlice(val label: String, val value: Double, val color: Color)

/** Donut chart with a legend, for showing where the operating cost goes. */
@Composable
fun CostDonut(title: String, slices: List<DonutSlice>) {
    val total = slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.size(120.dp)) {
                var start = -90f
                val stroke = 26.dp.toPx()
                val inset = stroke / 2
                slices.forEach { s ->
                    val sweep = (s.value / total * 360f).toFloat()
                    drawArc(
                        color = s.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                slices.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(s.color))
                        Text(
                            "${s.label}  ${(s.value / total * 100).toInt()}%",
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Item 22: customer summary (presentation mode)

data class SummaryStat(val label: String, val value: String, val highlight: Boolean = false)

/**
 * Full-screen-ish "show the customer" panel: hides inputs, shows only the punchline.
 * Rendered as a dialog so it overlays the calculator cleanly.
 */
@Composable
fun CustomerSummaryDialog(
    title: String,
    customerName: String,
    headlineCaption: String,
    headlineAmount: Double,
    stats: List<SummaryStat>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = TataBlue)
                if (customerName.isNotBlank())
                    Text("for $customerName", style = MaterialTheme.typography.bodyMedium)
                HeroFigure(headlineCaption, headlineAmount)
                stats.forEach { st ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(st.label, fontSize = 14.sp,
                            fontWeight = if (st.highlight) FontWeight.Bold else FontWeight.Normal)
                        Text(st.value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (st.highlight) AccentGreen else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

// ---------------------------------------------------------------- Item 9: reset confirm dialog

@Composable
fun ResetConfirmDialog(show: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset this calculator?") },
        text = { Text("All fields will return to their default values. Saved estimates are not affected.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Reset") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------- Items 23,24,33: share with preview

/**
 * Share button that builds the PDF, shows an in-app preview (item 24) with a loading
 * state (item 33), then lets the rep share as PDF or as image (item 23).
 */
@Composable
fun SharePreviewButton(
    label: String,
    title: String,
    buildPdf: () -> java.io.File?,
    imageName: String,
    enabled: Boolean = true,
    onBlocked: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var file by remember { mutableStateOf<java.io.File?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!enabled) { onBlocked(); return@Button }
            loading = true
            scope.launch {
                val f = withContext(kotlinx.coroutines.Dispatchers.Default) { buildPdf() }
                file = f
                preview = f?.let {
                    com.tatamotors.hcvcalculator.pdf.PdfShare.renderFirstPage(it)?.asImageBitmap()
                }
                loading = false
                showSheet = f != null
            }
        },
        enabled = !loading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("Preparing PDF\u2026")
        } else {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }

    if (showSheet && file != null) {
        Dialog(onDismissRequest = { showSheet = false }) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Preview", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = TataBlue)
                    preview?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "PDF preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                file?.let { com.tatamotors.hcvcalculator.pdf.PdfShare.sharePdf(context, it, title) }
                                showSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Share PDF") }
                        OutlinedButton(
                            onClick = {
                                file?.let { com.tatamotors.hcvcalculator.pdf.PdfShare.shareAsImage(context, it, imageName, title) }
                                showSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Share image") }
                    }
                    TextButton(onClick = { showSheet = false }) { Text("Close") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Item 9 + 22: actions row

/** Small actions row at the top of a calculator: Reset and (optional) Show customer. */
@Composable
fun ScreenActionsRow(onReset: () -> Unit, onShowCustomer: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reset")
        }
        if (onShowCustomer != null) {
            Button(onClick = onShowCustomer, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Show customer")
            }
        }
    }
}
