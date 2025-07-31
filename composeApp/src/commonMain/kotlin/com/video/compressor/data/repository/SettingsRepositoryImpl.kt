package com.video.compressor.data.repository

import com.video.compressor.domain.model.VideoParameters
import com.video.compressor.domain.model.AudioParameters
import com.video.compressor.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 设置仓库实现
 */
class SettingsRepositoryImpl : SettingsRepository {
    
    private val _defaultVideoParameters = MutableStateFlow(VideoParameters.default())
    private val _defaultAudioParameters = MutableStateFlow(AudioParameters())
    private val _defaultOutputDirectory = MutableStateFlow<String?>(null)
    private val _hardwareAccelerationEnabled = MutableStateFlow(false)
    
    override fun getDefaultVideoParameters(): Flow<VideoParameters> {
        return _defaultVideoParameters
    }
    
    override suspend fun saveDefaultVideoParameters(parameters: VideoParameters): Result<Unit> {
        return try {
            _defaultVideoParameters.value = parameters
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getDefaultAudioParameters(): Flow<AudioParameters> {
        return _defaultAudioParameters
    }
    
    override suspend fun saveDefaultAudioParameters(parameters: AudioParameters): Result<Unit> {
        return try {
            _defaultAudioParameters.value = parameters
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getDefaultOutputDirectory(): Flow<String?> {
        return _defaultOutputDirectory
    }
    
    override suspend fun saveDefaultOutputDirectory(directory: String): Result<Unit> {
        return try {
            _defaultOutputDirectory.value = directory
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getHardwareAccelerationEnabled(): Flow<Boolean> {
        return _hardwareAccelerationEnabled
    }
    
    override suspend fun saveHardwareAccelerationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            _hardwareAccelerationEnabled.value = enabled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
