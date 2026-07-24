package com.controlfinanciero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.Dashboard
import com.controlfinanciero.data.models.Transaction
import com.controlfinanciero.ui.theme.FinanceColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboard: Dashboard?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onAddTransaction: () -> Unit,
    onSyncMercadoPago: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control Financiero") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = onSyncMercadoPago) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar MP")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        if (isLoading && dashboard == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        error?.let {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        dashboard?.let { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance cards
                item {
                    BalanceCards(data, currencyFormat)
                }

                // Gastos por categoría
                item {
                    Text("Gastos por categoría", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                items(data.transaccionesPorCategoria.filter { it.type == "egreso" }) { cat ->
                    CategoryBar(
                        name = cat.categoryName,
                        amount = currencyFormat.format(cat.total),
                        percentage = if (data.totalEgresos > 0) (cat.total / data.totalEgresos).toFloat() else 0f,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Transacciones recientes
                item {
                    Text("Transacciones recientes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                items(data.transaccionesRecientes) { tx ->
                    TransactionItem(tx, currencyFormat)
                }
            }
        }
    }
}

@Composable
fun BalanceCards(dashboard: Dashboard, format: NumberFormat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FinanceCard(
            modifier = Modifier.weight(1f),
            title = "Ingresos",
            amount = format.format(dashboard.totalIngresos),
            color = FinanceColors.Income
        )
        FinanceCard(
            modifier = Modifier.weight(1f),
            title = "Egresos",
            amount = format.format(dashboard.totalEgresos),
            color = FinanceColors.Expense
        )
    }

    Spacer(Modifier.height(8.dp))

    val balanceColor = if (dashboard.balance >= 0) FinanceColors.Income else FinanceColors.Expense
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = balanceColor.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                format.format(dashboard.balance),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
        }
    }
}

@Composable
fun FinanceCard(modifier: Modifier, title: String, amount: String, color: Color) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun CategoryBar(name: String, amount: String, percentage: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 14.sp)
            Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(percentage).height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction, format: NumberFormat) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tx.description, fontWeight = FontWeight.Medium)
                Text(
                    "${tx.categoryName ?: "Sin categoría"} · ${tx.date.take(10)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.source != "manual") {
                    Text(tx.source, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Text(
                "${if (tx.type == "ingreso") "+" else "-"}${format.format(tx.amount)}",
                fontWeight = FontWeight.Bold,
                color = if (tx.type == "ingreso") FinanceColors.Income else FinanceColors.Expense
            )
        }
    }
}
