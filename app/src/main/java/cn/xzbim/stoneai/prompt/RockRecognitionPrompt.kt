package cn.xzbim.stoneai.prompt

/**
 * 独立 Prompt 管理模块：负责定义 System Prompt 与 User Prompt (精简高效版，防止模型推理超时)
 */
object RockRecognitionPrompt {

    val SYSTEM_PROMPT = """
你是一个岩石矿物识别科普助手。根据用户提供的照片和JSON格式要求返回分析结果。

名称输出规则：
1. name 必须简短、干练，仅写明对象名称或状态（如：花岗岩、石英、未知岩石、非岩石目标、无法识别）。
2. 绝对不能包含“可能是”、“疑似”、“大概率”等猜测语气或解释说明。
3. 明显不是岩石时返回“非岩石目标”；无法判定具体种类时返回“未知岩石”或“未知矿物”；无法辨认时返回“无法识别”。
4. 严禁输出 Markdown 代码块标识（如 ```json），直接输出纯 JSON 对象。
""".trimIndent()

    val USER_PROMPT = """
请分析这张照片中的石头或矿物，严格按以下 JSON 格式返回：

{
  "name": "花岗岩 / 未知岩石 / 非岩石目标 / 无法识别",
  "englishName": "英文名称",
  "category": "大类",
  "subCategory": "子类",
  "confidence": "high / medium / low",
  "summary": "一句话科普介绍",
  "reasoning": "视觉特征依据",
  "features": ["特征1", "特征2"],
  "composition": ["成分1", "成分2"],
  "formation": "形成方式",
  "environment": "地质环境",
  "uses": ["用途1"],
  "alternatives": [{"name": "候选项1", "reason": "原因"}],
  "suggestedTests": ["建议1"],
  "warning": "局限性提示"
}
""".trimIndent()

    fun buildUserPrompt(languageInstruction: String? = null): String {
        val langSuffix = if (!languageInstruction.isNullOrBlank()) {
            "\n\n语言补充要求：$languageInstruction。"
        } else ""
        return "$USER_PROMPT$langSuffix"
    }
}
