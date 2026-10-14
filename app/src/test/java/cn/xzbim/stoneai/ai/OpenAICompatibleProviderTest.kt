package cn.xzbim.stoneai.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenAICompatibleProviderTest {

    private val provider = OpenAICompatibleProvider()

    @Test
    fun testParseChatCompletionResponse_contentAsString() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "OK"
                  }
                }
              ]
            }
        """.trimIndent()

        val method = OpenAICompatibleProvider::class.java.getDeclaredMethod("parseChatCompletionResponse", String::class.java)
        method.isAccessible = true
        val result = method.invoke(provider, json) as String?

        assertNotNull(result)
        assertEquals("OK", result)
    }

    @Test
    fun testParseChatCompletionResponse_contentAsArray() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": [
                      {
                        "type": "text",
                        "text": "Hello, world!"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val method = OpenAICompatibleProvider::class.java.getDeclaredMethod("parseChatCompletionResponse", String::class.java)
        method.isAccessible = true
        val result = method.invoke(provider, json) as String?

        assertNotNull(result)
        assertEquals("Hello, world!", result)
    }

    @Test
    fun testParseChatCompletionResponse_textDirect() {
        val json = """
            {
              "choices": [
                {
                  "text": "Response text direct"
                }
              ]
            }
        """.trimIndent()

        val method = OpenAICompatibleProvider::class.java.getDeclaredMethod("parseChatCompletionResponse", String::class.java)
        method.isAccessible = true
        val result = method.invoke(provider, json) as String?

        assertNotNull(result)
        assertEquals("Response text direct", result)
    }

    @Test
    fun testIsImageGenerationModelNotice() {
        val json = """{"error": {"message": "Model Agnes is an image generation model. Please use /v1/images/generations."}}"""
        val method = OpenAICompatibleProvider::class.java.getDeclaredMethod("isImageGenerationModelNotice", String::class.java)
        method.isAccessible = true
        val isImageModel = method.invoke(provider, json) as Boolean

        assertEquals(true, isImageModel)
    }
}
