package com.controlfinanciero.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.ImportResult
import com.controlfinanciero.data.models.User
import com.controlfinanciero.ui.components.InitialsAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onBack: () -> Unit,
    onSaveName: (String, (Boolean) -> Unit) -> Unit,
    onImportCsv: (String, (ImportResult?) -> Unit) -> Unit
) {
    var name by remember(user?.name) { mutableStateOf(user?.name ?: "") }
    var saving by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val nameValid = name.trim().isNotEmpty()
    val changed = name.trim() != (user?.name ?: "")

    // Selector de archivo: lee el CSV de la URI y dispara la importación.
    val pickCsv = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csv = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
        if (csv.isNullOrBlank()) {
            scope.launch { snackbar.showSnackbar("No se pudo leer el archivo") }
            return@rememberLauncherForActivityResult
        }
        importing = true
        onImportCsv(csv) { result ->
            importing = false
            scope.launch {
                snackbar.showSnackbar(
                    if (result != null) {
                        buildString {
                            append("Importadas ${result.imported} · Omitidas ${result.skipped}")
                            if (result.errors > 0) append(" · Errores ${result.errors}")
                        }
                    } else {
                        "No se pudo importar el CSV"
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera: avatar + email
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(user?.name, user?.email ?: "", size = 56.dp, fontSize = 20.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        user?.name?.takeIf { it.isNotBlank() } ?: "Sin nombre",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        user?.email ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Text("Tu perfil", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre para mostrar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    saving = true
                    onSaveName(name.trim()) { ok ->
                        saving = false
                        scope.launch {
                            snackbar.showSnackbar(
                                if (ok) "Perfil actualizado" else "No se pudo guardar"
                            )
                        }
                    }
                },
                enabled = nameValid && changed && !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar")
                }
            }

            HorizontalDivider()

            Text("Importar movimientos", fontWeight = FontWeight.Bold)
            Text(
                "Exportá el CSV de \"dinero en cuenta\" desde Mercado Pago y seleccioná el archivo. " +
                    "Se cargan los movimientos como ingresos/egresos (sin duplicar los ya importados).",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { pickCsv.launch("*/*") },
                enabled = !importing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (importing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Importar CSV de Mercado Pago")
                }
            }
        }
    }
}
