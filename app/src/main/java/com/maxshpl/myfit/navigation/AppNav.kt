package com.maxshpl.myfit.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maxshpl.myfit.diary.AddDiaryEntryScreen
import com.maxshpl.myfit.diary.DiaryScreen
import com.maxshpl.myfit.products.AddEditProductScreen
import com.maxshpl.myfit.products.ProductsScreen

object Routes {
    const val DIARY = "diary"
    const val DIARY_ADD = "diary/add/{mealType}"
    const val PRODUCTS = "products"
    const val PRODUCT_NEW = "products/new"
    const val PRODUCT_EDIT = "products/edit/{id}"

    fun diaryAdd(mealType: String): String = "diary/add/$mealType"

    fun productEdit(id: Long): String = "products/edit/$id"
}

object NavArgs {
    const val PRODUCT_ID = "id"
    const val MEAL_TYPE = "mealType"
}

private enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Diary(Routes.DIARY, "Дневник", Icons.Default.DateRange),
    Products(Routes.PRODUCTS, "Продукты", Icons.AutoMirrored.Filled.List),
}

private val TopLevelRoutes: Set<String> = TopLevelTab.entries.map { it.route }.toSet()

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in TopLevelRoutes) {
                AppBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DIARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DIARY) {
                DiaryScreen(
                    onAddClick = { mealType ->
                        navController.navigate(Routes.diaryAdd(mealType.name))
                    },
                )
            }
            composable(
                route = Routes.DIARY_ADD,
                arguments = listOf(navArgument(NavArgs.MEAL_TYPE) { type = NavType.StringType }),
            ) {
                AddDiaryEntryScreen(onBack = { navController.popBackStack() })
            }
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
}

@Composable
private fun AppBottomBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
