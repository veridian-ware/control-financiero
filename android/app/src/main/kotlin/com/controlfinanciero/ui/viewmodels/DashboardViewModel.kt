package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Year

class DashboardViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _dashboard = MutableStateFlow<Dashboard?>(null)
    val dashboard: StateFlow<Dashboard?> = _dashboard

    private val _monthlyReport = MutableStateFlow<List<MonthlyReport>>(emptyList())
    val monthlyReport: StateFlow<List<MonthlyReport>> = _monthlyReport

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val dashResponse = api.getDashboard()
                if (dashResponse.success) _dashboard.value = dashResponse.data

                val monthlyResponse = api.getMonthlyReport(Year.now().value)
                if (monthlyResponse.success) _monthlyReport.value = monthlyResponse.data ?: emptyList()

                val catResponse = api.getCategories()
                if (catResponse.success) _categories.value = catResponse.data ?: emptyList()

                val accountsResponse = api.getAccounts()
                if (accountsResponse.success) _accounts.value = accountsResponse.data?.accounts ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTransaction(request: CreateTransactionRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.createTransaction(request)
                if (response.success) {
                    loadDashboard()
                    onSuccess()
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun syncMercadoPago(onResult: (SyncResult?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.syncMercadoPago()
                if (response.success) {
                    loadDashboard()
                    onResult(response.data)
                } else {
                    _error.value = response.message
                    onResult(null)
                }
            } catch (e: Exception) {
                _error.value = "Error sincronizando: ${e.message}"
                onResult(null)
            }
        }
    }

    fun clearError() { _error.value = null }
}
