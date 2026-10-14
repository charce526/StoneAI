package cn.xzbim.stoneai.model

import android.net.Uri

/**
 * 统一的选中图片信息
 */
data class SelectedImage(
    val uri: Uri,
    val source: ImageSource
)
