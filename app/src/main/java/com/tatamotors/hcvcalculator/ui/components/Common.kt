package com.tatamotors.hcvcalculator.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val inLocale = Locale("en", "IN")

/** Rs. 12,34,567 with Indian digit grouping. */
fun inr(value: Double, decimals: Int = 0): String {
    val nf = NumberFormat.getNumberInstance(inLocale)
    nf.maximumFractionDigits = decimals
    nf.minimumFractionDigits = if (decimals > 0) decimals else 0
    return "\u20B9 " + nf.format(value)
}

fun num(value: Double, decimals: Int = 0): String {
    val nf = NumberFormat.getNumberInstance(inLocale)
    nf.maximumFractionDigits = decimals
    return nf.format(value)
}

/** Compact lakh/crore phrasing used in customer talk-tracks. */
fun inrWords(value: Double): String {
    val v = abs(value)
    return when {
        v >= 1_00_00_000 -> "\u20B9 " + num(value / 1_00_00_000, 2) + " Cr"
        v >= 1_00_000 -> "\u20B9 " + num(value / 1_00_000, 2) + " Lakh"
        else -> inr(value.roundToLong().toDouble())
    }
}

fun shareText(context: Context, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

/** Numeric text field bound to a String state; tolerant of partial typing. */
@Composable
fun NumField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    helper: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { txt ->
            if (txt.isEmpty() || txt.matches(Regex("^\\d*\\.?\\d*$"))) onChange(txt)
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, fontSize = 12.sp) } },
        supportingText = helper?.let { { Text(it, fontSize = 11.sp) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, highlight: Boolean = false, valueColor: Color? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (highlight) Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                else Modifier.padding(vertical = 2.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: if (highlight) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Big green hero figure for the customer-facing headline number. */
@Composable
fun HeroFigure(caption: String, amount: Double, sub: String? = null) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(caption, style = MaterialTheme.typography.labelLarge)
            Text(
                inrWords(amount),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                inr(amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            sub?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    onSelect(i); expanded = false
                })
            }
        }
    }
}

/** Customer name / location / route — shared by all calculators. */
@Composable
fun CustomerCard(customer: com.tatamotors.hcvcalculator.data.CustomerInfo) {
    SectionCard("Customer details") {
        OutlinedTextField(
            value = customer.name, onValueChange = { customer.name = it },
            label = { Text("Customer name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = customer.location, onValueChange = { customer.location = it },
                label = { Text("Location") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customer.route, onValueChange = { customer.route = it },
                label = { Text("Route of operation") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Save-estimate button: persists a snapshot for the dashboard. */
@Composable
fun SaveEstimateButton(
    type: String,
    customer: com.tatamotors.hcvcalculator.data.CustomerInfo,
    payload: () -> String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    OutlinedButton(
        onClick = {
            com.tatamotors.hcvcalculator.data.Store.saveEstimate(
                type, customer.name, customer.location, customer.route, payload()
            )
            android.widget.Toast.makeText(context, "Estimate saved to dashboard", android.widget.Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save estimate")
    }
}
