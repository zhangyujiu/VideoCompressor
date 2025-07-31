package com.video.compressor.data.local.database

import kotlinx.serialization.Serializable

/**
 * 任务数据库实体
 * 注意：这里使用简单的内存存储，实际项目中可以使用SQLDelight或Room
 */
@Serializable
data class TaskEntity(
    val id: String,
    val inputFilePath: String,
    val inputFileName: String,
    val inputFileSize: Long,
    val inputDuration: Int,
    val inputWidth: Int,
    val inputHeight: Int,
    val inputFrameRate: Double,
    val inputBitRate: Long,
    val inputFormat: String,
    val inputCodec: String,
    val outputPath: String,
    val outputFormat: String,
    val outputWidth: Int?,
    val outputHeight: Int?,
    val outputFrameRate: Int?,
    val outputBitRate: Long?,
    val compressionLevel: String,
    val encoder: String,
    val targetFileSize: Long?,
    val enableHardwareAcceleration: Boolean,
    val customParameters: String, // JSON字符串
    val audioCodec: String,
    val audioBitRate: Int,
    val audioSampleRate: Int,
    val audioChannels: Int,
    val status: String,
    val progress: Float,
    val speed: String,
    val estimatedTimeRemaining: Int,
    val errorMessage: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?
)
