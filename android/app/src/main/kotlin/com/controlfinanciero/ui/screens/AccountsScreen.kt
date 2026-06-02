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
import com.controlfinanciero.data.models.Account
import com.controlfinanciero.data.models.AccountSummary
import com.controlfinanciero.data.models.CreateAccountRequest
import com.controlfinanciero.data.models.UpdateAccountRequest
import com.controlfinanciero.ui.theme.FinanceColors
import java.text.NumberFormat
import java.util.Locale

private val accountTypes = listOf(
    "efectivo" to "Efectivo", "banco" to "Banco", "billetera" to "Billetera", "otro" to "Otro"
)

private fun accTypeLabel(t: String) = accountTypes.firstOrNull { it.first == t }?.second ?: t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    summary: AccountSummary?,
    onCreate: (CreateAccountRequest) -> Unit,
    onUpdate: (Int, UpdateAccountRequest) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar cuenta")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            summary?.let {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Patrimonio total", fontSize = 12.sp)
                        Text(
                            fmt.format(it.totalBalance),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (it.totalBalance >= 0) MaterialTheme.colorScheme.onSurface else FinanceColors.Expense
                        )
                    }
                }
            }

            val items = summary?.accounts ?: emptyList()
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Creá tus cuentas (Efectivo, Banco, Mercado Pago, Brubank…) y asignales las " +
                            "transacciones para ver el saldo de cada una.",
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
                    items(items) { acc ->
                        AccountCard(
                            acc, fmt,
                            onEdit = { editing = acc; showDialog = true },
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AccountDialog(
            existing = editing,
            onConfirm = { req ->
                val e = editing
                if (e == null) onCreate(req)
                else onUpdate(e.id, UpdateAccountRequest(req.name, req.type, req.initialBalance))
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun AccountCard(
    acc: Account,
    fmt: NumberFormat,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(acc.name, fontWeight = FontWeight.Medium)
                Text(
                    accTypeLabel(acc.type),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                fmt.format(acc.balance),
                fontWeight = FontWeight.Bold,
                color = if (acc.balance >= 0) MaterialTheme.colorScheme.onSurface else FinanceColors.Expense
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = { onDelete(acc.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDialog(
    existing: Account?,
    onConfirm: (CreateAccountRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "efectivo") }
    var initial by remember {
        mutableStateOf(
            existing?.initialBalance?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva cuenta" else "Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej: Mercado Pago)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = accTypeLabel(type),
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
                        accountTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { type = value; showTypeDropdown = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initial,
                    onValueChange = { initial = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Saldo inicial") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        CreateAccountRequest(
                            name = name.trim(),
                            type = type,
                            initialBalance = initial.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
