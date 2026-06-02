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
import com.controlfinanciero.data.models.Budget
import com.controlfinanciero.data.models.Category
import com.controlfinanciero.data.models.CreateBudgetRequest
import com.controlfinanciero.data.models.UpdateBudgetRequest
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private fun budgetColor(b: Budget): Color = when {
    b.exceeded -> Color(0xFFC62828)
    b.percentUsed >= 80 -> Color(0xFFEF6C00)
    else -> Color(0xFF2E7D32)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    budgets: List<Budget>,
    categories: List<Category>,
    onCreate: (CreateBudgetRequest) -> Unit,
    onUpdate: (Int, UpdateBudgetRequest) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Budget?>(null) }

    // Categorías de egreso que todavía no tienen presupuesto (para el alta).
    val availableCategories = categories.filter { cat ->
        cat.type == "egreso" && budgets.none { it.categoryId == cat.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (availableCategories.isNotEmpty()) {
                FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar presupuesto")
                }
            }
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Poné un límite mensual de gasto por categoría y seguí cuánto llevás gastado. " +
                        "Te avisa cuando te estás por pasar.",
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
                items(budgets) { b ->
                    BudgetCard(
                        b, fmt,
                        onEdit = { editing = b; showDialog = true },
                        onDelete = onDelete
                    )
                }
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            existing = editing,
            availableCategories = availableCategories,
            onConfirm = { categoryId, limit ->
                val e = editing
                if (e == null) onCreate(CreateBudgetRequest(categoryId, limit))
                else onUpdate(e.id, UpdateBudgetRequest(limit))
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun BudgetCard(
    b: Budget,
    fmt: NumberFormat,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    val color = budgetColor(b)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    b.categoryName ?: "Sin categoría",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text("${b.percentUsed.roundToInt()}%", fontWeight = FontWeight.Bold, color = color)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { onDelete(b.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (b.percentUsed / 100.0).coerceIn(0.0, 1.0).toFloat() },
                color = color,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${fmt.format(b.spent)} / ${fmt.format(b.monthlyLimit)}", fontSize = 12.sp)
                Text(
                    if (b.exceeded) "Te pasaste por ${fmt.format(-b.remaining)}"
                    else "Te queda ${fmt.format(b.remaining)}",
                    fontSize = 12.sp,
                    color = if (b.exceeded) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    existing: Budget?,
    availableCategories: List<Category>,
    onConfirm: (categoryId: Int, limit: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showCatDropdown by remember { mutableStateOf(false) }
    var limit by remember {
        mutableStateOf(
            existing?.monthlyLimit?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
        )
    }

    val limitVal = limit.toDoubleOrNull()
    val valid = limitVal != null && limitVal > 0 && (existing != null || selectedCategory != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo presupuesto" else "Editar presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing != null) {
                    Text(
                        existing.categoryName ?: "Categoría",
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = showCatDropdown,
                        onExpandedChange = { showCatDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría (egreso)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCatDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCatDropdown,
                            onDismissRequest = { showCatDropdown = false }
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { selectedCategory = cat; showCatDropdown = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Límite mensual") },
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
                onClick = {
                    val catId = existing?.categoryId ?: selectedCategory!!.id!!
                    onConfirm(catId, limitVal!!)
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
