package com.maxshpl.myfit.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    data object Loading : ProductsUiState
    data class Success(val products: List<Product>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

class ProductsViewModel(
    private val repository: ProductRepository,
) : ViewModel() {

    val uiState: StateFlow<ProductsUiState> = repository.visibleProducts
        .toUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ProductsUiState.Loading,
        )

    fun hide(product: Product) {
        viewModelScope.launch { repository.setHidden(product.id, true) }
    }

    fun delete(product: Product) {
        viewModelScope.launch { repository.delete(product) }
    }

    private fun Flow<List<Product>>.toUiState(): Flow<ProductsUiState> =
        map<List<Product>, ProductsUiState> { ProductsUiState.Success(it) }
            .catch { emit(ProductsUiState.Error(it.message ?: "Не удалось загрузить продукты")) }

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
