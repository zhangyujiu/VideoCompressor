package com.video.compressor.core.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 通用扩展函数
 */

/**
 * 格式化文件大小
 */
fun Long.formatFileSize(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.2f %s".format(size, units[unitIndex])
}

/**
 * 格式化时间戳
 */
fun Long.formatTimestamp(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.date} ${localDateTime.time}"
}

/**
 * 格式化持续时间（秒）
 */
fun Int.formatDuration(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    
    return when {
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * 检查文件扩展名是否为支持的视频格式
 */
fun String.isSupportedVideoFormat(): Boolean {
    val extension = this.substringAfterLast('.', "").lowercase()
    return Constants.VideoFormats.INPUT_FORMATS.contains(extension)
}

/**
 * 获取文件扩展名
 */
fun String.getFileExtension(): String {
    return this.substringAfterLast('.', "")
}

/**
 * 获取不带扩展名的文件名
 */
fun String.getFileNameWithoutExtension(): String {
    return this.substringBeforeLast('.')
}
