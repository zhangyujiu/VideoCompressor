package com.video.compressor.domain.service

import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * FFmpeg服务接口
 */
interface FFmpegService {
    
    /**
     * 检查FFmpeg是否可用
     */
    suspend fun isFFmpegAvailable(): Boolean
    
    /**
     * 获取FFmpeg版本信息
     */
    suspend fun getFFmpegVersion(): String?
    
    /**
     * 下载并安装FFmpeg
     */
    suspend fun downloadFFmpeg(onProgress: (Int) -> Unit): Result<Unit>
    
    /**
     * 开始转码任务
     */
    suspend fun startTranscode(task: TranscodeTask): Flow<TranscodeProgress>
    
    /**
     * 停止转码任务
     */
    suspend fun stopTranscode(taskId: String): Result<Unit>

    /**
     * 获取转码进度
     */
    suspend fun getProgress(taskId: String): TranscodeProgress?

    /**
     * 获取支持的编码器列表
     */
    suspend fun getSupportedEncoders(): List<String>
    
    /**
     * 获取支持的格式列表
     */
    suspend fun getSupportedFormats(): List<String>
}

/**
 * 转码进度信息
 */
data class TranscodeProgress(
    val taskId: String,
    val status: TaskStatus,
    val progress: Float, // 0.0 - 1.0
    val speed: String = "0x",
    val fps: Int = 0,
    val bitrate: String = "",
    val size: String = "",
    val time: String = "",
    val estimatedTimeRemaining: Int = 0, // 秒
    val errorMessage: String? = null
)
