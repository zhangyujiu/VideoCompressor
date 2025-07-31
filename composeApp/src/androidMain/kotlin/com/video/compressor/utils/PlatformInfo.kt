package com.video.compressor.utils

import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.text.DecimalFormat

/**
 * Android平台的系统信息实现
 */
actual object PlatformInfo {
    actual fun getPlatformName(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
    
    actual fun getOSVersion(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    actual fun getAvailableMemory(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            "${formatBytes(usedMemory)} / ${formatBytes(maxMemory)}"
        } catch (e: Exception) {
            "无法获取内存信息"
        }
    }
    
    actual fun getAvailableStorage(): String {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = stat.availableBytes
            val totalBytes = stat.totalBytes
            val usedBytes = totalBytes - availableBytes
            
            "${formatBytes(usedBytes)} / ${formatBytes(totalBytes)}"
        } catch (e: Exception) {
            "无法获取存储信息"
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }
}
