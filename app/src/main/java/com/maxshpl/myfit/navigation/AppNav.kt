package com.maxshpl.myfit.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maxshpl.myfit.products.ProductsScreen

object Routes {
    const val PRODUCTS = "products"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.PRODUCTS,
    ) {
        composable(Routes.PRODUCTS) {
            ProductsScreen()
        }
    }
}
