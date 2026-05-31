package com.controlfinanciero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
    onLogout: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control Financiero") },
                actions = {
                    IconButton(onClick = onSyncMercadoPago) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar MP")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
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
                        color = Color(0xFFFF5722)
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
            color = Color(0xFF4CAF50)
        )
        FinanceCard(
            modifier = Modifier.weight(1f),
            title = "Egresos",
            amount = format.format(dashboard.totalEgresos),
            color = Color(0xFFF44336)
        )
    }

    Spacer(Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (dashboard.balance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Balance", fontSize = 14.sp)
            Text(
                format.format(dashboard.balance),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (dashboard.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun FinanceCard(modifier: Modifier, title: String, amount: String, color: Color) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
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
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
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
                    fontSize = 12.sp, color = Color.Gray
                )
                if (tx.source != "manual") {
                    Text(tx.source, fontSize = 10.sp, color = Color(0xFF1976D2))
                }
            }
            Text(
                "${if (tx.type == "ingreso") "+" else "-"}${format.format(tx.amount)}",
                fontWeight = FontWeight.Bold,
                color = if (tx.type == "ingreso") Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
