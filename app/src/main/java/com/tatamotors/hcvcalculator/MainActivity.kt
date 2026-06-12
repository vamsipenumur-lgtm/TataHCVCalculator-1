package com.tatamotors.hcvcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tatamotors.hcvcalculator.data.BrtState
import com.tatamotors.hcvcalculator.data.FeState
import com.tatamotors.hcvcalculator.data.HptState
import com.tatamotors.hcvcalculator.data.Store
import com.tatamotors.hcvcalculator.ui.screens.*
import com.tatamotors.hcvcalculator.ui.theme.TataHCVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent {
            TataHCVTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    // Calculator states live at app level. Drafts auto-restore the last entered
    // values even when the app was closed without saving.
    val feState = remember { FeState().also { s -> Store.loadDraft("fe")?.let(s::fromJson) } }
    val hptState = remember { HptState().also { s -> Store.loadDraft("hpt")?.let(s::fromJson) } }
    val brtState = remember { BrtState().also { s -> Store.loadDraft("brt")?.let(s::fromJson) } }

    // Auto-save drafts whenever anything changes (reads inside snapshotFlow track all fields).
    LaunchedEffect(Unit) {
        snapshotFlow { feState.toJson().toString() }.collect { Store.saveDraft("fe", it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { hptState.toJson().toString() }.collect { Store.saveDraft("hpt", it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { brtState.toJson().toString() }.collect { Store.saveDraft("brt", it) }
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (screen) {
                            Screen.HOME -> "Tata Motors CV | Better Always"
                            Screen.FEMAX -> "FE MaX — Fuel Savings"
                            Screen.HPT -> "Revenue MaX — High Payload"
                            Screen.BRT -> "BRT — Business Returns"
                            Screen.DASHBOARD -> "Saved Estimates"
                        }
                    )
                },
                navigationIcon = {
                    if (screen != Screen.HOME) {
                        IconButton(onClick = { screen = Screen.HOME }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                Screen.HOME -> HomeScreen(onOpen = { screen = it })
                Screen.FEMAX -> FeMaxScreen(feState)
                Screen.HPT -> HptScreen(hptState)
                Screen.BRT -> BrtScreen(brtState)
                Screen.DASHBOARD -> DashboardScreen(onOpen = { est ->
                    when (est.type) {
                        "FEMAX" -> { feState.fromJson(est.payload); screen = Screen.FEMAX }
                        "HPT" -> { hptState.fromJson(est.payload); screen = Screen.HPT }
                        "BRT" -> { brtState.fromJson(est.payload); screen = Screen.BRT }
                    }
                })
            }
        }
    }
}
