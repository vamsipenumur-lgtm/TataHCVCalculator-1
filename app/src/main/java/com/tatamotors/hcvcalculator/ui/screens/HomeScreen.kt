package com.tatamotors.hcvcalculator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatamotors.hcvcalculator.R
import com.tatamotors.hcvcalculator.ui.theme.TataBlue

enum class Screen { HOME, FEMAX, HPT, BRT, DASHBOARD }

private val CARD_HEIGHT = 96.dp

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand header: official lockup on white, campaign strip below
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Image(
                    painter = painterResource(R.drawable.tata_lockup),
                    contentDescription = "Tata Motors Commercial Vehicles - Better Always",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(TataBlue)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        "HCV ValueMax",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                    )
                    Text(
                        "Show your customer the money — fuel savings, extra revenue and full business returns. Fully offline.",
                        color = Color(0xFFD3E6F8), fontSize = 12.sp
                    )
                }
            }
        }

        CalcCard(
            icon = Icons.Default.LocalGasStation,
            title = "FE MaX — FE Series Trucks",
            subtitle = "FE Series: 7–10% better fuel efficiency. Monthly, yearly and multi-year diesel savings.",
            onClick = { onOpen(Screen.FEMAX) }
        )
        CalcCard(
            icon = Icons.Default.TrendingUp,
            title = "Revenue MaX — High Payload Trucks",
            subtitle = "Carry more, earn more. Extra freight revenue from additional payload.",
            onClick = { onOpen(Screen.HPT) }
        )
        CalcCard(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            title = "BRT — Business Return Template",
            subtitle = "Detailed 2-product comparison: operating cost, EMI, profit and the Tata advantage.",
            onClick = { onOpen(Screen.BRT) }
        )
        CalcCard(
            icon = Icons.Default.Folder,
            title = "Saved Estimates",
            subtitle = "Dashboard of all saved customer estimates. Tap any entry to reopen it in its calculator.",
            onClick = { onOpen(Screen.DASHBOARD) }
        )

        Text(
            "Indicative planning tools. Actual results depend on route, load, driver habits and maintenance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun CalcCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, lineHeight = 15.sp
                )
            }
        }
    }
}
