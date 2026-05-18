package com.maxshpl.myfit.products

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow

sealed interface DeleteProductResult {
    data object Success : DeleteProductResult
    data object InUse : DeleteProductResult
}

class ProductRepository(private val dao: ProductDao) {

    val visibleProducts: Flow<List<Product>> = dao.observeVisible()

    val allProducts: Flow<List<Product>> = dao.observeAll()

    suspend fun getById(id: Long): Product? = dao.getById(id)

    suspend fun insert(product: Product): Long = dao.insert(product)

    suspend fun update(product: Product) = dao.update(product)

    suspend fun delete(product: Product): DeleteProductResult = try {
        dao.delete(product)
        DeleteProductResult.Success
    } catch (e: SQLiteConstraintException) {
        DeleteProductResult.InUse
    }

    suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)
}
