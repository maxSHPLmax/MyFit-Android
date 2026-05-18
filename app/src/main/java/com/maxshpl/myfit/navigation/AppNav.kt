package com.maxshpl.myfit.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maxshpl.myfit.products.AddEditProductScreen
import com.maxshpl.myfit.products.ProductsScreen

object Routes {
    const val PRODUCTS = "products"
    const val PRODUCT_NEW = "products/new"
    const val PRODUCT_EDIT = "products/edit/{id}"

    fun productEdit(id: Long): String = "products/edit/$id"
}

object NavArgs {
    const val PRODUCT_ID = "id"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.PRODUCTS,
    ) {
        composable(Routes.PRODUCTS) {
            ProductsScreen(
                onAddClick = { navController.navigate(Routes.PRODUCT_NEW) },
                onEditClick = { id -> navController.navigate(Routes.productEdit(id)) },
            )
        }
        composable(Routes.PRODUCT_NEW) {
            AddEditProductScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PRODUCT_EDIT,
            arguments = listOf(navArgument(NavArgs.PRODUCT_ID) { type = NavType.LongType }),
        ) {
            AddEditProductScreen(onBack = { navController.popBackStack() })
        }
    }
}
