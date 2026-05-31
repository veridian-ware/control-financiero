package com.controlfinanciero.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.controlfinanciero.data.api.RetrofitClient
import com.controlfinanciero.data.models.Category
import com.controlfinanciero.data.models.CreateRecurringRequest
import com.controlfinanciero.data.models.RecurringTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecurringViewModel : ViewModel() {

    private val api = RetrofitClient.api

    private val _items = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val items: StateFlow<List<RecurringTransaction>> = _items

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val resp = api.getRecurring()
                if (resp.success) _items.value = resp.data ?: emptyList()
                val cats = api.getCategories()
                if (cats.success) _categories.value = cats.data ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun create(request: CreateRecurringRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val resp = api.createRecurring(request)
                if (resp.success) {
                    load()
                    onSuccess()
                } else _error.value = resp.message
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                if (api.deleteRecurring(id).success) load()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun clearError() { _error.value = null }
}
