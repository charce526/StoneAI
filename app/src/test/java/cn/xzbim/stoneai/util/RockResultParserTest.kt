package cn.xzbim.stoneai.util

import cn.xzbim.stoneai.model.RecognitionConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RockResultParserTest {

    @Test
    fun testCleanJsonString_markdownCodeFences() {
        val raw = """
            ```json
            {
              "name": "花岗岩",
              "confidence": "high"
            }
            ```
        """.trimIndent()

        val cleaned = RockResultParser.cleanJsonString(raw)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
    }

    @Test
    fun testParse_validRockJson() {
        val raw = """
            {
              "name": "花岗岩",
              "englishName": "Granite",
              "category": "火成岩",
              "subCategory": "深成岩",
              "confidence": "high",
              "summary": "一种常见的火成岩。",
              "reasoning": "晶体结构明显。",
              "features": ["粗粒结构", "结晶明显"],
              "composition": ["石英", "长石"],
              "uses": ["建材"]
            }
        """.trimIndent()

        val result = RockResultParser.parse(raw)
        assertEquals("花岗岩", result.name)
        assertEquals("Granite", result.englishName)
        assertEquals("火成岩", result.category)
        assertEquals(RecognitionConfidence.HIGH, result.confidence)
        assertEquals(2, result.features.size)
        assertEquals("石英", result.composition[0])
        assertTrue(result.isRock)
    }

    @Test
    fun testParse_nonRockJson() {
        val raw = """
            {
              "name": "未识别为岩石或矿物",
              "confidence": "low",
              "summary": "图片中未发现明显的石头。"
            }
        """.trimIndent()

        val result = RockResultParser.parse(raw)
        assertEquals("未识别为岩石或矿物", result.name)
        assertEquals(RecognitionConfidence.LOW, result.confidence)
        assertEquals(false, result.isRock)
    }

    @Test
    fun testParse_malformedJsonFallback() {
        val raw = "这是一段普通的文本，不是合法的 JSON。"
        val result = RockResultParser.parse(raw)
        assertNotNull(result)
        assertEquals(RecognitionConfidence.LOW, result.confidence)
    }
}
