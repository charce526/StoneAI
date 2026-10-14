package cn.xzbim.stoneai.model

/**
 * 图片处理质量设置
 */
enum class ImageQuality(val displayName: String, val maxEdge: Int, val quality: Int) {
    SAVER("节省流量 (1280px)", 1280, 75),
    STANDARD("标准质量 (1600px)", 1600, 80),
    HIGH("高清质量 (2048px)", 2048, 85);

    companion object {
        fun fromName(name: String?): ImageQuality {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: STANDARD
        }
    }
}

/**
 * AI 识别语言偏好设置
 */
enum class RecognitionLanguage(val displayName: String, val promptInstruction: String) {
    SYSTEM("跟随系统", "请优先使用简体中文返回分析结果"),
    ZH_CN("简体中文", "请严格使用简体中文返回分析结果"),
    EN("English", "Please return all analysis results in English");

    companion object {
        fun fromName(name: String?): RecognitionLanguage {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}
