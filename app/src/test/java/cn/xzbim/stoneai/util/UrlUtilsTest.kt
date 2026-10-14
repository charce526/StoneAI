package cn.xzbim.stoneai.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun testBuildEndpointUrl_standardUrl() {
        val result = UrlUtils.buildEndpointUrl("https://api.openai.com/v1", "/models")
        assertEquals("https://api.openai.com/v1/models", result)
    }

    @Test
    fun testBuildEndpointUrl_baseUrlWithTrailingSlash() {
        val result = UrlUtils.buildEndpointUrl("https://api.openai.com/v1/", "/models")
        assertEquals("https://api.openai.com/v1/models", result)
    }

    @Test
    fun testBuildEndpointUrl_pathWithoutLeadingSlash() {
        val result = UrlUtils.buildEndpointUrl("https://api.openai.com/v1", "models")
        assertEquals("https://api.openai.com/v1/models", result)
    }

    @Test
    fun testBuildEndpointUrl_baseUrlWithWhitespace() {
        val result = UrlUtils.buildEndpointUrl("  https://api.openai.com/v1/  ", "chat/completions")
        assertEquals("https://api.openai.com/v1/chat/completions", result)
    }
}
