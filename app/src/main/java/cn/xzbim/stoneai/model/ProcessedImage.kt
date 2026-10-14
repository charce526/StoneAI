package cn.xzbim.stoneai.model

/**
 * 适合 AI 请求使用的图片缩放、压缩与 Base64 编码处理结果
 */
data class ProcessedImage(
    val originalWidth: Int,
    val originalHeight: Int,
    val width: Int,
    val height: Int,
    val mimeType: String = "image/jpeg",
    val bytes: ByteArray,
    val base64: String,
    val sizeBytes: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedImage

        if (originalWidth != other.originalWidth) return false
        if (originalHeight != other.originalHeight) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (base64 != other.base64) return false
        if (sizeBytes != other.sizeBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originalWidth
        result = 31 * result + originalHeight
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + base64.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        return result
    }
}
