package com.video.compressor.domain.repository

import com.video.compressor.domain.model.VideoParameters
import com.video.compressor.domain.model.AudioParameters
import kotlinx.coroutines.flow.Flow

/**
 * 设置数据仓库接口
 */
interface SettingsRepository {
    
    /**
     * 获取默认视频参数
     */
    fun getDefaultVideoParameters(): Flow<VideoParameters>
    
    /**
     * 保存默认视频参数
     */
    suspend fun saveDefaultVideoParameters(parameters: VideoParameters): Result<Unit>
    
    /**
     * 获取默认音频参数
     */
    fun getDefaultAudioParameters(): Flow<AudioParameters>
    
    /**
     * 保存默认音频参数
     */
    suspend fun saveDefaultAudioParameters(parameters: AudioParameters): Result<Unit>
    
    /**
     * 获取默认输出目录
     */
    fun getDefaultOutputDirectory(): Flow<String?>
    
    /**
     * 保存默认输出目录
     */
    suspend fun saveDefaultOutputDirectory(directory: String): Result<Unit>
    
    /**
     * 获取硬件加速设置
     */
    fun getHardwareAccelerationEnabled(): Flow<Boolean>
    
    /**
     * 保存硬件加速设置
     */
    suspend fun saveHardwareAccelerationEnabled(enabled: Boolean): Result<Unit>
}
