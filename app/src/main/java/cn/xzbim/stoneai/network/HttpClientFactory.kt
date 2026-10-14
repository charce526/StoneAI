package cn.xzbim.stoneai.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 安全的 HTTP 客户端工厂，配置超时与防护策略
 */
object HttpClientFactory {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
