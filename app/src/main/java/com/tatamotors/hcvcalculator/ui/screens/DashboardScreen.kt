package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatamotors.hcvcalculator.data.Store
import com.tatamotors.hcvcalculator.ui.theme.WarnRed

private enum class SortMode(val label: String) {
    NEWEST("Newest first"),
    NAME("Customer name"),
    TYPE("Calculator type")
}

/**
 * Dashboard of saved estimates with search (item 15) and sort (item 16).
 * Tapping an entry loads it into its calculator and navigates there directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpen: (Store.Estimate) -> Unit) {
    var all by remember { mutableStateOf(Store.listEstimates()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortMode.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }

    val filtered = remember(all, query, sort) {
        val q = query.trim().lowercase()
        all.filter {
            q.isEmpty() ||
                it.customerName.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.route.lowercase().contains(q) ||
                it.typeLabel.lowercase().contains(q)
        }.let { list ->
            when (sort) {
                SortMode.NEWEST -> list.sortedByDescending { it.savedAt }
                SortMode.NAME -> list.sortedBy { it.customerName.lowercase() }
                SortMode.TYPE -> list.sortedBy { it.typeLabel }
            }
        }
    }

    if (all.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FolderOpen, contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text("No saved estimates yet", fontWeight = FontWeight.Bold)
            Text(
                "Use \u201CSave estimate\u201D inside any calculator and it will appear here for quick access later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Search + sort controls
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search customer, route, type") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty())
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(),
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { sortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label) },
                            trailingIcon = { if (m == sort) Text("\u2713") },
                            onClick = { sort = m; sortMenu = false }
                        )
                    }
                }
            }
        }
        Text(
            "${filtered.size} of ${all.size} estimate" + (if (all.size == 1) "" else "s") + "  •  ${sort.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
        )

        if (filtered.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No matches for \u201C$query\u201D", fontWeight = FontWeight.Bold)
                Text("Try a different name, route or type.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            return
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { e ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(e) },
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        e.typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    e.customerName.ifBlank { "(No customer name)" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                listOf(e.location, e.route).filter { it.isNotBlank() }
                                    .joinToString("  •  ").ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            Text(
                                "Saved: ${e.savedAtText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = {
                            Store.deleteEstimate(e.id)
                            all = Store.listEstimates()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarnRed)
                        }
                    }
                }
            }
        }
    }
}
