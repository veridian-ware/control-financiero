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
import com.controlfinanciero.ui.theme.ControlFinancieroTheme
import com.controlfinanciero.ui.viewmodels.AuthViewModel
import com.controlfinanciero.ui.viewmodels.DashboardViewModel

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
    }
}
