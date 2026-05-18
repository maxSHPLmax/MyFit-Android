package com.maxshpl.myfit.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.products.Product
import com.maxshpl.myfit.products.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddDiaryEntryUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val gramsText: String = "",
    val gramsError: String? = null,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
)

class AddDiaryEntryViewModel(
    private val productRepository: ProductRepository,
    private val diaryRepository: DiaryRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    private val _query = MutableStateFlow("")
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    private val _gramsText = MutableStateFlow("")
    private val _gramsError = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _saveCompleted = MutableStateFlow(false)

    private val filteredProducts: StateFlow<List<Product>> = combine(
        productRepository.visibleProducts,
        _query,
    ) { products, q ->
        if (q.isBlank()) products
        else products.filter { it.name.contains(q.trim(), ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    val uiState: StateFlow<AddDiaryEntryUiState> = combine(
        _query,
        filteredProducts,
        _selectedProduct,
        combine(_gramsText, _gramsError, _isSaving, _saveCompleted, ::Quad),
    ) { query, products, selected, gramsBlock ->
        AddDiaryEntryUiState(
            query = query,
            products = products,
            selectedProduct = selected,
            gramsText = gramsBlock.gramsText,
            gramsError = gramsBlock.gramsError,
            isSaving = gramsBlock.isSaving,
            saveCompleted = gramsBlock.saveCompleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AddDiaryEntryUiState(),
    )

    fun setQuery(value: String) = _query.update { value }

    fun selectProduct(product: Product) {
        _selectedProduct.update { product }
        _gramsText.update { "" }
        _gramsError.update { null }
    }

    fun clearSelection() {
        _selectedProduct.update { null }
        _gramsText.update { "" }
        _gramsError.update { null }
    }

    fun setGramsText(value: String) {
        _gramsText.update { value }
        if (_gramsError.value != null) _gramsError.update { null }
    }

    fun save() {
        val product = _selectedProduct.value ?: return
        val grams = parseGrams(_gramsText.value)
        if (grams == null) {
            _gramsError.update { "Введите число больше 0 (например, 150 или 10,5)" }
            return
        }
        if (_isSaving.value) return
        _isSaving.update { true }
        viewModelScope.launch {
            diaryRepository.add(date = today, productId = product.id, grams = grams)
            _saveCompleted.update { true }
        }
    }

    private fun parseGrams(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return value.takeIf { it > 0.0 }
    }

    private data class Quad(
        val gramsText: String,
        val gramsError: String?,
        val isSaving: Boolean,
        val saveCompleted: Boolean,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(app)
                AddDiaryEntryViewModel(
                    productRepository = ProductRepository(db.productDao()),
                    diaryRepository = DiaryRepository(db.diaryEntryDao()),
                )
            }
        }
    }
}
