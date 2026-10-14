package cn.xzbim.stoneai.share

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 分享管理器：负责驱动海报卡片生成与调用 Android 系统原生分享面板
 */
object ShareManager {

    private const val TAG = "ShareManager"

    /**
     * 生成高清海报卡片并拉起系统分享面板
     */
    suspend fun shareResult(
        context: Context,
        shareData: RockShareData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始生成分享卡片")

            // 1. 生成 1080x1440 高清 PNG 文件
            val file = ShareCardGenerator.generateCardFile(context, shareData)

            shareCardFile(context, shareData, file).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "生成或分享卡片过程异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** 分享已生成并在预览页展示过的同一个 PNG 文件。 */
    suspend fun shareCardFile(
        context: Context,
        shareData: RockShareData,
        file: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() <= 0) {
                return@withContext Result.failure(IllegalStateException("分享卡片文件无效"))
            }

            val authority = "${context.packageName}.fileprovider"
            val shareUri = FileProvider.getUriForFile(context, authority, file)

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_TEXT, "我用「AI 识石」发现这可能是${shareData.name} ✨")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "分享识别结果")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }

            Log.d(TAG, "成功调起系统分享面板")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "分享卡片文件异常: ${e.message}", e)
            Result.failure(e)
        }
    }
}
