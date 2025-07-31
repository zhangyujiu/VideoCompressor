package com.video.compressor.domain.model

import kotlinx.serialization.Serializable

/**
 * 视频文件信息模型
 */
@Serializable
data class VideoFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val duration: Int, // 秒
    val width: Int,
    val height: Int,
    val frameRate: Double,
    val bitRate: Long,
    val format: String,
    val codec: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val resolution: String
        get() = "${width}x${height}"
    
    val aspectRatio: Double
        get() = width.toDouble() / height.toDouble()
}

/**
 * 视频参数配置
 */
@Serializable
data class VideoParameters(
    val outputFormat: String,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Int? = null,
    val bitRate: Long? = null,
    val compressionLevel: String,
    val encoder: String,
    val targetFileSize: Long? = null,
    val enableHardwareAcceleration: Boolean = false,
    val customParameters: Map<String, String> = emptyMap()
) {
    companion object {
        fun default() = VideoParameters(
            outputFormat = "mp4",
            compressionLevel = "medium",
            encoder = "libx264"
        )
    }
}

/**
 * 音频参数配置
 */
@Serializable
data class AudioParameters(
    val codec: String = "aac",
    val bitRate: Int = 128, // kbps
    val sampleRate: Int = 44100, // Hz
    val channels: Int = 2
)

/**
 * 转码任务状态
 */
enum class TaskStatus {
    PENDING,    // 等待中
    RUNNING,    // 进行中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    FAILED,     // 失败
    CANCELLED   // 已取消
}

/**
 * 转码任务模型
 */
@Serializable
data class TranscodeTask(
    val id: String,
    val inputFile: VideoFile,
    val outputPath: String,
    val videoParameters: VideoParameters,
    val audioParameters: AudioParameters,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Float = 0f,
    val speed: String = "",
    val estimatedTimeRemaining: Int = 0, // 秒
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null
) {
    val outputFileName: String
        get() {
            val baseName = inputFile.name.substringBeforeLast('.')
            return "$baseName.${videoParameters.outputFormat}"
        }
}
