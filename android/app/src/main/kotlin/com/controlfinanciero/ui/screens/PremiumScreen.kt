package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val premiumBenefits = listOf(
    "Presupuestos por categoría con alertas",
    "Reportes y gráficos avanzados",
    "Inversiones y cuentas ilimitadas",
    "Exportar a Excel y PDF",
    "Recordatorios de vencimientos",
    "Sin anuncios"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mejorar plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Control Financiero Premium", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Llevá el control de tus finanzas al siguiente nivel",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    premiumBenefits.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(benefit, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Próximamente")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Los planes pagos todavía no están disponibles.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
