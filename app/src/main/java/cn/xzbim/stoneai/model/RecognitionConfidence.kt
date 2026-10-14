package cn.xzbim.stoneai.model

/**
 * 识别可信度枚举
 */
enum class RecognitionConfidence(val displayName: String) {
    HIGH("较高可能"),
    MEDIUM("有一定可能"),
    LOW("暂时难以判断"),
    UNKNOWN("暂未判定");

    companion object {
        fun fromString(value: String?): RecognitionConfidence {
            return when (value?.lowercase()?.trim()) {
                "high" -> HIGH
                "medium" -> MEDIUM
                "low" -> LOW
                else -> UNKNOWN
            }
        }
    }
}
