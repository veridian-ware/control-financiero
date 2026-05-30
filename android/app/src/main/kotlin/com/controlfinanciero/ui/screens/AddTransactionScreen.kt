package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.controlfinanciero.data.models.Category
import com.controlfinanciero.data.models.CreateTransactionRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    categories: List<Category>,
    onSave: (CreateTransactionRequest) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("egreso") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva transacción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tipo: Ingreso / Egreso
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

            // Monto
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monto") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Categoría
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
                            onClick = {
                                selectedCategory = cat
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Guardar
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    val cat = selectedCategory ?: return@Button
                    if (description.isBlank()) return@Button

                    onSave(
                        CreateTransactionRequest(
                            amount = amt,
                            description = description,
                            type = selectedType,
                            categoryId = cat.id!!
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = amount.toDoubleOrNull() != null
                        && description.isNotBlank()
                        && selectedCategory != null
            ) {
                Text("Guardar transacción")
            }
        }
    }
}
