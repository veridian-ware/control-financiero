package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.ContributeRequest
import com.controlfinanciero.data.models.CreateSavingsGoalRequest
import com.controlfinanciero.data.models.SavingsGoalSummary
import com.controlfinanciero.data.models.UpdateSavingsGoalRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavingsGoalViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _summary = MutableStateFlow<SavingsGoalSummary?>(null)
    val summary: StateFlow<SavingsGoalSummary?> = _summary

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getSavingsGoals()
                if (resp.success) _summary.value = resp.data
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateSavingsGoalRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.createSavingsGoal(request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun update(id: Int, request: UpdateSavingsGoalRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.updateSavingsGoal(id, request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /** Aporta (amount > 0) o retira (amount < 0) sobre el ahorrado de la meta. */
    fun contribute(id: Int, amount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.contributeSavingsGoal(id, ContributeRequest(amount)).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteSavingsGoal(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
