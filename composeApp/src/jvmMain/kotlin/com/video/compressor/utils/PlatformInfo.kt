package com.video.compressor.utils

import java.io.File
import java.text.DecimalFormat

/**
 * JVM平台的系统信息实现
 */
actual object PlatformInfo {
    actual fun getPlatformName(): String {
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osVersion = System.getProperty("os.version") ?: "Unknown"
        val osArch = System.getProperty("os.arch") ?: "Unknown"
        return "$osName $osVersion ($osArch)"
    }
    
    actual fun getOSVersion(): String {
        val javaVersion = System.getProperty("java.version") ?: "Unknown"
        val javaVendor = System.getProperty("java.vendor") ?: "Unknown"
        return "Java $javaVersion ($javaVendor)"
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
            val userHome = File(System.getProperty("user.home") ?: ".")
            val totalSpace = userHome.totalSpace
            val freeSpace = userHome.freeSpace
            val usedSpace = totalSpace - freeSpace
            
            "${formatBytes(usedSpace)} / ${formatBytes(totalSpace)}"
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
