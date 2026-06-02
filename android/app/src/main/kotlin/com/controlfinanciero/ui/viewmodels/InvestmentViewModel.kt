package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.CreateInvestmentRequest
import com.controlfinanciero.data.models.InvestmentSummary
import com.controlfinanciero.data.models.UpdateInvestmentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InvestmentViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _summary = MutableStateFlow<InvestmentSummary?>(null)
    val summary: StateFlow<InvestmentSummary?> = _summary

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getInvestments()
                if (resp.success) _summary.value = resp.data
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateInvestmentRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.createInvestment(request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun update(id: Int, request: UpdateInvestmentRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.updateInvestment(id, request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteInvestment(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
