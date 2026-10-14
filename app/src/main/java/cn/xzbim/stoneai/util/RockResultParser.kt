package cn.xzbim.stoneai.util

import cn.xzbim.stoneai.model.AlternativeRock
import cn.xzbim.stoneai.model.RecognitionConfidence
import cn.xzbim.stoneai.model.RockRecognitionResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * 容错 JSON 清理与解析器：负责将 AI 返回的文本清理为干净的 JSON 并转换为 RockRecognitionResult
 */
object RockResultParser {

    /**
     * 清理原始文本中的 Markdown 代码块或多余说明，提取包含 JSON 的 {...} 子串
     */
    fun cleanJsonString(rawText: String): String {
        var text = rawText.trim()

        // 移除 ```json 或 ``` 代码块前缀和后缀
        if (text.startsWith("```")) {
            text = text.replace(Regex("^```[a-zA-Z]*\\s*"), "")
            text = text.replace(Regex("\\s*```$"), "")
            text = text.trim()
        }

        // 寻找第一个 '{' 和最后一个 '}'
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')

        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1)
        } else {
            text
        }
    }

    /**
     * 解析 AI 返回的文本为结构化 RockRecognitionResult
     */
    fun parse(rawText: String): RockRecognitionResult {
        val cleanedJson = cleanJsonString(rawText)

        return try {
            val json = JSONObject(cleanedJson)

            val name = json.optString("name", "未识别的岩石样品")
            val englishName = json.optString("englishName", "").ifBlank { null }
            val category = json.optString("category", "").ifBlank { null }
            val subCategory = json.optString("subCategory", "").ifBlank { null }
            val confidenceStr = json.optString("confidence", "unknown")
            val confidence = RecognitionConfidence.fromString(confidenceStr)

            val summary = json.optString("summary", "AI 已对照片进行了初步视觉观察。")
            val reasoning = json.optString("reasoning", "基于照片中呈现的颜色、颗粒与纹理特征进行的判断。")

            val features = parseStringList(json.optJSONArray("features"))
            val composition = parseStringList(json.optJSONArray("composition"))

            val formation = json.optString("formation", "").ifBlank { null }
            val environment = json.optString("environment", "").ifBlank { null }

            val uses = parseStringList(json.optJSONArray("uses"))
            val alternatives = parseAlternativeRocks(json.optJSONArray("alternatives"))
            val suggestedTests = parseStringList(json.optJSONArray("suggestedTests"))

            val warning = json.optString("warning", "").ifBlank { null }

            RockRecognitionResult(
                name = name,
                englishName = englishName,
                category = category,
                subCategory = subCategory,
                confidence = confidence,
                summary = summary,
                reasoning = reasoning,
                features = features,
                composition = composition,
                formation = formation,
                environment = environment,
                uses = uses,
                alternatives = alternatives,
                suggestedTests = suggestedTests,
                warning = warning
            )
        } catch (_: Exception) {
            // 解析 JSON 容错降级，避免崩溃
            RockRecognitionResult(
                name = "初步视觉分析完成",
                confidence = RecognitionConfidence.LOW,
                summary = rawText.take(200),
                reasoning = rawText,
                warning = "AI 返回的数据未完全符合结构化格式，已展示原始文本。"
            )
        }
    }

    private fun parseStringList(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optString(i, "").trim()
            if (item.isNotBlank()) {
                list.add(item)
            }
        }
        return list
    }

    private fun parseAlternativeRocks(jsonArray: JSONArray?): List<AlternativeRock> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<AlternativeRock>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null) {
                val name = obj.optString("name", "").trim()
                val reason = obj.optString("reason", "").ifBlank { null }
                if (name.isNotBlank()) {
                    list.add(AlternativeRock(name = name, reason = reason))
                }
            }
        }
        return list
    }
}
