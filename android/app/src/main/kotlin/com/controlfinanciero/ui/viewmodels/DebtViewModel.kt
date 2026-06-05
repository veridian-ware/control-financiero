package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.CreateDebtRequest
import com.controlfinanciero.data.models.DebtSummary
import com.controlfinanciero.data.models.UpdateDebtRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DebtViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _summary = MutableStateFlow<DebtSummary?>(null)
    val summary: StateFlow<DebtSummary?> = _summary

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getDebts()
                if (resp.success) _summary.value = resp.data
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateDebtRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.createDebt(request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun update(id: Int, request: UpdateDebtRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.updateDebt(id, request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /** Registra el pago de una cuota (avanza el contador). */
    fun pay(id: Int) {
        viewModelScope.launch {
            try {
                if (api.payDebt(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /** Revierte el último pago. */
    fun unpay(id: Int) {
        viewModelScope.launch {
            try {
                if (api.unpayDebt(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteDebt(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
