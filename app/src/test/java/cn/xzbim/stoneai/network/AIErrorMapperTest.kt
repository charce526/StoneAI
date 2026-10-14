package cn.xzbim.stoneai.network

import cn.xzbim.stoneai.model.AIError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

class AIErrorMapperTest {

    @Test
    fun testMapHttpResponse_401ReturnsInvalidApiKey() {
        val error = AIErrorMapper.mapHttpResponse(401, """{"error": {"message": "Invalid API key"}}""")
        assertEquals(AIError.InvalidApiKey, error)
    }

    @Test
    fun testMapHttpResponse_429QuotaExceeded() {
        val error = AIErrorMapper.mapHttpResponse(429, """{"error": {"message": "You exceeded your current quota, please check your plan and billing details."}}""")
        assertEquals(AIError.QuotaExceeded, error)
    }

    @Test
    fun testMapHttpResponse_429RateLimited() {
        val error = AIErrorMapper.mapHttpResponse(429, """{"error": {"message": "Rate limit reached for requests."}}""")
        assertEquals(AIError.RateLimited, error)
    }

    @Test
    fun testMapHttpResponse_VisionNotSupported() {
        val error = AIErrorMapper.mapHttpResponse(400, """{"error": {"message": "The model gpt-3.5-turbo does not support image input."}}""")
        assertEquals(AIError.VisionNotSupported, error)
    }

    @Test
    fun testMapHttpResponse_ImageTooLarge() {
        val error = AIErrorMapper.mapHttpResponse(413, """{"error": {"message": "Payload too large"}}""")
        assertEquals(AIError.ImageTooLarge, error)
    }

    @Test
    fun testMapThrowable_UnknownHostReturnsInvalidBaseUrl() {
        val error = AIErrorMapper.mapThrowable(UnknownHostException("Unable to resolve host"))
        assertEquals(AIError.InvalidBaseUrl, error)
    }
}
