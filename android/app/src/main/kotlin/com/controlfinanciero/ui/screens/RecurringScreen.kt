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
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.Category
import com.controlfinanciero.data.models.CreateRecurringRequest
import com.controlfinanciero.data.models.RecurringOccurrence
import com.controlfinanciero.data.models.RecurringTransaction
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val frequencies = listOf("semanal" to "Semanal", "quincenal" to "Quincenal", "mensual" to "Mensual")

private fun frequencyLabel(f: String) = frequencies.firstOrNull { it.first == f }?.second ?: f

/** "2026-06-01" -> "01/06". */
private fun shortDate(iso: String): String =
    if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}" else iso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    recurrences: List<RecurringTransaction>,
    categories: List<Category>,
    onAdd: (CreateRecurringRequest) -> Unit,
    onDelete: (Int) -> Unit,
    onPay: (Long) -> Unit,
    onUnpay: (Long) -> Unit,
    onBack: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingresos / gastos fijos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar fijo")
            }
        }
    ) { padding ->
        if (recurrences.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Definí un ingreso o gasto fijo (semanal, quincenal o mensual) y marcá cada " +
                        "vencimiento como pagado cuando lo abones.",
                    Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recurrences) { item ->
                    FixedCard(item, currencyFormat, onDelete, onPay, onUnpay)
                }
            }
        }
    }

    if (showDialog) {
        AddRecurringDialog(
            categories = categories,
            onConfirm = { onAdd(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun FixedCard(
    item: RecurringTransaction,
    format: NumberFormat,
    onDelete: (Int) -> Unit,
    onPay: (Long) -> Unit,
    onUnpay: (Long) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.description, fontWeight = FontWeight.Medium)
                    Text(
                        "${item.categoryName ?: "Sin categoría"} · ${frequencyLabel(item.frequency)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${if (item.type == "ingreso") "+" else "-"}${format.format(item.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (item.type == "ingreso") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                IconButton(onClick = { onDelete(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }

            if (item.occurrences.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vencimientos del mes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.occurrences.forEach { occ ->
                    OccurrenceRow(occ, format, onPay, onUnpay)
                }
            }
        }
    }
}

@Composable
private fun OccurrenceRow(
    occ: RecurringOccurrence,
    format: NumberFormat,
    onPay: (Long) -> Unit,
    onUnpay: (Long) -> Unit
) {
    val paid = occ.status == "pagado"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (paid) onUnpay(occ.id) else onPay(occ.id) }) {
            Icon(
                if (paid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (paid) "Marcar pendiente" else "Marcar pagado",
                tint = if (paid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(Modifier.weight(1f)) {
            Text("Vence ${shortDate(occ.dueDate)}", fontSize = 14.sp)
            Text(
                if (paid) "Pagado" else "Pendiente",
                fontSize = 11.sp,
                color = if (paid) Color(0xFF2E7D32) else Color(0xFFEF6C00)
            )
        }
        Text(format.format(occ.amount), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringDialog(
    categories: List<Category>,
    onConfirm: (CreateRecurringRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf("ingreso") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("mensual") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var anchorMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val anchorDate = remember(anchorMillis) {
        Instant.ofEpochMilli(anchorMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val filteredCategories = categories.filter { it.type == selectedType }
    val valid = amount.toDoubleOrNull()?.let { it > 0 } == true &&
        description.isNotBlank() &&
        selectedCategory != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ingreso / gasto fijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "ingreso",
                        onClick = { selectedType = "ingreso"; selectedCategory = null },
                        label = { Text("Ingreso") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "egreso",
                        onClick = { selectedType = "egreso"; selectedCategory = null },
                        label = { Text("Egreso") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej: Alquiler)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategory = cat; showCategoryDropdown = false }
                            )
                        }
                    }
                }

                Text("Frecuencia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frequencies.forEach { (value, label) ->
                        FilterChip(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = anchorDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primer vencimiento") },
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Elegir") }
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
                        CreateRecurringRequest(
                            amount = amount.toDouble(),
                            description = description,
                            type = selectedType,
                            categoryId = selectedCategory!!.id!!,
                            frequency = frequency,
                            anchorDate = anchorDate.toString()
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = anchorMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { anchorMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
