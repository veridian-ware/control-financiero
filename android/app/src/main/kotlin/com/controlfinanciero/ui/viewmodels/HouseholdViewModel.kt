package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.CreateHouseholdRequest
import com.controlfinanciero.data.models.Household
import com.controlfinanciero.data.models.JoinHouseholdRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HouseholdViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = api.getHousehold()
                _household.value = if (resp.success) resp.data else null
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            try {
                val resp = api.createHousehold(CreateHouseholdRequest(name))
                if (resp.success) _household.value = resp.data else _error.value = resp.message
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun join(inviteCode: String) {
        viewModelScope.launch {
            try {
                val resp = api.joinHousehold(JoinHouseholdRequest(inviteCode))
                if (resp.success) _household.value = resp.data
                else _error.value = resp.message ?: "Código inválido"
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            try {
                if (api.leaveHousehold().success) _household.value = null
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
