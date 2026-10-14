package cn.xzbim.stoneai.share

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 分享卡片高清图片生成器 (固定 1080px 宽、按内容自适应高度的高清 PNG)
 */
object ShareCardGenerator {

    /**
     * 预加载图片并渲染 1080px 宽、按内容自适应高度的分享海报图片文件
     */
    suspend fun generateCardFile(
        context: Context,
        shareData: RockShareData
    ): File = withContext(Dispatchers.IO) {
        // 1. 预加载图片 Bitmap (避免离屏渲染异步解图导致图片空白)
        val preloadedBitmap = preloadPhotoBitmap(context, shareData.imageModel)

        // 2. 离屏绘制 Compose 视图为 1080px 宽、自适应高度 Bitmap
        val bitmap = withContext(Dispatchers.Main) {
            renderViewToBitmap(context, shareData, preloadedBitmap)
        }

        // 3. 将 Bitmap 保存为临时 PNG 文件
        val shareDir = File(context.cacheDir, "share_cards").apply { if (!exists()) mkdirs() }
        val outputFile = File(shareDir, "share_rock_${UUID.randomUUID()}.png")

        FileOutputStream(outputFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        // 回收临时的 raw bitmap 避免 OOM
        bitmap.recycle()
        preloadedBitmap?.recycle()

        outputFile
    }

    private suspend fun preloadPhotoBitmap(context: Context, imageModel: Any?): Bitmap? = withContext(Dispatchers.IO) {
        if (imageModel == null) return@withContext null
        try {
            val request = ImageRequest.Builder(context)
                .data(imageModel)
                .size(522, 522)
                .allowHardware(false) // 关闭 Hardware Bitmap，确保 Canvas 可直接读取绘制
                .build()

            val result = context.imageLoader.execute(request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                // 返回独立副本，避免生成结束时 recycle Coil 内存缓存持有的共享 Bitmap。
                drawable.bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun renderViewToBitmap(
        context: Context,
        shareData: RockShareData,
        preloadedBitmap: Bitmap?
    ): Bitmap {
        val widthPx = 1080
        val activity = context.findActivity() ?: throw IllegalStateException("无法获取当前 Activity 窗口")
        val decorView = activity.window.decorView as ViewGroup

        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            setContent {
                // 固定 3x 密度：360dp 精确对应 1080px，避免设备 dpi 影响布局。
                CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = 1f)) {
                    MaterialTheme {
                        RockShareCard(
                            shareData = shareData,
                            preloadedBitmap = preloadedBitmap
                        )
                    }
                }
            }
        }

        // 临时 attach 到 Activity decorView 继承 WindowRecomposer、Lifecycle 与 CompositionContext
        decorView.addView(composeView)

        try {
            val widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

            composeView.measure(widthSpec, heightSpec)
            val heightPx = composeView.measuredHeight.coerceAtLeast(1)
            composeView.layout(0, 0, widthPx, heightPx)

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)

            return bitmap
        } finally {
            decorView.removeView(composeView)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
