package com.controlfinanciero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.controlfinanciero.data.models.User
import com.controlfinanciero.ui.components.InitialsAvatar
import com.controlfinanciero.ui.screens.AddTransactionScreen
import com.controlfinanciero.ui.screens.AuthScreen
import com.controlfinanciero.ui.screens.DashboardScreen
import com.controlfinanciero.ui.screens.HouseholdScreen
import com.controlfinanciero.ui.screens.PremiumScreen
import com.controlfinanciero.ui.screens.RecurringScreen
import com.controlfinanciero.ui.screens.SettingsScreen
import com.controlfinanciero.ui.theme.ControlFinancieroTheme
import com.controlfinanciero.ui.viewmodels.AuthViewModel
import com.controlfinanciero.ui.viewmodels.DashboardViewModel
import com.controlfinanciero.ui.viewmodels.HouseholdViewModel
import com.controlfinanciero.ui.viewmodels.RecurringViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControlFinancieroTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.isAuthenticated.collectAsState()
                val authLoading by authViewModel.isLoading.collectAsState()
                val authError by authViewModel.error.collectAsState()

                when (authState) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    false -> AuthScreen(
                        isLoading = authLoading,
                        error = authError,
                        onLogin = authViewModel::login,
                        onRegister = authViewModel::register
                    )
                    true -> {
                        val user by authViewModel.user.collectAsState()
                        AppNavigation(
                            user = user,
                            onLogout = authViewModel::logout,
                            onEditProfile = authViewModel::editProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(
    user: User?,
    onLogout: () -> Unit,
    onEditProfile: (String, (Boolean) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val viewModel: DashboardViewModel = viewModel()

    val dashboard by viewModel.dashboard.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        // El gesto de abrir solo aplica en el inicio; las subpantallas tienen botón "volver".
        gesturesEnabled = currentRoute == "dashboard",
        drawerContent = {
            AppDrawer(
                user = user,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    when {
                        route == currentRoute -> Unit
                        route == "dashboard" -> navController.popBackStack("dashboard", inclusive = false)
                        else -> navController.navigate(route) { launchSingleTop = true }
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            )
        }
    ) {
        NavHost(navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    dashboard = dashboard,
                    isLoading = isLoading,
                    error = error,
                    onRefresh = { viewModel.loadDashboard() },
                    onAddTransaction = { navController.navigate("add_transaction") },
                    onSyncMercadoPago = { viewModel.syncMercadoPago { /* TODO: snackbar */ } },
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            }
            composable("add_transaction") {
                AddTransactionScreen(
                    categories = categories,
                    onSave = { request ->
                        viewModel.createTransaction(request) {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("recurring") {
                val recurringViewModel: RecurringViewModel = viewModel()
                val recurringItems by recurringViewModel.items.collectAsState()
                val recurringCategories by recurringViewModel.categories.collectAsState()
                RecurringScreen(
                    recurrences = recurringItems,
                    categories = recurringCategories,
                    onAdd = { recurringViewModel.create(it) },
                    onDelete = { recurringViewModel.delete(it) },
                    // Al volver recargamos el dashboard: pudo materializarse una transacción.
                    onBack = { viewModel.loadDashboard(); navController.popBackStack() }
                )
            }
            composable("household") {
                val householdViewModel: HouseholdViewModel = viewModel()
                val household by householdViewModel.household.collectAsState()
                val householdLoading by householdViewModel.isLoading.collectAsState()
                HouseholdScreen(
                    household = household,
                    isLoading = householdLoading,
                    onCreate = { householdViewModel.create(it) },
                    onJoin = { householdViewModel.join(it) },
                    onLeave = { householdViewModel.leave() },
                    // Cambiar de hogar cambia qué datos se ven: recargamos el dashboard.
                    onBack = { viewModel.loadDashboard(); navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    user = user,
                    onBack = { navController.popBackStack() },
                    onSaveName = onEditProfile
                )
            }
            composable("premium") {
                PremiumScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AppDrawer(
    user: User?,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            InitialsAvatar(user?.name, user?.email ?: "", size = 64.dp, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                user?.name?.takeIf { it.isNotBlank() } ?: "Mi cuenta",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                user?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        DrawerItem("Inicio", Icons.Default.Home, currentRoute == "dashboard") { onNavigate("dashboard") }
        DrawerItem("Ingresos/gastos fijos", Icons.Default.Repeat, currentRoute == "recurring") { onNavigate("recurring") }
        DrawerItem("Hogar compartido", Icons.Default.Group, currentRoute == "household") { onNavigate("household") }
        DrawerItem("Configuración", Icons.Default.Settings, currentRoute == "settings") { onNavigate("settings") }
        DrawerItem("Mejorar plan", Icons.Default.WorkspacePremium, currentRoute == "premium") { onNavigate("premium") }
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        DrawerItem("Cerrar sesión", Icons.AutoMirrored.Filled.Logout, false, onLogout)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
