package cn.xzbim.stoneai.navigation

/**
 * 应用路由定义
 */
sealed class Screen(val route: String) {
    // 一级导航路由
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    // 历史详情路由
    data object HistoryDetail : Screen("history/{historyId}") {
        fun createRoute(historyId: String): String = "history/$historyId"
    }

    // 分享卡片预览路由
    data object SharePreviewResult : Screen("share_preview/result")
    data object SharePreviewHistory : Screen("share_preview/history/{historyId}") {
        fun createRoute(historyId: String): String = "share_preview/history/$historyId"
    }

    // 相机、图片预览与岩石识别结果路由
    data object Camera : Screen("camera")
    data object ImagePreview : Screen("image_preview")
    data object RockResult : Screen("rock_result")

    // 设置二级与 AI 服务配置路由
    data object AIServiceList : Screen("settings/ai_service")
    data object AIServiceAdd : Screen("settings/ai_service/add")
    data object AIServiceEdit : Screen("settings/ai_service/edit/{configId}") {
        fun createRoute(configId: String): String = "settings/ai_service/edit/$configId"
    }

    data object RecognitionSettings : Screen("settings/recognition")
    data object DataManagement : Screen("settings/data_management")
    data object About : Screen("settings/about")
}
