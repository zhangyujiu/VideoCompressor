package com.video.compressor.data.repository

import com.video.compressor.data.local.database.InMemoryDatabase
import com.video.compressor.data.mapper.TaskMapper
import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/**
 * 视频仓库实现
 */
class VideoRepositoryImpl(
    private val database: InMemoryDatabase
) : VideoRepository {
    
    override suspend fun getVideoInfo(filePath: String): Result<VideoFile> {
        return try {
            // 模拟获取视频信息
            val videoFile = VideoFile(
                id = Random.nextLong().toString(),
                name = filePath.substringAfterLast('/').substringAfterLast('\\'),
                path = filePath,
                size = 125600000L, // 125.6 MB
                duration = 330, // 5分30秒
                width = 1920,
                height = 1080,
                frameRate = 30.0,
                bitRate = 5000000L,
                format = "mp4",
                codec = "h264"
            )
            Result.success(videoFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveTask(task: TranscodeTask): Result<Unit> {
        return try {
            val entity = TaskMapper.toEntity(task)
            database.insertTask(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getTaskHistory(): Flow<List<TranscodeTask>> {
        return database.getTaskHistory().map { entities ->
            entities.map { TaskMapper.toDomain(it) }
        }
    }
    
    override fun getActiveTasks(): Flow<List<TranscodeTask>> {
        return database.getActiveTasks().map { entities ->
            entities.map { TaskMapper.toDomain(it) }
        }
    }
    
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            database.updateTask(taskId) { entity ->
                entity.copy(status = status.name)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTaskProgress(
        taskId: String,
        progress: Float,
        speed: String,
        estimatedTime: Int
    ): Result<Unit> {
        return try {
            database.updateTask(taskId) { entity ->
                entity.copy(
                    progress = progress,
                    speed = speed,
                    estimatedTimeRemaining = estimatedTime
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            database.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearTaskHistory(): Result<Unit> {
        return try {
            database.clearHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
