package com.controlfinanciero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.controlfinanciero.ui.screens.AddTransactionScreen
import com.controlfinanciero.ui.screens.AuthScreen
import com.controlfinanciero.ui.screens.DashboardScreen
import com.controlfinanciero.ui.screens.HouseholdScreen
import com.controlfinanciero.ui.screens.RecurringScreen
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
                    true -> AppNavigation(onLogout = authViewModel::logout)
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val viewModel: DashboardViewModel = viewModel()

    val dashboard by viewModel.dashboard.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                dashboard = dashboard,
                isLoading = isLoading,
                error = error,
                onRefresh = { viewModel.loadDashboard() },
                onAddTransaction = { navController.navigate("add_transaction") },
                onSyncMercadoPago = { viewModel.syncMercadoPago { /* TODO: snackbar */ } },
                onOpenRecurring = { navController.navigate("recurring") },
                onOpenHousehold = { navController.navigate("household") },
                onLogout = onLogout
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
    }
}
