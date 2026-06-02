package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.AccountSummary
import com.controlfinanciero.data.models.CreateAccountRequest
import com.controlfinanciero.data.models.UpdateAccountRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _summary = MutableStateFlow<AccountSummary?>(null)
    val summary: StateFlow<AccountSummary?> = _summary

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getAccounts()
                if (resp.success) _summary.value = resp.data
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.createAccount(request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun update(id: Int, request: UpdateAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (api.updateAccount(id, request).success) { load(); onSuccess() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteAccount(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
