package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.Dashboard
import com.controlfinanciero.data.models.MonthlyReport
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.NumberFormat
import java.util.Locale

private val monthNames =
    listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    monthlyReport: List<MonthlyReport>,
    dashboard: Dashboard?,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    val egresosPorCat = (dashboard?.transaccionesPorCategoria ?: emptyList())
        .filter { it.type == "egreso" }
        .sortedByDescending { it.total }
        .take(8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Balance mensual del año ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Balance mensual del año", fontWeight = FontWeight.Bold)
                    Text(
                        "Ingresos − egresos por mes (Ene → Dic)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (monthlyReport.isEmpty()) {
                        Text("Sin datos todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val balances = monthlyReport.map { it.balance.toFloat() }
                        val producer = remember { CartesianChartModelProducer() }
                        LaunchedEffect(balances) {
                            producer.runTransaction { lineSeries { series(balances) } }
                        }
                        CartesianChartHost(
                            rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(),
                                bottomAxis = HorizontalAxis.rememberBottom(),
                            ),
                            producer,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }

            // --- Gasto por categoría (mes actual) ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Gasto por categoría (mes)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (egresosPorCat.isEmpty()) {
                        Text("Sin gastos este mes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val values = egresosPorCat.map { it.total.toFloat() }
                        val producer = remember { CartesianChartModelProducer() }
                        LaunchedEffect(values) {
                            producer.runTransaction { columnSeries { series(values) } }
                        }
                        CartesianChartHost(
                            rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(),
                                bottomAxis = HorizontalAxis.rememberBottom(),
                            ),
                            producer,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        // Leyenda (las columnas van en este orden, de izquierda a derecha).
                        egresosPorCat.forEachIndexed { i, cat ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${i + 1}. ${cat.categoryName}", fontSize = 13.sp)
                                Text(fmt.format(cat.total), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
