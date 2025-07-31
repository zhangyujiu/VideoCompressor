package com.video.compressor.data.mapper

import com.video.compressor.data.local.database.TaskEntity
import com.video.compressor.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 任务数据映射器
 */
object TaskMapper {
    
    fun toEntity(task: TranscodeTask): TaskEntity {
        return TaskEntity(
            id = task.id,
            inputFilePath = task.inputFile.path,
            inputFileName = task.inputFile.name,
            inputFileSize = task.inputFile.size,
            inputDuration = task.inputFile.duration,
            inputWidth = task.inputFile.width,
            inputHeight = task.inputFile.height,
            inputFrameRate = task.inputFile.frameRate,
            inputBitRate = task.inputFile.bitRate,
            inputFormat = task.inputFile.format,
            inputCodec = task.inputFile.codec,
            outputPath = task.outputPath,
            outputFormat = task.videoParameters.outputFormat,
            outputWidth = task.videoParameters.width,
            outputHeight = task.videoParameters.height,
            outputFrameRate = task.videoParameters.frameRate,
            outputBitRate = task.videoParameters.bitRate,
            compressionLevel = task.videoParameters.compressionLevel,
            encoder = task.videoParameters.encoder,
            targetFileSize = task.videoParameters.targetFileSize,
            enableHardwareAcceleration = task.videoParameters.enableHardwareAcceleration,
            customParameters = Json.encodeToString(task.videoParameters.customParameters),
            audioCodec = task.audioParameters.codec,
            audioBitRate = task.audioParameters.bitRate,
            audioSampleRate = task.audioParameters.sampleRate,
            audioChannels = task.audioParameters.channels,
            status = task.status.name,
            progress = task.progress,
            speed = task.speed,
            estimatedTimeRemaining = task.estimatedTimeRemaining,
            errorMessage = task.errorMessage,
            createdAt = task.createdAt,
            startedAt = task.startedAt,
            completedAt = task.completedAt
        )
    }
    
    fun toDomain(entity: TaskEntity): TranscodeTask {
        val customParameters: Map<String, String> = try {
            Json.decodeFromString(entity.customParameters)
        } catch (e: Exception) {
            emptyMap()
        }
        
        return TranscodeTask(
            id = entity.id,
            inputFile = VideoFile(
                id = entity.id,
                name = entity.inputFileName,
                path = entity.inputFilePath,
                size = entity.inputFileSize,
                duration = entity.inputDuration,
                width = entity.inputWidth,
                height = entity.inputHeight,
                frameRate = entity.inputFrameRate,
                bitRate = entity.inputBitRate,
                format = entity.inputFormat,
                codec = entity.inputCodec,
                createdAt = entity.createdAt
            ),
            outputPath = entity.outputPath,
            videoParameters = VideoParameters(
                outputFormat = entity.outputFormat,
                width = entity.outputWidth,
                height = entity.outputHeight,
                frameRate = entity.outputFrameRate,
                bitRate = entity.outputBitRate,
                compressionLevel = entity.compressionLevel,
                encoder = entity.encoder,
                targetFileSize = entity.targetFileSize,
                enableHardwareAcceleration = entity.enableHardwareAcceleration,
                customParameters = customParameters
            ),
            audioParameters = AudioParameters(
                codec = entity.audioCodec,
                bitRate = entity.audioBitRate,
                sampleRate = entity.audioSampleRate,
                channels = entity.audioChannels
            ),
            status = TaskStatus.valueOf(entity.status),
            progress = entity.progress,
            speed = entity.speed,
            estimatedTimeRemaining = entity.estimatedTimeRemaining,
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt
        )
    }
}
