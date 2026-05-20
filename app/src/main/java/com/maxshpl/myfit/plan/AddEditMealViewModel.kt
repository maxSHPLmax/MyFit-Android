package com.maxshpl.myfit.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.navigation.NavArgs
import com.maxshpl.myfit.products.Product
import com.maxshpl.myfit.products.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class MealItemDraft(
    val productId: Long,
    val productName: String,
    val grams: Double,
    val kcalPer100g: Int,
    val proteinPer100g: Float,
    val fatPer100g: Float,
    val carbsPer100g: Float,
) {
    val kcal: Double get() = kcalPer100g * grams / 100.0
    val protein: Double get() = proteinPer100g * grams / 100.0
    val fat: Double get() = fatPer100g * grams / 100.0
    val carbs: Double get() = carbsPer100g * grams / 100.0
}

sealed interface ItemPicker {
    data object Hidden : ItemPicker
    data class ProductSearch(val query: String) : ItemPicker
    data class GramsEntry(val product: Product, val gramsText: String, val error: String?) :
        ItemPicker
}

data class AddEditMealUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val name: String = "",
    val nameError: String? = null,
    val time: String? = null,
    val items: List<MealItemDraft> = emptyList(),
    val picker: ItemPicker = ItemPicker.Hidden,
    val visibleProducts: List<Product> = emptyList(),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
)

