package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.CreateInvestmentRequest
import com.controlfinanciero.data.models.Investment
import com.controlfinanciero.data.models.InvestmentSummary
import com.controlfinanciero.data.models.UpdateInvestmentRequest
import java.text.NumberFormat
import java.util.Locale

private val investmentTypes = listOf(
    "acciones" to "Acciones", "cripto" to "Cripto", "plazo_fijo" to "Plazo fijo",
    "fci" to "FCI", "dolares" to "Dólares", "otro" to "Otro"
)

private fun typeLabel(t: String) = investmentTypes.firstOrNull { it.first == t }?.second ?: t

private val gainGreen = Color(0xFF2E7D32)
private val lossRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    summary: InvestmentSummary?,
    onCreate: (CreateInvestmentRequest) -> Unit,
    onUpdate: (Int, UpdateInvestmentRequest) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Investment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inversiones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar inversión")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            summary?.let { SummaryHeader(it, fmt) }

            val items = summary?.investments ?: emptyList()
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Agregá tu primera inversión (acciones, cripto, plazo fijo, dólares…) y " +
                            "actualizá su valor actual cuando quieras ver el rendimiento.",
                        Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { inv ->
                        InvestmentCard(
                            inv, fmt,
                            onEdit = { editing = inv; showDialog = true },
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        InvestmentDialog(
            existing = editing,
            onConfirm = { req, current ->
                val e = editing
                if (e == null) {
                    onCreate(CreateInvestmentRequest(req.name, req.type, req.amountInvested, current))
                } else {
                    onUpdate(
                        e.id,
                        UpdateInvestmentRequest(req.name, req.type, req.amountInvested, current)
                    )
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun SummaryHeader(summary: InvestmentSummary, fmt: NumberFormat) {
    val gainColor = if (summary.totalGain >= 0) gainGreen else lossRed
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Valor actual del portfolio", fontSize = 12.sp)
            Text(fmt.format(summary.totalValue), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Invertido: ${fmt.format(summary.totalInvested)}", fontSize = 13.sp)
                Text(
                    "${if (summary.totalGain >= 0) "▲" else "▼"} ${fmt.format(summary.totalGain)} " +
                        "(${"%+.1f".format(summary.yieldPct)}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = gainColor
                )
            }
        }
    }
}

@Composable
private fun InvestmentCard(
    inv: Investment,
    fmt: NumberFormat,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    val gainColor = if (inv.gain >= 0) gainGreen else lossRed
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(inv.name, fontWeight = FontWeight.Medium)
                Text(
                    typeLabel(inv.type),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Invertido ${fmt.format(inv.amountInvested)} → ${fmt.format(inv.currentValue)}",
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (inv.gain >= 0) "+" else ""}${fmt.format(inv.gain)}",
                    fontWeight = FontWeight.Bold,
                    color = gainColor
                )
                Text("${"%+.1f".format(inv.yieldPct)}%", fontSize = 12.sp, color = gainColor)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = { onDelete(inv.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

/** Campos comunes que devuelve el diálogo (el valor actual va aparte por ser opcional). */
private data class InvestmentForm(val name: String, val type: String, val amountInvested: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentDialog(
    existing: Investment?,
    onConfirm: (InvestmentForm, currentValue: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "acciones") }
    var invested by remember { mutableStateOf(existing?.amountInvested?.let { fmtNumber(it) } ?: "") }
    var current by remember { mutableStateOf(existing?.currentValue?.let { fmtNumber(it) } ?: "") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val investedVal = invested.toDoubleOrNull()
    val valid = name.isNotBlank() && investedVal != null && investedVal >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva inversión" else "Editar inversión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej: Acciones AAPL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = typeLabel(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        investmentTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { type = value; showTypeDropdown = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = invested,
                    onValueChange = { invested = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto invertido") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor actual (opcional)") },
                    prefix = { Text("$") },
                    supportingText = { Text("Si lo dejás vacío, se usa el monto invertido") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        InvestmentForm(name.trim(), type, investedVal!!),
                        current.toDoubleOrNull()
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Número plano para prellenar los campos (sin separadores de miles). */
private fun fmtNumber(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
