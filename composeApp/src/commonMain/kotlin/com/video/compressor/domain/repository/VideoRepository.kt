package com.video.compressor.domain.repository

import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.model.TranscodeTask
import kotlinx.coroutines.flow.Flow

/**
 * 视频数据仓库接口
 */
interface VideoRepository {
    
    /**
     * 获取视频文件信息
     */
    suspend fun getVideoInfo(filePath: String): Result<VideoFile>
    
    /**
     * 保存任务历史
     */
    suspend fun saveTask(task: TranscodeTask): Result<Unit>
    
    /**
     * 获取任务历史
     */
    fun getTaskHistory(): Flow<List<TranscodeTask>>
    
    /**
     * 获取正在进行的任务
     */
    fun getActiveTasks(): Flow<List<TranscodeTask>>
    
    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, status: com.video.compressor.domain.model.TaskStatus): Result<Unit>
    
    /**
     * 更新任务进度
     */
    suspend fun updateTaskProgress(taskId: String, progress: Float, speed: String, estimatedTime: Int): Result<Unit>
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String): Result<Unit>
    
    /**
     * 清空任务历史
     */
    suspend fun clearTaskHistory(): Result<Unit>
}
