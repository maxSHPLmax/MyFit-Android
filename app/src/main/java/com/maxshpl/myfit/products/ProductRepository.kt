package com.maxshpl.myfit.products

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {

    val visibleProducts: Flow<List<Product>> = dao.observeVisible()

    val allProducts: Flow<List<Product>> = dao.observeAll()

    suspend fun getById(id: Long): Product? = dao.getById(id)

    suspend fun insert(product: Product): Long = dao.insert(product)

    suspend fun update(product: Product) = dao.update(product)

    suspend fun delete(product: Product) = dao.delete(product)

    suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)
}
