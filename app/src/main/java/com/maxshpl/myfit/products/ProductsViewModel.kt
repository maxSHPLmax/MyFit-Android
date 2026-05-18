package com.maxshpl.myfit.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    data object Loading : ProductsUiState
    data class Success(val products: List<Product>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

class ProductsViewModel(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    val uiState: StateFlow<ProductsUiState> = combine(
        repository.allProducts,
        _query,
        _showHidden,
    ) { products, query, showHidden ->
        val filtered = products
            .filter { showHidden || !it.isHidden }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        ProductsUiState.Success(filtered) as ProductsUiState
    }
        .catch { emit(ProductsUiState.Error(it.message ?: "Не удалось загрузить продукты")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ProductsUiState.Loading,
        )

    fun setQuery(value: String) = _query.update { value }

    fun setShowHidden(value: Boolean) = _showHidden.update { value }

    fun hide(product: Product) {
        viewModelScope.launch { repository.setHidden(product.id, true) }
    }

    fun unhide(product: Product) {
        viewModelScope.launch { repository.setHidden(product.id, false) }
    }

    fun delete(product: Product) {
        viewModelScope.launch { repository.delete(product) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val dao = AppDatabase.get(application).productDao()
                ProductsViewModel(ProductRepository(dao))
            }
        }
    }
}
