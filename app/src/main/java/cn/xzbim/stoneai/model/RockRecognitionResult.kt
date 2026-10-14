package cn.xzbim.stoneai.model

/**
 * 结构化岩石/矿物识别结果数据模型
 */
data class RockRecognitionResult(
    val name: String,
    val englishName: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val confidence: RecognitionConfidence = RecognitionConfidence.UNKNOWN,
    val summary: String,
    val reasoning: String,
    val features: List<String> = emptyList(),
    val composition: List<String> = emptyList(),
    val formation: String? = null,
    val environment: String? = null,
    val uses: List<String> = emptyList(),
    val alternatives: List<AlternativeRock> = emptyList(),
    val suggestedTests: List<String> = emptyList(),
    val warning: String? = null
) {
    val isRock: Boolean
        get() = !name.contains("未识别为", ignoreCase = true) &&
                !name.contains("非岩石", ignoreCase = true) &&
                !name.contains("不是石头", ignoreCase = true)
}
