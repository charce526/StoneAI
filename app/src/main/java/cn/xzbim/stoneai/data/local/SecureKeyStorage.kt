package cn.xzbim.stoneai.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 Android Keystore AES/GCM 的 API Key 安全加密存储类
 */
class SecureKeyStorage(context: Context) {

    private val prefs = context.getSharedPreferences("stone_ai_secure_keys", Context.MODE_PRIVATE)
    private val keyStoreAlias = "StoneAISecureKeyAlias"
    private val provider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        if (keyStore.containsAlias(keyStoreAlias)) {
            val entry = keyStore.getEntry(keyStoreAlias, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
        val spec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 加密保存指定 configId 的 API Key
     */
    fun saveApiKey(configId: String, apiKey: String) {
        if (configId.isBlank() || apiKey.isBlank()) return

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(4 + iv.size + encryptedBytes.size)
            combined[0] = (iv.size shr 24).toByte()
            combined[1] = (iv.size shr 16).toByte()
            combined[2] = (iv.size shr 8).toByte()
            combined[3] = iv.size.toByte()

            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 4 + iv.size, encryptedBytes.size)

            val base64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString("key_$configId", base64).apply()
        } catch (_: Exception) {
            // 安全存储异常捕获
        }
    }

    /**
     * 解密获取指定 configId 的 API Key
     */
    fun getApiKey(configId: String): String? {
        if (configId.isBlank()) return null
        val base64 = prefs.getString("key_$configId", null) ?: return null

        return try {
            val combined = Base64.decode(base64, Base64.NO_WRAP)
            if (combined.size < 4) return null

            val ivSize = ((combined[0].toInt() and 0xFF) shl 24) or
                    ((combined[1].toInt() and 0xFF) shl 16) or
                    ((combined[2].toInt() and 0xFF) shl 8) or
                    (combined[3].toInt() and 0xFF)

            if (combined.size < 4 + ivSize) return null

            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 4, iv, 0, ivSize)

            val encryptedBytesSize = combined.size - 4 - ivSize
            val encryptedBytes = ByteArray(encryptedBytesSize)
            System.arraycopy(combined, 4 + ivSize, encryptedBytes, 0, encryptedBytesSize)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 检查是否存在已保存的 API Key
     */
    fun hasApiKey(configId: String): Boolean {
        if (configId.isBlank()) return false
        return prefs.contains("key_$configId")
    }

    /**
     * 删除指定 configId 的 API Key
     */
    fun deleteApiKey(configId: String) {
        if (configId.isBlank()) return
        prefs.edit().remove("key_$configId").apply()
    }
}
