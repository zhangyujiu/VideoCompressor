package com.video.compressor.domain.usecase

import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

/**
 * 任务管理用例
 */
class ManageTasksUseCase(
    private val videoRepository: VideoRepository
) {
    
    /**
     * 获取任务历史
     */
    fun getTaskHistory(): Flow<List<TranscodeTask>> {
        return videoRepository.getTaskHistory()
    }
    
    /**
     * 获取活跃任务
     */
    fun getActiveTasks(): Flow<List<TranscodeTask>> {
        return videoRepository.getActiveTasks()
    }
    
    /**
     * 暂停任务
     */
    suspend fun pauseTask(taskId: String): Result<Unit> {
        return videoRepository.updateTaskStatus(taskId, TaskStatus.PAUSED)
    }
    
    /**
     * 恢复任务
     */
    suspend fun resumeTask(taskId: String): Result<Unit> {
        return videoRepository.updateTaskStatus(taskId, TaskStatus.RUNNING)
    }
    
    /**
     * 取消任务
     */
    suspend fun cancelTask(taskId: String): Result<Unit> {
        return videoRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
    }
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return videoRepository.deleteTask(taskId)
    }
    
    /**
     * 清空任务历史
     */
    suspend fun clearTaskHistory(): Result<Unit> {
        return videoRepository.clearTaskHistory()
    }
}
