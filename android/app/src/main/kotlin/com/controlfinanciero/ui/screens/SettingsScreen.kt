package com.controlfinanciero.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controlfinanciero.data.models.User
import com.controlfinanciero.ui.components.InitialsAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onBack: () -> Unit,
    onSaveName: (String, (Boolean) -> Unit) -> Unit
) {
    var name by remember(user?.name) { mutableStateOf(user?.name ?: "") }
    var saving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val nameValid = name.trim().isNotEmpty()
    val changed = name.trim() != (user?.name ?: "")

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
        }
    }
}