class AddEditMealViewModel(
    private val planRepository: PlanRepository,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingMealId: Long? = savedStateHandle.get<Long>(NavArgs.MEAL_ID)
    private val initialDayOfWeek: DayOfWeek = parseDayOfWeekArg(
        savedStateHandle[NavArgs.DAY_OF_WEEK],
    )

    private val _name = MutableStateFlow("")
    private val _nameError = MutableStateFlow<String?>(null)
    private val _time = MutableStateFlow<String?>(null)
    private val _items = MutableStateFlow<List<MealItemDraft>>(emptyList())
    private val _dayOfWeek = MutableStateFlow(initialDayOfWeek)
    private val _picker = MutableStateFlow<ItemPicker>(ItemPicker.Hidden)
    private val _isLoading = MutableStateFlow(editingMealId != null)
    private val _isSaving = MutableStateFlow(false)
    private val _saveCompleted = MutableStateFlow(false)

    val uiState: StateFlow<AddEditMealUiState> = combine(
        combine(_name, _nameError, _time, _dayOfWeek, ::FormBlock),
        _items,
        _picker,
        productRepository.visibleProducts,
        combine(_isLoading, _isSaving, _saveCompleted, ::StatusBlock),
    ) { form, items, picker, products, status ->
        AddEditMealUiState(
            isLoading = status.isLoading,
            isEditing = editingMealId != null,
            dayOfWeek = form.dayOfWeek,
            name = form.name,
            nameError = form.nameError,
            time = form.time,
            items = items,
            picker = picker,
            visibleProducts = filterProducts(products, picker),
            isSaving = status.isSaving,
            saveCompleted = status.saveCompleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AddEditMealUiState(
            isLoading = editingMealId != null,
            isEditing = editingMealId != null,
            dayOfWeek = initialDayOfWeek,
        ),
    )

    init {
        if (editingMealId != null) {
            viewModelScope.launch {
                val meal = planRepository.getMealById(editingMealId)
                if (meal == null) {
                    _isLoading.update { false }
                    return@launch
                }
                _name.update { meal.name }
                _time.update { meal.time }
                _dayOfWeek.update { meal.dayOfWeek }
                // Берём только первое значение — items загружаются один раз для редактора.
                val rows = planRepository.observeItemsForMeal(editingMealId).first()
                _items.update {
                    rows.map { row ->
                        MealItemDraft(
                            productId = row.productId,
                            productName = row.productName,
                            grams = row.grams,
                            kcalPer100g = (row.kcal / row.grams * 100.0).toInt(),
                            proteinPer100g = (row.protein / row.grams * 100.0).toFloat(),
                            fatPer100g = (row.fat / row.grams * 100.0).toFloat(),
                            carbsPer100g = (row.carbs / row.grams * 100.0).toFloat(),
                        )
                    }
                }
                _isLoading.update { false }
            }
        }
    }

    fun setName(value: String) {
        _name.update { value }
        if (_nameError.value != null) _nameError.update { null }
    }

    fun setTime(value: String?) {
        _time.update { value }
    }

    fun removeItem(index: Int) {
        _items.update { current -> current.toMutableList().also { it.removeAt(index) } }
    }

    fun openProductPicker() {
        _picker.update { ItemPicker.ProductSearch(query = "") }
    }

    fun closePicker() {
        _picker.update { ItemPicker.Hidden }
    }

    fun setProductQuery(query: String) {
        val current = _picker.value
        if (current is ItemPicker.ProductSearch) {
            _picker.update { ItemPicker.ProductSearch(query = query) }
        }
    }

    fun selectProduct(product: Product) {
        _picker.update { ItemPicker.GramsEntry(product = product, gramsText = "", error = null) }
    }

    fun setGramsText(value: String) {
        val current = _picker.value
        if (current is ItemPicker.GramsEntry) {
            _picker.update { current.copy(gramsText = value, error = null) }
        }
    }

    fun confirmItem() {
        val current = _picker.value
        if (current !is ItemPicker.GramsEntry) return
        val grams = parseGrams(current.gramsText)
        if (grams == null) {
            _picker.update {
                current.copy(error = "Введите число больше 0 (например, 150 или 10,5)")
            }
            return
        }
        val product = current.product
        val draft = MealItemDraft(
            productId = product.id,
            productName = product.name,
            grams = grams,
            kcalPer100g = product.kcalPer100g,
            proteinPer100g = product.proteinPer100g,
            fatPer100g = product.fatPer100g,
            carbsPer100g = product.carbsPer100g,
        )
        _items.update { it + draft }
        _picker.update { ItemPicker.Hidden }
    }

    fun save() {
        if (_isSaving.value) return
        val trimmedName = _name.value.trim()
        if (trimmedName.isEmpty()) {
            _nameError.update { "Введите название приёма" }
            return
        }
        _isSaving.update { true }
        viewModelScope.launch {
            val meal = PlannedMeal(
                id = editingMealId ?: 0L,
                dayOfWeek = _dayOfWeek.value,
                name = trimmedName,
                time = _time.value,
            )
            val items = _items.value.map { draft ->
                PlannedMealItem(
                    mealId = editingMealId ?: 0L,
                    productId = draft.productId,
                    grams = draft.grams,
                )
            }
            planRepository.saveMeal(meal, items)
            _saveCompleted.update { true }
        }
    }

    private fun filterProducts(all: List<Product>, picker: ItemPicker): List<Product> {
        if (picker !is ItemPicker.ProductSearch) return emptyList()
        val q = picker.query.trim()
        return if (q.isBlank()) all
        else all.filter { it.name.contains(q, ignoreCase = true) }
    }

    private fun parseGrams(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return value.takeIf { it > 0.0 }
    }

    private data class FormBlock(
        val name: String,
        val nameError: String?,
        val time: String?,
        val dayOfWeek: DayOfWeek,
    )

    private data class StatusBlock(
        val isLoading: Boolean,
        val isSaving: Boolean,
        val saveCompleted: Boolean,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        private fun parseDayOfWeekArg(raw: String?): DayOfWeek = raw?.let {
            try {
                DayOfWeek.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        } ?: java.time.LocalDate.now().dayOfWeek

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(app)
                AddEditMealViewModel(
                    planRepository = PlanRepository(
                        planDao = db.planDao(),
                        diaryEntryDao = db.diaryEntryDao(),
                    ),
                    productRepository = ProductRepository(db.productDao()),
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
