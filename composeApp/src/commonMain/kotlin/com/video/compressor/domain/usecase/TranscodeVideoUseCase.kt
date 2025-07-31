package com.video.compressor.domain.usecase

import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 视频转码用例
 */
class TranscodeVideoUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(task: TranscodeTask): Flow<TranscodeTask> = flow {
        try {
            // 保存任务到数据库
            videoRepository.saveTask(task)
            
            // 更新任务状态为运行中
            val runningTask = task.copy(
                status = TaskStatus.RUNNING,
                startedAt = System.currentTimeMillis()
            )
            videoRepository.updateTaskStatus(task.id, TaskStatus.RUNNING)
            emit(runningTask)
            
            // TODO: 实际的转码逻辑将在FFmpeg集成模块中实现
            // 这里先模拟转码过程
            for (progress in 1..100) {
                kotlinx.coroutines.delay(100) // 模拟转码时间
                
                val updatedTask = runningTask.copy(
                    progress = progress / 100f,
                    speed = "${(1..10).random()}x",
                    estimatedTimeRemaining = (100 - progress) * 2
                )
                
                videoRepository.updateTaskProgress(
                    task.id,
                    updatedTask.progress,
                    updatedTask.speed,
                    updatedTask.estimatedTimeRemaining
                )
                
                emit(updatedTask)
            }
            
            // 完成任务
            val completedTask = runningTask.copy(
                status = TaskStatus.COMPLETED,
                progress = 1f,
                completedAt = System.currentTimeMillis()
            )
            videoRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
            emit(completedTask)
            
        } catch (e: Exception) {
            // 任务失败
            val failedTask = task.copy(
                status = TaskStatus.FAILED,
                errorMessage = e.message
            )
            videoRepository.updateTaskStatus(task.id, TaskStatus.FAILED)
            emit(failedTask)
        }
    }
}
