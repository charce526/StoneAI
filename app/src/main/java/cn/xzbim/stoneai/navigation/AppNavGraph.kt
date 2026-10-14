package cn.xzbim.stoneai.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.xzbim.stoneai.model.ImageSource
import cn.xzbim.stoneai.share.SharePreviewScreen
import cn.xzbim.stoneai.ui.camera.CameraScreen
import cn.xzbim.stoneai.ui.history.HistoryDetailScreen
import cn.xzbim.stoneai.ui.history.HistoryScreen
import cn.xzbim.stoneai.ui.home.HomeScreen
import cn.xzbim.stoneai.ui.preview.ImagePreviewScreen
import cn.xzbim.stoneai.ui.result.RockResultScreen
import cn.xzbim.stoneai.ui.settings.AIServiceEditScreen
import cn.xzbim.stoneai.ui.settings.AIServiceListScreen
import cn.xzbim.stoneai.ui.settings.AboutScreen
import cn.xzbim.stoneai.ui.settings.DataManagementScreen
import cn.xzbim.stoneai.ui.settings.RecognitionSettingsScreen
import cn.xzbim.stoneai.ui.settings.SettingsScreen
import cn.xzbim.stoneai.viewmodel.ImageSelectionViewModel

/**
 * 主导航图与 App 外层 Shell 结构 (Soft & Friendly 自然重构风，包含 Photo Picker 与 CameraX 流程)
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    imageSelectionViewModel: ImageSelectionViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 一级导航路由集合
    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Settings.route
    )

    // 仅在一级页面展示底部导航栏
    val shouldShowBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 一级页面
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onNavigateToImagePreview = {
                        navController.navigate(Screen.ImagePreview.route)
                    },
                    onNavigateToAIService = {
                        navController.navigate(Screen.AIServiceList.route)
                    },
                    onHistoryItemClick = { entity ->
                        navController.navigate(Screen.HistoryDetail.createRoute(entity.id))
                    },
                    imageSelectionViewModel = imageSelectionViewModel
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onHistoryItemClick = { entity ->
                        navController.navigate(Screen.HistoryDetail.createRoute(entity.id))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAIService = {
                        navController.navigate(Screen.AIServiceList.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            // 历史详情路由
            composable(
                route = Screen.HistoryDetail.route,
                arguments = listOf(
                    navArgument("historyId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val historyId = backStackEntry.arguments?.getString("historyId") ?: ""
                HistoryDetailScreen(
                    historyId = historyId,
                    onBackClick = { navController.popBackStack() },
                    onShareClick = {
                        navController.navigate(Screen.SharePreviewHistory.createRoute(historyId))
                    }
                )
            }

            // 分享卡片预览路由 1: 来自实时识别结果
            composable(Screen.SharePreviewResult.route) {
                val uiState by imageSelectionViewModel.uiState.collectAsState()
                SharePreviewScreen(
                    historyId = null,
                    result = uiState.rockResult,
                    imageModel = uiState.selectedImage?.uri,
                    captureMetadata = uiState.captureMetadata,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 分享卡片预览路由 2: 来自历史记录
            composable(
                route = Screen.SharePreviewHistory.route,
                arguments = listOf(
                    navArgument("historyId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val historyId = backStackEntry.arguments?.getString("historyId") ?: ""
                SharePreviewScreen(
                    historyId = historyId,
                    result = null,
                    imageModel = null,
                    captureMetadata = null,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 相机拍摄与图片预览流程
            composable(Screen.Camera.route) {
                CameraScreen(
                    onBackClick = { navController.popBackStack() },
                    onImageCaptured = { uri ->
                        imageSelectionViewModel.setSelectedImage(uri, ImageSource.CAMERA)
                        navController.navigate(Screen.ImagePreview.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.ImagePreview.route) {
                ImagePreviewScreen(
                    onBackClick = { navController.popBackStack() },
                    onReselectClick = {
                        val source = imageSelectionViewModel.uiState.value.selectedImage?.source
                        if (source == ImageSource.CAMERA) {
                            navController.navigate(Screen.Camera.route) {
                                popUpTo(Screen.Home.route)
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToResult = {
                        navController.navigate(Screen.RockResult.route)
                    },
                    viewModel = imageSelectionViewModel
                )
            }

            composable(Screen.RockResult.route) {
                RockResultScreen(
                    onBackClick = {
                        imageSelectionViewModel.clearSelectedImage()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHome = {
                        imageSelectionViewModel.clearSelectedImage()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onRetakeClick = {
                        imageSelectionViewModel.clearSelectedImage()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onShareClick = {
                        navController.navigate(Screen.SharePreviewResult.route)
                    },
                    viewModel = imageSelectionViewModel
                )
            }

            // 二级页面 (AI 服务管理)
            composable(Screen.AIServiceList.route) {
                AIServiceListScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddConfigClick = {
                        navController.navigate(Screen.AIServiceAdd.route)
                    },
                    onEditConfigClick = { configId ->
                        navController.navigate(Screen.AIServiceEdit.createRoute(configId))
                    }
                )
            }
            composable(Screen.AIServiceAdd.route) {
                AIServiceEditScreen(
                    configId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AIServiceEdit.route,
                arguments = listOf(
                    navArgument("configId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val configId = backStackEntry.arguments?.getString("configId")
                AIServiceEditScreen(
                    configId = configId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 二级页面 (其他设置)
            composable(Screen.RecognitionSettings.route) {
                RecognitionSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.DataManagement.route) {
                DataManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                AboutScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
