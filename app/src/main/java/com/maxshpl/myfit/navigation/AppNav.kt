package com.maxshpl.myfit.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
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
import com.maxshpl.myfit.activities.ActivitiesScreen
import com.maxshpl.myfit.activities.AddEditActivityScreen
import com.maxshpl.myfit.diary.AddActivityLogScreen
import com.maxshpl.myfit.diary.AddDiaryEntryScreen
import com.maxshpl.myfit.diary.DiaryScreen
import com.maxshpl.myfit.plan.AddEditMealScreen
import com.maxshpl.myfit.plan.PlanScreen
import com.maxshpl.myfit.products.AddEditProductScreen
import com.maxshpl.myfit.products.ProductsScreen

object Routes {
    const val DIARY = "diary"
    const val DIARY_ADD = "diary/add?date={date}"
    const val DIARY_EDIT = "diary/edit/{entryId}"
    const val DIARY_ADD_ACTIVITY = "diary/activity/add?date={date}"
    const val DIARY_EDIT_ACTIVITY = "diary/activity/edit/{logId}"
    const val PLAN = "plan"
    const val PLAN_NEW_MEAL = "plan/meal/new?dayOfWeek={dayOfWeek}"
    const val PLAN_EDIT_MEAL = "plan/meal/edit/{mealId}"
    const val HISTORY = "history"
    const val PRODUCTS = "products"
    const val PRODUCT_NEW = "products/new"
    const val PRODUCT_EDIT = "products/edit/{id}"
    const val ACTIVITIES = "activities"
    const val ACTIVITY_NEW = "activities/new"
    const val ACTIVITY_EDIT = "activities/edit/{activityId}"
    const val SETTINGS = "settings"

    fun productEdit(id: Long): String = "products/edit/$id"

    fun activityEdit(id: Long): String = "activities/edit/$id"

    fun diaryAdd(date: String): String = "diary/add?date=$date"

    fun diaryEdit(entryId: Long): String = "diary/edit/$entryId"

    fun diaryAddActivity(date: String): String = "diary/activity/add?date=$date"

    fun diaryEditActivity(logId: Long): String = "diary/activity/edit/$logId"

    fun planNewMeal(dayOfWeek: java.time.DayOfWeek): String =
        "plan/meal/new?dayOfWeek=${dayOfWeek.name}"

    fun planEditMeal(mealId: Long): String = "plan/meal/edit/$mealId"
}

object NavArgs {
    const val PRODUCT_ID = "id"
    const val ACTIVITY_ID = "activityId"
    const val DATE = "date"
    const val ENTRY_ID = "entryId"
    const val LOG_ID = "logId"
    const val MEAL_ID = "mealId"
    const val DAY_OF_WEEK = "dayOfWeek"
}

private enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Diary(Routes.DIARY, "Дневник", Icons.Default.Home),
    Plan(Routes.PLAN, "План", Icons.Default.DateRange),
    History(Routes.HISTORY, "История", Icons.Default.Refresh),
    Products(Routes.PRODUCTS, "Продукты", Icons.AutoMirrored.Filled.List),
    Settings(Routes.SETTINGS, "Настройки", Icons.Default.Settings),
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
                    onAddProductClick = { date ->
                        navController.navigate(Routes.diaryAdd(date.toString()))
                    },
                    onAddActivityClick = { date ->
                        navController.navigate(Routes.diaryAddActivity(date.toString()))
                    },
                    onEditEntryClick = { id ->
                        navController.navigate(Routes.diaryEdit(id))
                    },
                    onEditActivityLogClick = { id ->
                        navController.navigate(Routes.diaryEditActivity(id))
                    },
                    onOpenPlanClick = {
                        navController.navigate(Routes.PLAN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(
                route = Routes.DIARY_ADD,
                arguments = listOf(
                    navArgument(NavArgs.DATE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                AddDiaryEntryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.DIARY_EDIT,
                arguments = listOf(navArgument(NavArgs.ENTRY_ID) { type = NavType.LongType }),
            ) {
                AddDiaryEntryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.DIARY_ADD_ACTIVITY,
                arguments = listOf(
                    navArgument(NavArgs.DATE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                AddActivityLogScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToActivities = { navController.navigate(Routes.ACTIVITIES) },
                )
            }
            composable(
                route = Routes.DIARY_EDIT_ACTIVITY,
                arguments = listOf(navArgument(NavArgs.LOG_ID) { type = NavType.LongType }),
            ) {
                AddActivityLogScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToActivities = { navController.navigate(Routes.ACTIVITIES) },
                )
            }
            composable(Routes.PLAN) {
                PlanScreen(
                    onAddMealClick = { day ->
                        navController.navigate(Routes.planNewMeal(day))
                    },
                    onEditMealClick = { id ->
                        navController.navigate(Routes.planEditMeal(id))
                    },
                )
            }
            composable(
                route = Routes.PLAN_NEW_MEAL,
                arguments = listOf(
                    navArgument(NavArgs.DAY_OF_WEEK) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                AddEditMealScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.PLAN_EDIT_MEAL,
                arguments = listOf(navArgument(NavArgs.MEAL_ID) { type = NavType.LongType }),
            ) {
                AddEditMealScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                PlaceholderScreen(title = "История")
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
            composable(Routes.ACTIVITIES) {
                ActivitiesScreen(
                    onAddClick = { navController.navigate(Routes.ACTIVITY_NEW) },
                    onEditClick = { id -> navController.navigate(Routes.activityEdit(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACTIVITY_NEW) {
                AddEditActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ACTIVITY_EDIT,
                arguments = listOf(navArgument(NavArgs.ACTIVITY_ID) { type = NavType.LongType }),
            ) {
                AddEditActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                PlaceholderScreen(
                    title = "Настройки",
                    extraContent = {
                        // KAN-16: временная debug-кнопка. УДАЛИТЬ при реализации KAN-9
                        // (полноценный экран настроек заменит этот placeholder и должен
                        // содержать пункт "Управление активностями" в основном меню).
                        FilledTonalButton(
                            onClick = { navController.navigate(Routes.ACTIVITIES) },
                        ) {
                            Text("Управление активностями (dev)")
                        }
                    },
                )
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
