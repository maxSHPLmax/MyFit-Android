package com.maxshpl.myfit.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditFormState(
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val errors: AddEditFormErrors = AddEditFormErrors(),
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val saveCompleted: Boolean = false,
    val notFound: Boolean = false,
)

data class AddEditFormErrors(
    val name: String? = null,
    val kcal: String? = null,
    val protein: String? = null,
    val fat: String? = null,
    val carbs: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || kcal != null || protein != null || fat != null || carbs != null
}

class AddEditProductViewModel(
    private val repository: ProductRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Long? = savedStateHandle.get<Long>(NavArgs.PRODUCT_ID)?.takeIf { it > 0L }
    private val isEditMode: Boolean = productId != null
    private var original: Product? = null

    private val _state = MutableStateFlow(
        AddEditFormState(isEditMode = isEditMode, isLoading = isEditMode),
    )
    val state: StateFlow<AddEditFormState> = _state.asStateFlow()

    init {
        if (productId != null) loadProduct(productId)
    }

    private fun loadProduct(id: Long) {
        viewModelScope.launch {
            val product = repository.getById(id)
            if (product == null) {
                _state.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            original = product
            _state.update {
                it.copy(
                    name = product.name,
                    kcal = product.kcalPer100g.toString(),
                    protein = formatFloat(product.proteinPer100g),
                    fat = formatFloat(product.fatPer100g),
                    carbs = formatFloat(product.carbsPer100g),
                    isLoading = false,
                )
            }
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onKcalChange(value: String) =
        _state.update { it.copy(kcal = value, errors = it.errors.copy(kcal = null)) }

    fun onProteinChange(value: String) =
        _state.update { it.copy(protein = value, errors = it.errors.copy(protein = null)) }

    fun onFatChange(value: String) =
        _state.update { it.copy(fat = value, errors = it.errors.copy(fat = null)) }

    fun onCarbsChange(value: String) =
        _state.update { it.copy(carbs = value, errors = it.errors.copy(carbs = null)) }

    fun save() {
        val current = _state.value
        val errors = validate(current)
        if (errors.hasErrors) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            val product = Product(
                id = original?.id ?: 0L,
                name = current.name.trim(),
                kcalPer100g = current.kcal.toInt(),
                proteinPer100g = current.protein.normalizeFloat(),
                fatPer100g = current.fat.normalizeFloat(),
                carbsPer100g = current.carbs.normalizeFloat(),
                isCustom = original?.isCustom ?: true,
                isHidden = original?.isHidden ?: false,
            )
            if (original == null) repository.insert(product) else repository.update(product)
            _state.update { it.copy(saveCompleted = true) }
        }
    }

    private fun validate(s: AddEditFormState): AddEditFormErrors = AddEditFormErrors(
        name = if (s.name.isBlank()) "Введите название" else null,
        kcal = validateInt(s.kcal),
        protein = validateFloat(s.protein),
        fat = validateFloat(s.fat),
        carbs = validateFloat(s.carbs),
    )

    private fun validateInt(raw: String): String? {
        if (raw.isBlank()) return "Заполните"
        val parsed = raw.toIntOrNull() ?: return "Целое число"
        if (parsed < 0) return "Не меньше 0"
        return null
    }

    private fun validateFloat(raw: String): String? {
        if (raw.isBlank()) return "Заполните"
        val parsed = raw.replace(',', '.').toFloatOrNull() ?: return "Число"
        if (parsed < 0f) return "Не меньше 0"
        return null
    }

    private fun String.normalizeFloat(): Float = replace(',', '.').toFloat()

    private fun formatFloat(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val dao = AppDatabase.get(app).productDao()
                AddEditProductViewModel(ProductRepository(dao), createSavedStateHandle())
            }
        }
    }
}
