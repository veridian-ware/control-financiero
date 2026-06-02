package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.Budget
import com.controlfinanciero.data.models.CreateBudgetRequest
import com.controlfinanciero.data.models.UpdateBudgetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getBudgets()
                if (resp.success) _budgets.value = resp.data ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateBudgetRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.createBudget(request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun update(id: Int, request: UpdateBudgetRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.updateBudget(id, request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteBudget(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
