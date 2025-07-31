package com.video.compressor.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 */
object PermissionManager {
    
    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要WRITE_EXTERNAL_STORAGE权限来写入Download目录
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用分区存储，写入Download目录不需要权限
            true
        } else {
            // Android 9 及以下需要WRITE_EXTERNAL_STORAGE权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查是否有读取媒体文件权限
     */
    fun hasMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要READ_MEDIA_VIDEO权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 及以下使用READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取需要请求的权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            // Android 9 及以下
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * 检查是否有所有必需的权限
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasStoragePermission(context) && hasMediaPermission(context)
    }
    
    /**
     * 获取权限说明文本
     */
    fun getPermissionExplanation(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "需要访问媒体文件权限来选择和处理视频文件。转码后的文件将保存到Download目录。"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "需要存储权限来访问视频文件。转码后的文件将保存到Download目录。"
        } else {
            "需要存储权限来访问和保存视频文件。转码后的文件将保存到Download目录。"
        }
    }
}
