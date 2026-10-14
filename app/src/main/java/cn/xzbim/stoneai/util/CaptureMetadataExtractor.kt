package cn.xzbim.stoneai.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import cn.xzbim.stoneai.model.CaptureMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 拍摄元数据抽取器：负责从相册 EXIF 或 CameraX 拍摄环境中安全抽取时间与地点信息
 */
object CaptureMetadataExtractor {

    private const val TAG = "CaptureMetadata"

    /**
     * 相册图片：优先读取照片原生 EXIF 中的拍摄时间与 GPS 坐标，绝对不伪造或使用当前位置！
     */
    suspend fun extractFromExif(context: Context, file: File): CaptureMetadata = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) {
            return@withContext CaptureMetadata()
        }

        try {
            extractFromExif(context, ExifInterface(file.absolutePath))
        } catch (e: Exception) {
            Log.w(TAG, "解析相册图片 EXIF 元数据异常: ${e.message}")
            CaptureMetadata()
        }
    }

    /**
     * 直接从系统返回的原始媒体 Uri 读取 EXIF。
     * Android 10+ 会默认抹除共享媒体中的 GPS；获得 ACCESS_MEDIA_LOCATION 后，
     * 必须通过 setRequireOriginal() 请求原始内容，才能读到照片自身的坐标。
     */
    suspend fun extractFromExif(context: Context, uri: Uri): CaptureMetadata = withContext(Dispatchers.IO) {
        val candidates = buildList {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    add(MediaStore.setRequireOriginal(uri))
                } catch (_: Exception) {
                }
            }
            add(uri)
        }.distinct()

        var bestMetadata = CaptureMetadata()
        for (candidate in candidates) {
            val metadata = try {
                // FileDescriptor 对 JPEG/HEIF 及需要 seek 的 EXIF 结构更可靠。
                context.contentResolver.openFileDescriptor(candidate, "r")?.use { descriptor ->
                    extractFromExif(context, ExifInterface(descriptor.fileDescriptor))
                } ?: CaptureMetadata()
            } catch (descriptorError: Exception) {
                try {
                    context.contentResolver.openInputStream(candidate)?.use { input ->
                        extractFromExif(context, ExifInterface(input))
                    } ?: CaptureMetadata()
                } catch (streamError: Exception) {
                    Log.w(
                        TAG,
                        "从媒体 Uri 解析 EXIF 元数据异常: ${streamError.message ?: descriptorError.message}"
                    )
                    CaptureMetadata()
                }
            }

            if (metadata.hasValidGps()) return@withContext metadata
            if (!bestMetadata.hasData() && metadata.hasData()) bestMetadata = metadata
        }
        bestMetadata
    }

    private fun extractFromExif(context: Context, exif: ExifInterface): CaptureMetadata {
        val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        val capturedAt = dateTimeStr?.let { parseExifDate(it) }

        // 使用 DoubleArray API，避免旧 FloatArray API 带来的坐标精度损失。
        var latitude: Double? = exif.latLong?.getOrNull(0)
        var longitude: Double? = exif.latLong?.getOrNull(1)

        if (latitude == null || longitude == null) {
            val latStr = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
            val lngStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
            latitude = latStr?.let { convertDmsToDecimal(it, latRef) }
            longitude = lngStr?.let { convertDmsToDecimal(it, lngRef) }
        }

        val validGps = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(kotlin.math.abs(latitude) < 0.0001 && kotlin.math.abs(longitude) < 0.0001)

        if (!validGps) {
            latitude = null
            longitude = null
        }

        val locationName = if (latitude != null && longitude != null) {
            reverseGeocode(context, latitude, longitude)
        } else {
            null
        }

        Log.d(TAG, "相册 EXIF 抽取完成 [HasTime: ${capturedAt != null}, HasGps: ${latitude != null && longitude != null}]")
        return CaptureMetadata(capturedAt, latitude, longitude, locationName)
    }

    /**
     * APP 拍摄照片：记录当下实际拍照时间，并在已授权定位时抓取当前单次有效位置
     */
    @SuppressLint("MissingPermission")
    suspend fun extractFromCamera(context: Context): CaptureMetadata = withContext(Dispatchers.IO) {
        val capturedAt = System.currentTimeMillis()

        // 检查定位权限
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.d(TAG, "拍照抓取元数据：未授权定位权限，仅记录拍摄时间")
            return@withContext CaptureMetadata(capturedAt = capturedAt)
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLocation: Location? = null

            if (locationManager != null) {
                val enabledProviders = locationManager.getProviders(true)
                Log.d(TAG, "当前启用的定位 Provider: $enabledProviders")

                for (provider in enabledProviders) {
                    try {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.time > bestLocation.time) {
                                bestLocation = loc
                            }
                        }
                    } catch (_: SecurityException) {
                    }
                }

                // 兜底：若缓存位置为空，使用网络或 GPS 极速请求位置
                if (bestLocation == null && enabledProviders.isNotEmpty()) {
                    val targetProvider = if (enabledProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                        LocationManager.NETWORK_PROVIDER
                    } else {
                        enabledProviders.first()
                    }

                    val latch = CountDownLatch(1)
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            bestLocation = location
                            latch.countDown()
                        }
                        override fun onProviderDisabled(p: String) {}
                        override fun onProviderEnabled(p: String) {}
                        @Suppress("DEPRECATION")
                        override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) {}
                    }

                    try {
                        locationManager.requestLocationUpdates(targetProvider, 0L, 0f, listener, Looper.getMainLooper())
                        latch.await(3, TimeUnit.SECONDS)
                        locationManager.removeUpdates(listener)
                    } catch (_: Exception) {
                    }
                }
            }

            var latitude: Double? = null
            var longitude: Double? = null
            var locationName: String? = null

            val loc = bestLocation
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                locationName = reverseGeocode(context, loc.latitude, loc.longitude)
            }

            Log.d(TAG, "相机拍照位置抓取完成 [HasGps: ${latitude != null && longitude != null}]")

            CaptureMetadata(
                capturedAt = capturedAt,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName
            )
        } catch (e: Exception) {
            Log.w(TAG, "拍照抓取当前位置元数据异常: ${e.message}")
            CaptureMetadata(capturedAt = capturedAt)
        }
    }

    /**
     * 将经纬度通过 Android Geocoder 反向解析为可读省市区地点名称
     */
    fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val admin = addr.adminArea ?: ""
                val locality = addr.locality ?: addr.subAdminArea ?: ""
                val subLocality = addr.subLocality ?: ""
                val feature = addr.featureName ?: addr.thoroughfare ?: ""

                val name = buildString {
                    if (admin.isNotBlank()) append(admin)
                    if (locality.isNotBlank() && locality != admin) append(locality)
                    if (subLocality.isNotBlank()) append(subLocality)
                }.ifBlank {
                    if (feature.isNotBlank()) feature else null
                }
                name
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "反向地理编码解析位置名称异常: ${e.message}")
            null
        }
    }

    private fun parseExifDate(dateStr: String): Long? {
        val formats = listOf(
            "yyyy:MM:dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy:MM:dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun convertDmsToDecimal(dms: String, ref: String?): Double? {
        return try {
            val parts = dms.split(",")
            if (parts.size < 3) return null
            val d = parseRational(parts[0])
            val m = parseRational(parts[1])
            val s = parseRational(parts[2])
            var dec = d + (m / 60.0) + (s / 3600.0)
            if (ref != null && (ref.equals("S", ignoreCase = true) || ref.equals("W", ignoreCase = true))) {
                dec = -dec
            }
            dec
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRational(rationalStr: String): Double {
        val parts = rationalStr.trim().split("/")
        return if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull() ?: 0.0
            val den = parts[1].toDoubleOrNull() ?: 1.0
            if (den != 0.0) num / den else 0.0
        } else {
            rationalStr.trim().toDoubleOrNull() ?: 0.0
        }
    }
}
