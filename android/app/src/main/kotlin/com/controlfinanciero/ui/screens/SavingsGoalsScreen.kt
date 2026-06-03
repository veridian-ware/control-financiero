package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import com.controlfinanciero.data.models.CreateSavingsGoalRequest
import com.controlfinanciero.data.models.SavingsGoal
import com.controlfinanciero.data.models.SavingsGoalSummary
import com.controlfinanciero.data.models.UpdateSavingsGoalRequest
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private val reachedGreen = Color(0xFF2E7D32)

/** "2026-12-31" -> "31/12/2026". */
private fun prettyDate(iso: String): String =
    if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}" else iso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    summary: SavingsGoalSummary?,
    onCreate: (CreateSavingsGoalRequest) -> Unit,
    onUpdate: (Int, UpdateSavingsGoalRequest) -> Unit,
    onContribute: (Int, Double) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SavingsGoal?>(null) }
    var contributing by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metas de ahorro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva meta")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            summary?.let { SummaryHeader(it, fmt) }

            val goals = summary?.goals ?: emptyList()
            if (goals.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Creá tu primera meta de ahorro (ej: Vacaciones, Fondo de emergencia) con un " +
                            "objetivo y, si querés, una fecha límite. Después andá sumando aportes.",
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
                    items(goals) { goal ->
                        GoalCard(
                            goal, fmt,
                            onContribute = { contributing = goal },
                            onEdit = { editing = goal; showDialog = true },
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        GoalDialog(
            existing = editing,
            onConfirm = { form, deadline, initial ->
                val e = editing
                if (e == null) {
                    onCreate(CreateSavingsGoalRequest(form.name, form.target, deadline, initial))
                } else {
                    onUpdate(e.id, UpdateSavingsGoalRequest(name = form.name, targetAmount = form.target, deadline = deadline))
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    contributing?.let { goal ->
        ContributeDialog(
            goal = goal,
            fmt = fmt,
            onConfirm = { signedAmount -> onContribute(goal.id, signedAmount); contributing = null },
            onDismiss = { contributing = null }
        )
    }
}

@Composable
private fun SummaryHeader(summary: SavingsGoalSummary, fmt: NumberFormat) {
    val fraction = if (summary.totalTarget > 0)
        (summary.totalSaved / summary.totalTarget).toFloat().coerceIn(0f, 1f) else 0f
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Ahorrado en total", fontSize = 12.sp)
            Text(fmt.format(summary.totalSaved), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Objetivo total: ${fmt.format(summary.totalTarget)}", fontSize = 13.sp)
        }
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    fmt: NumberFormat,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    val fraction = (goal.progressPct / 100.0).toFloat().coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.name, fontWeight = FontWeight.Medium)
                        if (goal.reached) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Meta alcanzada",
                                tint = reachedGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    goal.deadline?.let {
                        Text(
                            "Para el ${prettyDate(it)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = { onDelete(goal.id) }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (goal.reached) reachedGreen else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${fmt.format(goal.currentAmount)} / ${fmt.format(goal.targetAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text("${"%.0f".format(goal.progressPct)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (!goal.reached) {
                Text(
                    "Faltan ${fmt.format(goal.remaining)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onContribute, modifier = Modifier.fillMaxWidth()) {
                Text("Aportar / retirar")
            }
        }
    }
}

/** Campos comunes que devuelve el diálogo de alta/edición. */
private data class GoalForm(val name: String, val target: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    existing: SavingsGoal?,
    onConfirm: (GoalForm, deadline: String?, initialAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var target by remember { mutableStateOf(existing?.targetAmount?.let { fmtPlain(it) } ?: "") }
    var initial by remember { mutableStateOf("") }
    var deadlineMillis by remember {
        mutableStateOf(existing?.deadline?.let { isoToMillis(it) })
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val targetVal = target.toDoubleOrNull()
    val valid = name.isNotBlank() && targetVal != null && targetVal > 0
    val deadlineIso = deadlineMillis?.let { millisToIso(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej: Vacaciones)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Objetivo") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing == null) {
                    OutlinedTextField(
                        value = initial,
                        onValueChange = { initial = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Ahorrado inicial (opcional)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = deadlineIso?.let { prettyDate(it) } ?: "Sin fecha límite",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha límite (opcional)") },
                    trailingIcon = {
                        Row {
                            if (deadlineMillis != null) {
                                TextButton(onClick = { deadlineMillis = null }) { Text("Quitar") }
                            }
                            TextButton(onClick = { showDatePicker = true }) { Text("Elegir") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        GoalForm(name.trim(), targetVal!!),
                        deadlineIso,
                        initial.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { deadlineMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    fmt: NumberFormat,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var isWithdraw by remember { mutableStateOf(false) }
    val amountVal = amount.toDoubleOrNull()
    val valid = amountVal != null && amountVal > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Ahorrado: ${fmt.format(goal.currentAmount)} de ${fmt.format(goal.targetAmount)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isWithdraw,
                        onClick = { isWithdraw = false },
                        label = { Text("Aportar") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isWithdraw,
                        onClick = { isWithdraw = true },
                        label = { Text("Retirar") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(if (isWithdraw) -amountVal!! else amountVal!!) }
            ) { Text(if (isWithdraw) "Retirar" else "Aportar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Número plano para prellenar campos (sin separadores de miles). */
private fun fmtPlain(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun isoToMillis(iso: String): Long =
    java.time.LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
