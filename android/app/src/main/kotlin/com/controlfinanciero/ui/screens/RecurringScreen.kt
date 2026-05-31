package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.Category
import com.controlfinanciero.data.models.CreateRecurringRequest
import com.controlfinanciero.data.models.RecurringTransaction
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    items: List<RecurringTransaction>,
    categories: List<Category>,
    onAdd: (CreateRecurringRequest) -> Unit,
    onDelete: (Int) -> Unit,
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
                Icon(Icons.Default.Add, contentDescription = "Agregar recurrente")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Definí un ingreso o gasto fijo (ej: tus haberes el día 1) y se registra solo cada mes.",
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
                items(items) { item ->
                    RecurringItem(item, currencyFormat, onDelete)
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
private fun RecurringItem(
    item: RecurringTransaction,
    format: NumberFormat,
    onDelete: (Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.description, fontWeight = FontWeight.Medium)
                Text(
                    "${item.categoryName ?: "Sin categoría"} · día ${item.dayOfMonth}",
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
            item.id?.let { id ->
                IconButton(onClick = { onDelete(id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
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
    var day by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == selectedType }
    val dayInt = day.toIntOrNull()
    val valid = amount.toDoubleOrNull()?.let { it > 0 } == true &&
            description.isNotBlank() &&
            selectedCategory != null &&
            dayInt != null && dayInt in 1..28

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo fijo mensual") },
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
                    label = { Text("Descripción (ej: Haberes)") },
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

                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Día del mes (1-28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                            dayOfMonth = dayInt!!
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
