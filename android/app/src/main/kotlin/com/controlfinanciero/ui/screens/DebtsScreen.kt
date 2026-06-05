package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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
import com.controlfinanciero.data.models.CreateDebtRequest
import com.controlfinanciero.data.models.Debt
import com.controlfinanciero.data.models.DebtSummary
import com.controlfinanciero.data.models.UpdateDebtRequest
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private val overdueRed = Color(0xFFC62828)
private val dueSoonOrange = Color(0xFFEF6C00)
private val finishedGreen = Color(0xFF2E7D32)

private val debtTypes = listOf(
    "prestamo" to "Préstamo",
    "persona" to "Deuda a persona",
    "compra" to "Compra en cuotas"
)

private fun typeLabel(type: String): String = debtTypes.firstOrNull { it.first == type }?.second ?: type

/** "2026-08-10" -> "10/08/2026". */
private fun prettyDate(iso: String): String =
    if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}" else iso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    summary: DebtSummary?,
    onCreate: (CreateDebtRequest) -> Unit,
    onUpdate: (Int, UpdateDebtRequest) -> Unit,
    onPay: (Int) -> Unit,
    onUnpay: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuotas y deudas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva deuda")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            summary?.let { SummaryHeader(it, fmt) }

            val debts = summary?.debts ?: emptyList()
            if (debts.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Cargá tus préstamos, compras en cuotas y deudas a personas. Para cada una poné " +
                            "el monto de la cuota, en cuántas va (ej: 3 de 6) y cuándo vence. Después marcás " +
                            "cada cuota a medida que la pagás.",
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
                    items(debts) { debt ->
                        DebtCard(
                            debt, fmt,
                            onPay = { onPay(debt.id) },
                            onUnpay = { onUnpay(debt.id) },
                            onEdit = { editing = debt; showDialog = true },
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        DebtDialog(
            existing = editing,
            onConfirm = { form ->
                val e = editing
                if (e == null) {
                    onCreate(
                        CreateDebtRequest(
                            description = form.description,
                            type = form.type,
                            creditor = form.creditor,
                            installmentAmount = form.installmentAmount,
                            totalInstallments = form.totalInstallments,
                            paidInstallments = form.paidInstallments,
                            dueDate = form.dueDate,
                            notes = form.notes
                        )
                    )
                } else {
                    onUpdate(
                        e.id,
                        UpdateDebtRequest(
                            description = form.description,
                            type = form.type,
                            creditor = form.creditor,
                            installmentAmount = form.installmentAmount,
                            totalInstallments = form.totalInstallments,
                            paidInstallments = form.paidInstallments,
                            dueDate = form.dueDate,
                            notes = form.notes
                        )
                    )
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun SummaryHeader(summary: DebtSummary, fmt: NumberFormat) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Te queda por pagar", fontSize = 12.sp)
            Text(fmt.format(summary.totalRemaining), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Compromiso mensual: ${fmt.format(summary.totalMonthly)}", fontSize = 13.sp)
            if (summary.overdueCount > 0 || summary.dueSoonCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (summary.overdueCount > 0) {
                        StatusPill(
                            "${summary.overdueCount} vencida${if (summary.overdueCount > 1) "s" else ""}",
                            overdueRed
                        )
                    }
                    if (summary.dueSoonCount > 0) StatusPill("${summary.dueSoonCount} por vencer", dueSoonOrange)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), contentColor = color, shape = MaterialTheme.shapes.small) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    fmt: NumberFormat,
    onPay: () -> Unit,
    onUnpay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(debt.description, fontWeight = FontWeight.Medium)
                        if (debt.finished) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Saldada",
                                tint = finishedGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        typeLabel(debt.type) + (debt.creditor?.let { " · $it" } ?: ""),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = { onDelete(debt.id) }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }

            if (debt.totalInstallments != null) {
                val fraction = (debt.progressPct / 100.0).toFloat().coerceIn(0f, 1f)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (debt.finished) finishedGreen else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Cuota ${debt.paidInstallments}/${debt.totalInstallments}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(fmt.format(debt.installmentAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (!debt.finished) {
                    Text(
                        "Restan ${fmt.format(debt.remainingAmount)} · ${debt.remainingInstallments} cuotas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (debt.finished) "Pagada" else "Pendiente",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (debt.finished) finishedGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(fmt.format(debt.installmentAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            debt.dueDate?.let { due ->
                val (label, color) = when (debt.dueStatus) {
                    "vencido" -> "Venció el ${prettyDate(due)}" to overdueRed
                    "proximo" -> "Vence el ${prettyDate(due)}" to dueSoonOrange
                    else -> "Vence el ${prettyDate(due)}" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = if (debt.dueStatus == "vencido") FontWeight.Bold else FontWeight.Normal
                )
            }

            if (!debt.finished) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
                    Text(if (debt.totalInstallments != null) "Registrar pago de cuota" else "Marcar pagada")
                }
            }
            if (debt.paidInstallments > 0) {
                TextButton(onClick = onUnpay, modifier = Modifier.align(Alignment.End)) {
                    Text("Deshacer último pago", fontSize = 12.sp)
                }
            }
        }
    }
}

/** Campos que devuelve el diálogo de alta/edición. */
private data class DebtForm(
    val description: String,
    val type: String,
    val creditor: String?,
    val installmentAmount: Double,
    val totalInstallments: Int?,
    val paidInstallments: Int,
    val dueDate: String?,
    val notes: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtDialog(
    existing: Debt?,
    onConfirm: (DebtForm) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "prestamo") }
    var creditor by remember { mutableStateOf(existing?.creditor ?: "") }
    var amount by remember { mutableStateOf(existing?.installmentAmount?.let { fmtPlain(it) } ?: "") }
    var hasInstallments by remember { mutableStateOf(existing?.totalInstallments != null) }
    var total by remember { mutableStateOf(existing?.totalInstallments?.toString() ?: "") }
    var paid by remember { mutableStateOf(existing?.paidInstallments?.toString() ?: "0") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var dueMillis by remember { mutableStateOf(existing?.dueDate?.let { isoToMillis(it) }) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountVal = amount.toDoubleOrNull()
    val totalVal = total.toIntOrNull()
    val paidVal = paid.toIntOrNull() ?: 0
    val valid = description.isNotBlank() && amountVal != null && amountVal > 0 &&
        (!hasInstallments || (totalVal != null && totalVal > 0 && paidVal in 0..totalVal))
    val dueIso = dueMillis?.let { millisToIso(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva deuda" else "Editar deuda") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej: Philco, Préstamo Banco)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    debtTypes.forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text("A quién (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (hasInstallments) "Monto de la cuota" else "Monto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasInstallments, onCheckedChange = { hasInstallments = it })
                    Text("En cuotas")
                }
                if (hasInstallments) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = paid,
                            onValueChange = { paid = it.filter { c -> c.isDigit() } },
                            label = { Text("Pagadas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = total,
                            onValueChange = { total = it.filter { c -> c.isDigit() } },
                            label = { Text("Total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = dueIso?.let { prettyDate(it) } ?: "Sin fecha",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vencimiento (opcional)") },
                    trailingIcon = {
                        Row {
                            if (dueMillis != null) {
                                TextButton(onClick = { dueMillis = null }) { Text("Quitar") }
                            }
                            TextButton(onClick = { showDatePicker = true }) { Text("Elegir") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota (opcional)") },
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
                        DebtForm(
                            description = description.trim(),
                            type = type,
                            creditor = creditor.trim().ifBlank { null },
                            installmentAmount = amountVal!!,
                            totalInstallments = if (hasInstallments) totalVal else null,
                            paidInstallments = if (hasInstallments) paidVal else 0,
                            dueDate = dueIso,
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

/** Número plano para prellenar campos (sin separadores de miles). */
private fun fmtPlain(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun isoToMillis(iso: String): Long =
    java.time.LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
