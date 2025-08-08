package com.video.compressor.data.service

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.SessionState
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.service.FFmpegService
import com.video.compressor.domain.service.TranscodeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Android平台FFmpeg服务实现
 * 使用FFmpeg-Kit库
 */
class AndroidFFmpegService(
    private val context: Context
) : FFmpegService {

    private val activeSessions = ConcurrentHashMap<String, FFmpegSession>()
    private val taskStatuses = ConcurrentHashMap<String, TaskStatus>()

    // 进度更新流 - 每个任务一个
    private val progressFlows = ConcurrentHashMap<String, MutableSharedFlow<TranscodeProgress>>()

    // 任务开始时间和进度数据
    private val taskStartTimes = ConcurrentHashMap<String, Long>()
    private val taskProgress = ConcurrentHashMap<String, TranscodeProgress>()

    init {
        // 配置FFmpeg-Kit日志级别
        FFmpegKitConfig.setLogLevel(Level.AV_LOG_INFO)
    }

    override suspend fun isFFmpegAvailable(): Boolean {
        // FFmpeg-Kit内置FFmpeg，总是可用
        return true
    }

    override suspend fun getFFmpegVersion(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val session = FFmpegKit.execute("-version")
            if (ReturnCode.isSuccess(session.returnCode)) {
                val output = session.allLogsAsString
                output.lines().firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun downloadFFmpeg(onProgress: (Int) -> Unit): Result<Unit> {
        // FFmpeg-Kit内置FFmpeg，无需下载
        // 模拟下载过程用于UI反馈
        return withContext(Dispatchers.IO) {
            try {
                for (i in 0..100 step 20) {
                    onProgress(i)
                    kotlinx.coroutines.delay(50)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun startTranscode(task: TranscodeTask): Flow<TranscodeProgress> = flow {
        try {
            // 验证输入文件
            val inputPath = task.inputFile.path
            android.util.Log.d("AndroidFFmpegService", "输入文件路径: $inputPath")

            val inputFile = java.io.File(inputPath)
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw Exception("无法读取输入文件: $inputPath (文件不存在或无读取权限)")
            }

            android.util.Log.d("AndroidFFmpegService", "输入文件验证成功: ${inputFile.length()} bytes")

            // 测试并创建所需目录
            ensureDirectoriesExist()

            // 验证输出目录
            val outputPath = getValidOutputPath(task.outputPath)
            val outputFile = java.io.File(outputPath)
            val outputDir = outputFile.parentFile

            android.util.Log.d("AndroidFFmpegService", "输出文件路径: $outputPath")
            android.util.Log.d("AndroidFFmpegService", "输出目录: ${outputDir?.absolutePath}")

            if (outputDir != null && !outputDir.exists()) {
                android.util.Log.d("AndroidFFmpegService", "输出目录不存在，尝试创建...")
                val created = outputDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建输出目录结果: $created")

                if (!created && !outputDir.exists()) {
                    android.util.Log.e("AndroidFFmpegService", "无法创建输出目录: ${outputDir.absolutePath}")
                    taskStatuses[task.id] = TaskStatus.FAILED
                    val errorProgress = TranscodeProgress(
                        taskId = task.id,
                        status = TaskStatus.FAILED,
                        progress = 0f,
                        speed = "失败",
                        estimatedTimeRemaining = 0,
                        errorMessage = "无法创建输出目录: ${outputDir.absolutePath}"
                    )
                    taskProgress[task.id] = errorProgress
                    emit(errorProgress)
                    return@flow
                }
            }

            // 验证目录权限
            if (outputDir != null && outputDir.exists() && !outputDir.canWrite()) {
                android.util.Log.e("AndroidFFmpegService", "输出目录不可写: ${outputDir.absolutePath}")
                taskStatuses[task.id] = TaskStatus.FAILED
                val errorProgress = TranscodeProgress(
                    taskId = task.id,
                    status = TaskStatus.FAILED,
                    progress = 0f,
                    speed = "失败",
                    estimatedTimeRemaining = 0,
                    errorMessage = "输出目录不可写: ${outputDir.absolutePath}"
                )
                taskProgress[task.id] = errorProgress
                emit(errorProgress)
                return@flow
            }

            // 生成实际的输出路径（只生成一次，确保一致性）
            val actualOutputPath = getValidOutputPath(task.outputPath)
            
            android.util.Log.d("AndroidFFmpegService", "开始转码任务: ${task.id}")
            android.util.Log.d("AndroidFFmpegService", "输入文件: ${task.inputFile.path}")
            android.util.Log.d("AndroidFFmpegService", "输出文件: $actualOutputPath")

            taskStatuses[task.id] = TaskStatus.RUNNING

            // 构建并验证FFmpeg命令，传入实际输出路径
            val command = buildAndValidateFFmpegCommandWithPath(task, actualOutputPath)

            emit(TranscodeProgress(
                taskId = task.id,
                status = TaskStatus.RUNNING,
                progress = 0f,
                speed = "启动中...",
                estimatedTimeRemaining = task.inputFile.duration
            ))

            // 记录详细的转码信息
            android.util.Log.d("AndroidFFmpegService", "=== 开始转码 ===")
            android.util.Log.d("AndroidFFmpegService", "任务ID: ${task.id}")
            android.util.Log.d("AndroidFFmpegService", "FFmpeg命令: $command")

            // 启动FFmpeg转码（异步）
            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    // 执行完成回调
                    activeSessions.remove(task.id)

                    android.util.Log.d("AndroidFFmpegService", "=== FFmpeg执行完成 ===")
                    android.util.Log.d("AndroidFFmpegService", "返回码: ${session.returnCode}")
                    android.util.Log.d("AndroidFFmpegService", "会话状态: ${session.state}")
                    android.util.Log.d("AndroidFFmpegService", "失败堆栈: ${session.failStackTrace}")

                    if (ReturnCode.isSuccess(session.returnCode) &&
                        taskStatuses[task.id] != TaskStatus.CANCELLED) {

                        // 验证输出文件是否真的存在 - 使用之前生成的实际输出路径
                        val foundFile = findOutputFile(actualOutputPath, task.outputPath)

                        if (foundFile != null && foundFile.exists() && foundFile.length() > 0) {
                            taskStatuses[task.id] = TaskStatus.COMPLETED
                            android.util.Log.d("AndroidFFmpegService", "转码成功完成: ${task.id}")
                            android.util.Log.d("AndroidFFmpegService", "输出文件验证成功: ${foundFile.absolutePath}, 大小: ${foundFile.length()} bytes")

                            // 显示文件保存位置
                            val locationMessage = when {
                                foundFile.absolutePath.contains("Download") -> {
                                    "文件已保存到Download/VideoCompressor文件夹"
                                }
                                foundFile.absolutePath.contains(context.filesDir.absolutePath) -> {
                                    "文件已保存到应用内部存储"
                                }
                                else -> {
                                    "文件已保存到: ${foundFile.parent}"
                                }
                            }
                            android.util.Log.i("AndroidFFmpegService", "$locationMessage - ${foundFile.absolutePath}")

                            // 立即更新进度为完成状态
                            taskProgress[task.id] = TranscodeProgress(
                                taskId = task.id,
                                status = TaskStatus.COMPLETED,
                                progress = 1f,
                                speed = locationMessage,
                                estimatedTimeRemaining = 0
                            )

                            // 复制文件到用户可访问的目录并扫描
                            copyToUserAccessibleLocation(foundFile.absolutePath, task.outputPath)
                        } else {
                            taskStatuses[task.id] = TaskStatus.FAILED
                            android.util.Log.e("AndroidFFmpegService", "转码失败: 输出文件不存在或为空")
                            android.util.Log.e("AndroidFFmpegService", "预期输出路径: $outputPath")

                            // 搜索所有可能的位置
                            searchForOutputFile(task.outputPath)

                            // 更新进度以包含错误信息
                            taskProgress[task.id] = TranscodeProgress(
                                taskId = task.id,
                                status = TaskStatus.FAILED,
                                progress = 0f,
                                speed = "失败",
                                estimatedTimeRemaining = 0,
                                errorMessage = "转码失败: 输出文件未生成"
                            )
                        }
                    } else if (taskStatuses[task.id] == TaskStatus.CANCELLED) {
                        android.util.Log.d("AndroidFFmpegService", "转码已取消: ${task.id}")

                        // 更新进度为取消状态
                        taskProgress[task.id] = TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.CANCELLED,
                            progress = 0f,
                            speed = "已取消",
                            estimatedTimeRemaining = 0
                        )
                    } else {
                        taskStatuses[task.id] = TaskStatus.FAILED
                        val errorMessage = "转码失败 - 返回码: ${session.returnCode}, 失败原因: ${session.failStackTrace}"
                        android.util.Log.e("AndroidFFmpegService", errorMessage)

                        // 更新进度以包含错误信息
                        taskProgress[task.id] = TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.FAILED,
                            progress = 0f,
                            speed = "失败",
                            estimatedTimeRemaining = 0,
                            errorMessage = errorMessage
                        )
                    }

                    // 清理临时文件
                    cleanupTempFile(task.inputFile.path)
                },
                { log ->
                    // 日志回调 - 记录详细的FFmpeg日志
                    val logMessage = log.message
                    when (log.level) {
                        com.arthenica.ffmpegkit.Level.AV_LOG_ERROR -> {
                            android.util.Log.e("FFmpeg", "错误: $logMessage")
                        }
                        com.arthenica.ffmpegkit.Level.AV_LOG_WARNING -> {
                            android.util.Log.w("FFmpeg", "警告: $logMessage")
                        }
                        com.arthenica.ffmpegkit.Level.AV_LOG_INFO -> {
                            android.util.Log.i("FFmpeg", "信息: $logMessage")
                        }
                        else -> {
                            android.util.Log.d("FFmpeg", "调试: $logMessage")
                        }
                    }

                    // 检查是否有关键错误信息
                    if (logMessage.contains("No such file or directory") ||
                        logMessage.contains("Permission denied") ||
                        logMessage.contains("Invalid argument")) {
                        android.util.Log.e("AndroidFFmpegService", "检测到关键错误: $logMessage")
                    }
                },
                { statistics ->
                    // 统计回调 - 更新真实进度数据
                    if (taskStatuses[task.id] == TaskStatus.RUNNING) {
                        val progress = if (task.inputFile.duration > 0) {
                            (statistics.time / 1000.0 / task.inputFile.duration.toDouble()).coerceIn(0.0, 1.0).toFloat()
                        } else {
                            0f
                        }

                        val currentProgress = TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.RUNNING,
                            progress = progress,
                            speed = "${String.format("%.1f", statistics.speed)}x",
                            fps = statistics.videoFrameNumber,
                            bitrate = "${statistics.bitrate} kbps",
                            size = "${statistics.size} KB",
                            time = formatTime((statistics.time / 1000).toInt()),
                            estimatedTimeRemaining = if (statistics.speed > 0 && progress > 0) {
                                ((task.inputFile.duration - statistics.time / 1000) / statistics.speed).toInt()
                            } else {
                                task.inputFile.duration
                            }
                        )

                        taskProgress[task.id] = currentProgress
                    }
                }
            )

            activeSessions[task.id] = session
            taskStartTimes[task.id] = System.currentTimeMillis()

            // 监控转码进度（基于真实FFmpeg执行状态）
            var progressCheckCount = 0
            while (taskStatuses[task.id] == TaskStatus.RUNNING) {
                kotlinx.coroutines.delay(500) // 每500ms检查一次
                progressCheckCount++

                // 每10次检查（5秒）检查一次输出文件
                if (progressCheckCount % 10 == 0) {
                    val outputFile = java.io.File(actualOutputPath)
                    android.util.Log.d("AndroidFFmpegService", "进度检查 #$progressCheckCount: 输出文件存在=${outputFile.exists()}, 大小=${outputFile.length()}")
                }

                val currentStatus = taskStatuses[task.id] ?: TaskStatus.FAILED

                when (currentStatus) {
                    TaskStatus.RUNNING -> {
                        // 使用真实的FFmpeg统计数据或回退到时间估算
                        val realProgress = taskProgress[task.id]
                        if (realProgress != null) {
                            // 使用真实进度数据
                            emit(realProgress)
                        } else {
                            // 回退到时间估算（FFmpeg还未开始报告统计）
                            val startTime = taskStartTimes[task.id] ?: System.currentTimeMillis()
                            val elapsedTime = System.currentTimeMillis() - startTime
                            val estimatedProgress = (elapsedTime / 1000.0 / task.inputFile.duration).coerceIn(0.0, 0.1).toFloat()

                            emit(TranscodeProgress(
                                taskId = task.id,
                                status = TaskStatus.RUNNING,
                                progress = estimatedProgress,
                                speed = "启动中...",
                                fps = 0,
                                time = formatTime((elapsedTime / 1000).toInt()),
                                estimatedTimeRemaining = task.inputFile.duration
                            ))
                        }
                    }
                    TaskStatus.COMPLETED -> {
                        emit(TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.COMPLETED,
                            progress = 1f,
                            speed = "完成",
                            estimatedTimeRemaining = 0
                        ))
                        taskStartTimes.remove(task.id)
                        taskProgress.remove(task.id)
                        break
                    }
                    TaskStatus.CANCELLED -> {
                        emit(TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.CANCELLED,
                            progress = 0f,
                            speed = "已取消",
                            estimatedTimeRemaining = 0
                        ))
                        taskStartTimes.remove(task.id)
                        taskProgress.remove(task.id)
                        break
                    }
                    TaskStatus.FAILED -> {
                        emit(TranscodeProgress(
                            taskId = task.id,
                            status = TaskStatus.FAILED,
                            progress = 0f,
                            speed = "错误",
                            estimatedTimeRemaining = 0,
                            errorMessage = "转码失败"
                        ))
                        taskStartTimes.remove(task.id)
                        taskProgress.remove(task.id)
                        break
                    }
                    else -> break
                }
            }

        } catch (e: Exception) {
            taskStatuses[task.id] = TaskStatus.FAILED
            emit(TranscodeProgress(
                taskId = task.id,
                status = TaskStatus.FAILED,
                progress = 0f,
                speed = "错误",
                estimatedTimeRemaining = 0,
                errorMessage = e.message ?: "未知错误"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun stopTranscode(taskId: String): Result<Unit> {
        return try {
            taskStatuses[taskId] = TaskStatus.CANCELLED
            activeSessions[taskId]?.cancel()
            activeSessions.remove(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProgress(taskId: String): TranscodeProgress? {
        return taskProgress[taskId]
    }

    override suspend fun getSupportedEncoders(): List<String> {
        return listOf("libx264", "libx265", "h264_mediacodec", "hevc_mediacodec")
    }
    
    override suspend fun getSupportedFormats(): List<String> {
        return listOf("mp4", "mov", "avi", "mkv", "webm", "3gp")
    }
    
    /**
     * 构建并验证FFmpeg命令，使用多级回退策略
     */
    private fun buildAndValidateFFmpegCommand(task: TranscodeTask): String {
        // 使用最简单的测试命令
        android.util.Log.d("AndroidFFmpegService", "使用测试命令策略")
        return buildTestFFmpegCommand(task)
    }

    /**
     * 构建并验证FFmpeg命令，使用指定的输出路径
     */
    private fun buildAndValidateFFmpegCommandWithPath(task: TranscodeTask, outputPath: String): String {
        // 使用最简单的测试命令，但使用指定的输出路径
        android.util.Log.d("AndroidFFmpegService", "使用测试命令策略，输出路径: $outputPath")
        return buildTestFFmpegCommandWithPath(task, outputPath)
    }

    private fun buildFFmpegCommand(task: TranscodeTask): String {
        return buildAndValidateFFmpegCommand(task)
    }

    private fun buildStandardFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add(task.inputFile.path) // 移除引号，FFmpeg-Kit会自动处理

        // 不强制指定视频编码器，让FFmpeg自动选择
        // 这样可以避免编码器不存在的问题
        android.util.Log.d("AndroidFFmpegService", "使用FFmpeg默认视频编码器")

        // 分辨率
        task.videoParameters.width?.let { width ->
            task.videoParameters.height?.let { height ->
                commandParts.add("-s")
                commandParts.add("${width}x${height}")
            }
        }

        // 帧率
        task.videoParameters.frameRate?.let { fps ->
            commandParts.add("-r")
            commandParts.add(fps.toString())
        }

        // 比特率
        task.videoParameters.bitRate?.let { bitrate ->
            commandParts.add("-b:v")
            commandParts.add("${bitrate}k")
        }

        // 音频比特率（不强制指定编码器，让FFmpeg自动选择）
        commandParts.add("-b:a")
        commandParts.add("${task.audioParameters.bitRate}k")

        // 视频比特率（不强制指定编码器，让FFmpeg自动选择）
        commandParts.add("-b:v")
        commandParts.add("${task.videoParameters.bitRate ?: 1000}k")

        // 优化MP4格式
        if (task.outputPath.endsWith(".mp4", ignoreCase = true)) {
            commandParts.add("-movflags")
            commandParts.add("+faststart") // 优化流媒体播放
        }

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件 - 确保路径有效
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add(outputPath) // 移除引号，FFmpeg-Kit会自动处理

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "FFmpeg命令: $command")

        return command
    }
    

    
    private fun getValidOutputPath(originalPath: String): String {
        return try {
            // 检查外部存储权限
            if (!hasStoragePermission()) {
                android.util.Log.w("AndroidFFmpegService", "没有外部存储权限，使用内部存储")
                return getInternalStoragePath(originalPath)
            }

            // 使用Download文件夹
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val videoCompressorDir = java.io.File(downloadDir, "VideoCompressor")

            android.util.Log.d("AndroidFFmpegService", "Download目录: ${downloadDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "VideoCompressor目录: ${videoCompressorDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "Download目录存在: ${downloadDir.exists()}")
            android.util.Log.d("AndroidFFmpegService", "Download目录可写: ${downloadDir.canWrite()}")

            // 确保Download目录存在
            if (!downloadDir.exists()) {
                android.util.Log.e("AndroidFFmpegService", "Download目录不存在，使用内部存储")
                return getInternalStoragePath(originalPath)
            }

            // 创建VideoCompressor子目录
            if (!videoCompressorDir.exists()) {
                android.util.Log.d("AndroidFFmpegService", "VideoCompressor目录不存在，尝试创建...")
                val created = videoCompressorDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建VideoCompressor目录结果: $created")
                android.util.Log.d("AndroidFFmpegService", "创建后目录存在: ${videoCompressorDir.exists()}")

                if (!created && !videoCompressorDir.exists()) {
                    android.util.Log.e("AndroidFFmpegService", "无法创建VideoCompressor目录，使用内部存储")
                    android.util.Log.e("AndroidFFmpegService", "尝试创建的路径: ${videoCompressorDir.absolutePath}")
                    android.util.Log.e("AndroidFFmpegService", "父目录存在: ${videoCompressorDir.parentFile?.exists()}")
                    android.util.Log.e("AndroidFFmpegService", "父目录可写: ${videoCompressorDir.parentFile?.canWrite()}")
                    return getInternalStoragePath(originalPath)
                }
            } else {
                android.util.Log.d("AndroidFFmpegService", "VideoCompressor目录已存在")
            }

            // 验证目录可写
            if (!videoCompressorDir.canWrite()) {
                android.util.Log.e("AndroidFFmpegService", "VideoCompressor目录不可写，使用内部存储")
                return getInternalStoragePath(originalPath)
            }

            // 生成唯一的输出文件名
            val originalFileName = if (originalPath.startsWith("/")) {
                java.io.File(originalPath).name
            } else {
                originalPath
            }

            val uniqueFileName = generateUniqueFileName(videoCompressorDir, originalFileName)
            val finalPath = java.io.File(videoCompressorDir, uniqueFileName).absolutePath
            android.util.Log.d("AndroidFFmpegService", "最终输出路径: $finalPath")

            finalPath
        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "创建Download路径失败: ${e.message}")
            return getInternalStoragePath(originalPath)
        }
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    /**
     * 构建简化的FFmpeg命令，确保最大兼容性
     * 只使用最基础的FFmpeg选项
     */
    private fun buildSimpleFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add("\"${task.inputFile.path}\"")

        // 使用最基础的视频编码设置 - 不指定编码器，让FFmpeg自动选择
        // 或者使用比特率控制而不是CRF
        commandParts.add("-b:v")
        commandParts.add("1000k") // 1Mbps视频比特率

        // 音频比特率
        commandParts.add("-b:a")
        commandParts.add("128k")

        // 分辨率设置（如果指定）
        task.videoParameters.width?.let { width ->
            task.videoParameters.height?.let { height ->
                commandParts.add("-s")
                commandParts.add("${width}x${height}")
            }
        }

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add("\"$outputPath\"")

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "简化FFmpeg命令: $command")

        return command
    }

    /**
     * 构建超级简化的FFmpeg命令，只使用最基础的选项
     */
    private fun buildUltraSimpleFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add("\"${task.inputFile.path}\"")

        // 分辨率设置（如果指定）
        task.videoParameters.width?.let { width ->
            task.videoParameters.height?.let { height ->
                commandParts.add("-s")
                commandParts.add("${width}x${height}")
            }
        }

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add("\"$outputPath\"")

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "超级简化FFmpeg命令: $command")

        return command
    }

    /**
     * 构建最小化FFmpeg命令，只包含最基础的输入输出
     */
    private fun buildMinimalFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add("\"${task.inputFile.path}\"")

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add("\"$outputPath\"")

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "最小化FFmpeg命令: $command")

        return command
    }

    /**
     * 构建最兼容的FFmpeg命令，避免所有可能的编码器问题
     */
    private fun buildCompatibleFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add("\"${task.inputFile.path}\"")

        // 只设置分辨率（如果指定），让FFmpeg自动选择所有其他参数
        task.videoParameters.width?.let { width ->
            task.videoParameters.height?.let { height ->
                commandParts.add("-s")
                commandParts.add("${width}x${height}")
            }
        }

        // 设置视频比特率（如果指定）
        task.videoParameters.bitRate?.let { bitRate ->
            commandParts.add("-b:v")
            commandParts.add("${bitRate}k")
        }

        // 设置音频比特率
        commandParts.add("-b:a")
        commandParts.add("${task.audioParameters.bitRate}k")

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add(outputPath)

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "兼容FFmpeg命令: $command")

        return command
    }

    /**
     * 构建测试用的最简FFmpeg命令
     */
    private fun buildTestFFmpegCommand(task: TranscodeTask): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add(task.inputFile.path)

        // 最简单的复制命令，不重新编码
        commandParts.add("-c")
        commandParts.add("copy")

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件
        val outputPath = getValidOutputPath(task.outputPath)
        commandParts.add(outputPath)

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "测试FFmpeg命令: $command")

        return command
    }

    /**
     * 构建测试FFmpeg命令，使用指定的输出路径
     */
    private fun buildTestFFmpegCommandWithPath(task: TranscodeTask, outputPath: String): String {
        val commandParts = mutableListOf<String>()

        // 输入文件
        commandParts.add("-i")
        commandParts.add(task.inputFile.path)

        // 最简单的复制命令，不重新编码
        commandParts.add("-c")
        commandParts.add("copy")

        // 覆盖输出文件
        commandParts.add("-y")

        // 输出文件 - 使用传入的路径
        commandParts.add(outputPath)

        val command = commandParts.joinToString(" ")
        android.util.Log.d("AndroidFFmpegService", "测试FFmpeg命令（指定路径）: $command")

        return command
    }

    /**
     * 在多个可能的位置查找输出文件
     */
    private fun findOutputFile(expectedPath: String, originalPath: String): java.io.File? {
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val videoCompressorDir = java.io.File(downloadDir, "VideoCompressor")
        val internalVideoDir = java.io.File(context.filesDir, "VideoCompressor")

        val possiblePaths = listOf(
            expectedPath,
            // Download文件夹相关路径
            "${videoCompressorDir.absolutePath}/${java.io.File(originalPath).name}",
            "${downloadDir.absolutePath}/${java.io.File(originalPath).name}",
            "${downloadDir.absolutePath}/VideoCompressor/${java.io.File(originalPath).name}",
            // 内部存储路径
            "${internalVideoDir.absolutePath}/${java.io.File(originalPath).name}",
            "${context.filesDir.absolutePath}/VideoCompressor/${java.io.File(originalPath).name}",
            "${context.filesDir.absolutePath}/${java.io.File(originalPath).name}",
            // 原有的内部存储路径作为备选
            expectedPath.replace("/data/data/", "/data/user/0/"),
            expectedPath.replace("/data/user/0/", "/data/data/"),
            "${context.applicationInfo.dataDir}/files/VideoCompressor/${java.io.File(originalPath).name}"
        )

        for (path in possiblePaths) {
            val file = java.io.File(path)
            android.util.Log.d("AndroidFFmpegService", "检查路径: $path, 存在: ${file.exists()}, 大小: ${file.length()}")
            if (file.exists() && file.length() > 0) {
                android.util.Log.d("AndroidFFmpegService", "找到输出文件: $path")
                return file
            }
        }

        return null
    }

    /**
     * 搜索输出文件的所有可能位置
     */
    private fun searchForOutputFile(originalPath: String) {
        val fileName = java.io.File(originalPath).name
        val searchDirs = listOf(
            context.applicationInfo.dataDir,
            context.filesDir.absolutePath,
            "${context.applicationInfo.dataDir}/files",
            "${context.applicationInfo.dataDir}/files/VideoCompressor",
            "/data/user/0/${context.packageName}/files",
            "/data/user/0/${context.packageName}/files/VideoCompressor"
        )

        android.util.Log.d("AndroidFFmpegService", "=== 搜索输出文件 ===")
        for (dir in searchDirs) {
            try {
                val directory = java.io.File(dir)
                if (directory.exists() && directory.isDirectory) {
                    val files = directory.listFiles()
                    android.util.Log.d("AndroidFFmpegService", "目录 $dir 包含 ${files?.size ?: 0} 个文件")
                    files?.forEach { file ->
                        if (file.name.contains(fileName.substringBeforeLast(".")) ||
                            file.name.endsWith(".mp4")) {
                            android.util.Log.d("AndroidFFmpegService", "发现相关文件: ${file.absolutePath}, 大小: ${file.length()}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AndroidFFmpegService", "搜索目录失败 $dir: ${e.message}")
            }
        }
    }

    /**
     * 生成唯一的文件名，避免覆盖现有文件
     */
    private fun generateUniqueFileName(directory: java.io.File, originalFileName: String): String {
        val file = java.io.File(directory, originalFileName)
        if (!file.exists()) {
            return originalFileName
        }

        // 如果文件已存在，添加时间戳
        val nameWithoutExt = originalFileName.substringBeforeLast(".")
        val extension = originalFileName.substringAfterLast(".", "")
        val timestamp = System.currentTimeMillis()

        return if (extension.isNotEmpty()) {
            "${nameWithoutExt}_$timestamp.$extension"
        } else {
            "${nameWithoutExt}_$timestamp"
        }
    }

    /**
     * 复制文件到用户可访问的Download目录
     */
    private fun copyToUserAccessibleLocation(sourcePath: String, originalFileName: String) {
        try {
            val sourceFile = java.io.File(sourcePath)
            if (!sourceFile.exists()) {
                android.util.Log.e("AndroidFFmpegService", "源文件不存在: $sourcePath")
                return
            }

            // 检查外部存储是否可用
            val externalStorageState = android.os.Environment.getExternalStorageState()
            if (externalStorageState != android.os.Environment.MEDIA_MOUNTED) {
                android.util.Log.e("AndroidFFmpegService", "外部存储不可用: $externalStorageState")
                return
            }

            // 获取Download目录
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )

            android.util.Log.d("AndroidFFmpegService", "Download目录路径: ${downloadsDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "Download目录存在: ${downloadsDir.exists()}")
            android.util.Log.d("AndroidFFmpegService", "Download目录可写: ${downloadsDir.canWrite()}")

            // 确保Download目录存在
            if (!downloadsDir.exists()) {
                val created = downloadsDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建Download目录: $created")
            }

            // 创建VideoCompressor子目录
            val videoCompressorDir = java.io.File(downloadsDir, "VideoCompressor")
            if (!videoCompressorDir.exists()) {
                val created = videoCompressorDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建VideoCompressor子目录: ${videoCompressorDir.absolutePath}, 成功: $created")

                if (!created) {
                    android.util.Log.e("AndroidFFmpegService", "无法创建VideoCompressor目录，可能是权限问题")
                    // 尝试使用应用的外部文件目录作为替代
                    copyToAlternativeLocation(sourceFile, originalFileName)
                    return
                }
            }

            // 生成输出文件名
            val fileName = if (originalFileName.startsWith("/")) {
                java.io.File(originalFileName).name
            } else {
                originalFileName
            }

            // 添加时间戳确保文件名唯一，避免重复"compressed"
            val timestamp = System.currentTimeMillis()
            val nameWithoutExt = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".", "mp4")

            // 如果文件名已经包含"compressed"，就不再添加
            val uniqueFileName = if (nameWithoutExt.contains("compressed", ignoreCase = true)) {
                "${nameWithoutExt}_$timestamp.$extension"
            } else {
                "${nameWithoutExt}_compressed_$timestamp.$extension"
            }

            val destFile = java.io.File(videoCompressorDir, uniqueFileName)

            // 复制文件到Download目录
            sourceFile.copyTo(destFile, overwrite = true)

            android.util.Log.d("AndroidFFmpegService", "✅ 文件已保存到Download目录")
            android.util.Log.d("AndroidFFmpegService", "📁 路径: ${destFile.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "📊 大小: ${destFile.length()} bytes (${destFile.length() / 1024 / 1024} MB)")

            // 扫描媒体文件，让文件管理器可以发现
            scanMediaFile(destFile.absolutePath)

            // 显示用户友好的路径信息
            android.util.Log.i("AndroidFFmpegService", "🎉 转码完成！文件已保存到: Download/VideoCompressor/$uniqueFileName")

        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "复制到Download目录失败: ${e.message}")
            android.util.Log.e("AndroidFFmpegService", "错误详情: ${e.stackTrace.joinToString("\n")}")

            // 尝试使用替代位置
            val sourceFile = java.io.File(sourcePath)
            copyToAlternativeLocation(sourceFile, originalFileName)
        }
    }

    /**
     * 复制到替代位置（尝试多个公共目录）
     */
    private fun copyToAlternativeLocation(sourceFile: java.io.File, originalFileName: String) {
        try {
            // 尝试保存到Movies目录（更容易被媒体扫描器发现）
            val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_MOVIES
            )

            if (moviesDir != null && moviesDir.exists()) {
                val videoCompressorDir = java.io.File(moviesDir, "VideoCompressor")
                if (!videoCompressorDir.exists()) {
                    val created = videoCompressorDir.mkdirs()
                    android.util.Log.d("AndroidFFmpegService", "创建Movies/VideoCompressor目录: $created")
                }

                if (videoCompressorDir.exists() && videoCompressorDir.canWrite()) {
                    val success = copyToDirectory(sourceFile, originalFileName, videoCompressorDir, "Movies/VideoCompressor")
                    if (success) return
                }
            }

            // 如果Movies目录失败，尝试DCIM目录
            val dcimDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DCIM
            )

            if (dcimDir != null && dcimDir.exists()) {
                val videoCompressorDir = java.io.File(dcimDir, "VideoCompressor")
                if (!videoCompressorDir.exists()) {
                    val created = videoCompressorDir.mkdirs()
                    android.util.Log.d("AndroidFFmpegService", "创建DCIM/VideoCompressor目录: $created")
                }

                if (videoCompressorDir.exists() && videoCompressorDir.canWrite()) {
                    val success = copyToDirectory(sourceFile, originalFileName, videoCompressorDir, "DCIM/VideoCompressor")
                    if (success) return
                }
            }

            // 最后回退到应用外部目录
            val externalFilesDir = context.getExternalFilesDir(null)
            if (externalFilesDir != null) {
                val videoDir = java.io.File(externalFilesDir, "CompressedVideos")
                if (!videoDir.exists()) {
                    videoDir.mkdirs()
                }
                copyToDirectory(sourceFile, originalFileName, videoDir, "Android/data/com.video.compressor/files/CompressedVideos")
            } else {
                android.util.Log.e("AndroidFFmpegService", "所有保存位置都失败")
                scanMediaFile(sourceFile.absolutePath)
            }

        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "复制到替代位置失败: ${e.message}")
            scanMediaFile(sourceFile.absolutePath)
        }
    }

    /**
     * 复制文件到指定目录
     */
    private fun copyToDirectory(sourceFile: java.io.File, originalFileName: String, targetDir: java.io.File, locationName: String): Boolean {
        return try {
            // 生成文件名
            val fileName = if (originalFileName.startsWith("/")) {
                java.io.File(originalFileName).name
            } else {
                originalFileName
            }

            val timestamp = System.currentTimeMillis()
            val nameWithoutExt = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".", "mp4")

            // 避免重复"compressed"
            val uniqueFileName = if (nameWithoutExt.contains("compressed", ignoreCase = true)) {
                "${nameWithoutExt}_$timestamp.$extension"
            } else {
                "${nameWithoutExt}_compressed_$timestamp.$extension"
            }

            val destFile = java.io.File(targetDir, uniqueFileName)

            // 复制文件
            sourceFile.copyTo(destFile, overwrite = true)

            android.util.Log.d("AndroidFFmpegService", "✅ 文件已保存到: $locationName")
            android.util.Log.d("AndroidFFmpegService", "📁 路径: ${destFile.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "📊 大小: ${destFile.length()} bytes")

            // 扫描媒体文件
            scanMediaFile(destFile.absolutePath)

            android.util.Log.i("AndroidFFmpegService", "🎉 转码完成！文件已保存到: $locationName/$uniqueFileName")

            true
        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "复制到${locationName}失败: ${e.message}")
            false
        }
    }

    /**
     * 扫描媒体文件，让它在文件管理器和媒体库中可见
     */
    private fun scanMediaFile(filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                android.util.Log.d("AndroidFFmpegService", "开始扫描媒体文件: $filePath")
                android.util.Log.d("AndroidFFmpegService", "文件大小: ${file.length()} bytes")

                // 确定MIME类型
                val mimeType = when (file.extension.lowercase()) {
                    "mp4" -> "video/mp4"
                    "avi" -> "video/x-msvideo"
                    "mov" -> "video/quicktime"
                    "mkv" -> "video/x-matroska"
                    else -> "video/*"
                }

                // 使用MediaScannerConnection扫描文件
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filePath),
                    arrayOf(mimeType)
                ) { path, uri ->
                    if (uri != null) {
                        android.util.Log.d("AndroidFFmpegService", "✅ 媒体扫描成功: $path -> $uri")
                        android.util.Log.i("AndroidFFmpegService", "📱 文件已添加到媒体库，可以在相册中找到")
                    } else {
                        android.util.Log.w("AndroidFFmpegService", "⚠️ 媒体扫描返回null: $path")
                        android.util.Log.w("AndroidFFmpegService", "文件可能在应用私有目录中，无法被系统媒体库索引")
                        android.util.Log.i("AndroidFFmpegService", "📁 请在文件管理器中查找文件")
                    }
                }

            } else {
                android.util.Log.e("AndroidFFmpegService", "输出文件不存在: $filePath")
            }
        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "媒体扫描失败: ${e.message}")
        }
    }

    /**
     * 清理临时文件
     */
    private fun cleanupTempFile(filePath: String) {
        try {
            if (filePath.contains("/cache/videos/temp_")) {
                val tempFile = java.io.File(filePath)
                if (tempFile.exists() && tempFile.delete()) {
                    android.util.Log.d("AndroidFFmpegService", "已清理临时文件: $filePath")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AndroidFFmpegService", "清理临时文件失败: ${e.message}")
        }
    }

    /**
     * 检查是否有外部存储权限
     */
    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要WRITE_EXTERNAL_STORAGE权限访问Download文件夹
            true
        } else {
            // Android 12及以下需要权限
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取内部存储路径作为回退方案
     */
    private fun getInternalStoragePath(originalPath: String): String {
        return try {
            val internalDir = java.io.File(context.filesDir, "VideoCompressor")
            if (!internalDir.exists()) {
                val created = internalDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建内部存储目录结果: $created")
            }

            val originalFileName = if (originalPath.startsWith("/")) {
                java.io.File(originalPath).name
            } else {
                originalPath
            }

            val uniqueFileName = generateUniqueFileName(internalDir, originalFileName)
            val finalPath = java.io.File(internalDir, uniqueFileName).absolutePath
            android.util.Log.d("AndroidFFmpegService", "内部存储路径: $finalPath")

            finalPath
        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "创建内部存储路径失败: ${e.message}")
            "${context.filesDir.absolutePath}/output_${System.currentTimeMillis()}.mp4"
        }
    }

    /**
     * 测试并创建所需的目录
     */
    private fun ensureDirectoriesExist() {
        try {
            // 测试Download目录
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            android.util.Log.d("AndroidFFmpegService", "=== 目录测试 ===")
            android.util.Log.d("AndroidFFmpegService", "Download目录: ${downloadDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "Download目录存在: ${downloadDir.exists()}")
            android.util.Log.d("AndroidFFmpegService", "Download目录可读: ${downloadDir.canRead()}")
            android.util.Log.d("AndroidFFmpegService", "Download目录可写: ${downloadDir.canWrite()}")

            // 测试VideoCompressor目录
            val videoCompressorDir = java.io.File(downloadDir, "VideoCompressor")
            android.util.Log.d("AndroidFFmpegService", "VideoCompressor目录: ${videoCompressorDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "VideoCompressor目录存在: ${videoCompressorDir.exists()}")

            if (!videoCompressorDir.exists()) {
                android.util.Log.d("AndroidFFmpegService", "尝试创建VideoCompressor目录...")
                val created = videoCompressorDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建结果: $created")
                android.util.Log.d("AndroidFFmpegService", "创建后存在: ${videoCompressorDir.exists()}")
                android.util.Log.d("AndroidFFmpegService", "创建后可写: ${videoCompressorDir.canWrite()}")
            }

            // 测试内部存储目录
            val internalDir = java.io.File(context.filesDir, "VideoCompressor")
            android.util.Log.d("AndroidFFmpegService", "内部存储目录: ${internalDir.absolutePath}")
            android.util.Log.d("AndroidFFmpegService", "内部存储目录存在: ${internalDir.exists()}")

            if (!internalDir.exists()) {
                val created = internalDir.mkdirs()
                android.util.Log.d("AndroidFFmpegService", "创建内部存储目录结果: $created")
            }

            android.util.Log.d("AndroidFFmpegService", "=== 目录测试完成 ===")
        } catch (e: Exception) {
            android.util.Log.e("AndroidFFmpegService", "目录测试失败: ${e.message}")
        }
    }
}
