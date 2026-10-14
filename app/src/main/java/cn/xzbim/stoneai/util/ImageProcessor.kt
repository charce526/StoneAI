package cn.xzbim.stoneai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import cn.xzbim.stoneai.data.local.AppPreferences
import cn.xzbim.stoneai.model.ProcessedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 独立的图片处理类：负责图片安全持久化、EXIF 旋转修正、长边 1600px 比例缩放、JPEG 压缩与 Base64 编码
 */
object ImageProcessor {

    private const val TAG = "ImageProcessor"

    /**
     * 将外部 Uri (Photo Picker / CameraX / ContentProvider) 安全复制到 App 私有 cache 目录
     * 解决 Photo Picker 临时 Uri 权限失效与 ContentResolver 频繁 openInputStream 失败的问题
     */
    fun copyUriToPrivateFile(context: Context, sourceUri: Uri): File {
        val inputDir = File(context.cacheDir, "recognition_input").apply { if (!exists()) mkdirs() }
        val targetFile = File(inputDir, "input_${System.currentTimeMillis()}.jpg")

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.e(TAG, "无法打开所选图片输入流")
                throw IllegalArgumentException("无法打开源图片输入流: $sourceUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "复制图片文件到私有目录异常", e)
            throw IllegalArgumentException("读取图片失败: ${e.localizedMessage ?: "源图片文件无法读取"}")
        }

        if (!targetFile.exists() || targetFile.length() <= 0) {
            throw IllegalArgumentException("写入私有缓存图片文件失败或文件大小为 0")
        }

        Log.d(TAG, "源图片已成功转存至私有缓存 [Size: ${targetFile.length()} bytes]")
        return targetFile
    }

    /**
     * 计算目标尺寸，保持宽高比，最长边默认限制在 maxEdge (例如 1600px) 内，原图小于 maxEdge 则不放大
     */
    fun calculateTargetDimensions(origWidth: Int, origHeight: Int, maxEdge: Int = 1600): Pair<Int, Int> {
        if (origWidth <= 0 || origHeight <= 0) return 1600 to 1200
        val maxDimension = max(origWidth, origHeight)
        if (maxDimension <= maxEdge) {
            return origWidth to origHeight
        }

        val scale = maxEdge.toDouble() / maxDimension.toDouble()
        val targetWidth = (origWidth * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (origHeight * scale).roundToInt().coerceAtLeast(1)
        return targetWidth to targetHeight
    }

    /**
     * 计算 BitmapFactory 的 inSampleSize 采样率
     */
    fun calculateInSampleSize(origWidth: Int, origHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (origHeight > reqHeight || origWidth > reqWidth) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 处理 Uri 指定的图片（读取 AppPreferences 中的动态图片质量配置）
     */
    suspend fun processWithPreferences(
        context: Context,
        uri: Uri
    ): ProcessedImage {
        val appPrefs = AppPreferences.getInstance(context)
        val qualitySetting = appPrefs.getImageQuality()
        return process(
            context = context,
            uri = uri,
            maxEdge = qualitySetting.maxEdge,
            quality = qualitySetting.quality
        )
    }

    /**
     * 处理 Uri 指定的图片（内部自动转存为稳定 App 私有本地文件后再进行处理）
     */
    suspend fun process(
        context: Context,
        uri: Uri,
        maxEdge: Int = 1600,
        quality: Int = 80
    ): ProcessedImage = withContext(Dispatchers.IO) {
        val file = if (uri.scheme == "file") {
            File(uri.path ?: throw IllegalArgumentException("非法 File Uri 路径"))
        } else {
            copyUriToPrivateFile(context, uri)
        }
        processFile(file, maxEdge, quality)
    }

    /**
     * 处理本地 File 图片
     */
    suspend fun processFile(
        file: File,
        maxEdge: Int = 1600,
        quality: Int = 80
    ): ProcessedImage = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) {
            Log.e(TAG, "目标图片文件不存在或为空")
            throw IllegalArgumentException("本地图片文件不存在或已被删除")
        }

        // 1. 读取 EXIF 旋转角度
        val rotationDegrees = getExifRotation(file)

        // 2. 测量原图尺寸
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var origW = options.outWidth
        var origH = options.outHeight

        if (origW <= 0 || origH <= 0) {
            Log.e(TAG, "BitmapFactory.decodeFile 测量尺寸失败")
            throw IllegalArgumentException("图片尺寸测量失败，可能是不支持的图片格式")
        }

        // 如果 EXIF 旋转了 90 或 270 度，原图宽高对调计算
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            val temp = origW
            origW = origH
            origH = temp
        }

        val (targetW, targetH) = calculateTargetDimensions(origW, origH, maxEdge)
        val sampleSize = calculateInSampleSize(origW, origH, targetW, targetH)

        // 3. 采样解码 Bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalArgumentException("解码本地图片失败")

        // 4. 应用 EXIF 旋转
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val transformed = Bitmap.createBitmap(
                decodedBitmap,
                0, 0,
                decodedBitmap.width, decodedBitmap.height,
                matrix, true
            )
            if (transformed != decodedBitmap) {
                decodedBitmap.recycle()
            }
            transformed
        } else {
            decodedBitmap
        }

        // 5. 精确二次缩放到目标尺寸
        val scaledBitmap = if (rotatedBitmap.width != targetW || rotatedBitmap.height != targetH) {
            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, targetW, targetH, true)
            if (scaled != rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            scaled
        } else {
            rotatedBitmap
        }

        // 6. 处理透明背景 (如果是透明 PNG/WEBP，绘制到白色背景上)
        val finalBitmap = if (scaledBitmap.hasAlpha()) {
            val whiteBgBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(whiteBgBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            scaledBitmap.recycle()
            whiteBgBitmap
        } else {
            scaledBitmap
        }

        // 7. JPEG 压缩与 Base64 编码
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        finalBitmap.recycle()

        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        Log.d(TAG, "图片处理成功 [原尺寸: ${origW}x${origH}, 目标尺寸: ${targetW}x${targetH}, 压缩大小: ${bytes.size} bytes]")

        ProcessedImage(
            originalWidth = origW,
            originalHeight = origH,
            width = targetW,
            height = targetH,
            mimeType = "image/jpeg",
            bytes = bytes,
            base64 = base64,
            sizeBytes = bytes.size.toLong()
        )
    }

    private fun getExifRotation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取 EXIF 旋转信息失败: ${e.message}")
            0
        }
    }
}
