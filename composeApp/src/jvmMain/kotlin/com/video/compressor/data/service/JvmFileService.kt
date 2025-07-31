package com.video.compressor.data.service

import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.service.FileService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.random.Random

/**
 * JVM平台文件服务实现
 */
class JvmFileService : FileService {

    override suspend fun selectVideoFile(): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用 CompletableDeferred 来处理 Swing 回调
                val deferred = kotlinx.coroutines.CompletableDeferred<String?>()

                // 在 EDT 线程中执行 Swing 操作
                javax.swing.SwingUtilities.invokeLater {
                    try {
                        val fileChooser = JFileChooser().apply {
                            dialogTitle = "选择视频文件"
                            fileSelectionMode = JFileChooser.FILES_ONLY
                            isMultiSelectionEnabled = false

                            // 添加视频文件过滤器
                            val videoFilter = FileNameExtensionFilter(
                                "视频文件 (*.mp4, *.mov, *.avi, *.mkv, *.flv, *.wmv, *.3gp)",
                                "mp4", "mov", "avi", "mkv", "flv", "wmv", "3gp"
                            )
                            fileFilter = videoFilter
                        }

                        val result = fileChooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            deferred.complete(fileChooser.selectedFile.absolutePath)
                        } else {
                            deferred.complete(null)
                        }
                    } catch (e: Exception) {
                        deferred.completeExceptionally(e)
                    }
                }

                // 等待 Swing 操作完成
                val filePath = deferred.await()
                Result.success(filePath)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun selectOutputDirectory(): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用 CompletableDeferred 来处理 Swing 回调
                val deferred = kotlinx.coroutines.CompletableDeferred<String?>()

                // 在 EDT 线程中执行 Swing 操作
                javax.swing.SwingUtilities.invokeLater {
                    try {
                        val fileChooser = JFileChooser().apply {
                            dialogTitle = "选择输出目录"
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            isMultiSelectionEnabled = false
                        }

                        val result = fileChooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            deferred.complete(fileChooser.selectedFile.absolutePath)
                        } else {
                            deferred.complete(null)
                        }
                    } catch (e: Exception) {
                        deferred.completeExceptionally(e)
                    }
                }

                // 等待 Swing 操作完成
                val directoryPath = deferred.await()
                Result.success(directoryPath)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getVideoInfo(filePath: String): Result<VideoFile> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("文件不存在: $filePath"))
            }

            // 使用FFmpeg获取真实的视频信息
            val ffmpegPath = com.video.compressor.data.service.FFmpegManager.getFFmpegPath()
            if (ffmpegPath != null) {
                getVideoInfoWithFFmpeg(file, ffmpegPath)
            } else {
                // 如果FFmpeg不可用，返回基本信息
                getBasicVideoInfo(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getVideoInfoWithFFmpeg(file: File, ffmpegPath: String): Result<VideoFile> {
        return withContext(Dispatchers.IO) {
            try {
                val command = listOf(
                    ffmpegPath,
                    "-i", file.absolutePath,
                    "-hide_banner"
                )

                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                parseFFmpegOutput(file, output)
            } catch (e: Exception) {
                getBasicVideoInfo(file)
            }
        }
    }

    private fun parseFFmpegOutput(file: File, output: String): Result<VideoFile> {
        try {
            var duration = 0
            var width = 0
            var height = 0
            var frameRate = 30.0
            var bitRate = 0L
            var codec = "unknown"

            // 解析时长 Duration: 00:05:30.25
            val durationRegex = """Duration: (\d{2}):(\d{2}):(\d{2})\.(\d{2})""".toRegex()
            durationRegex.find(output)?.let { match ->
                val hours = match.groupValues[1].toInt()
                val minutes = match.groupValues[2].toInt()
                val seconds = match.groupValues[3].toInt()
                duration = hours * 3600 + minutes * 60 + seconds
            }

            // 解析分辨率和帧率 Video: h264, yuv420p, 1920x1080, 30 fps
            val videoRegex =
                """Video: (\w+).*?(\d{3,4})x(\d{3,4}).*?(\d+(?:\.\d+)?) fps""".toRegex()
            videoRegex.find(output)?.let { match ->
                codec = match.groupValues[1]
                width = match.groupValues[2].toInt()
                height = match.groupValues[3].toInt()
                frameRate = match.groupValues[4].toDouble()
            }

            // 解析比特率 bitrate: 5000 kb/s
            val bitrateRegex = """bitrate: (\d+) kb/s""".toRegex()
            bitrateRegex.find(output)?.let { match ->
                bitRate = match.groupValues[1].toLong() * 1000
            }

            val videoFile = VideoFile(
                id = Random.nextLong().toString(),
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                duration = duration,
                width = width,
                height = height,
                frameRate = frameRate,
                bitRate = bitRate,
                format = file.extension.lowercase(),
                codec = codec,
                createdAt = file.lastModified()
            )

            return Result.success(videoFile)
        } catch (e: Exception) {
            return getBasicVideoInfo(file)
        }
    }

    private fun getBasicVideoInfo(file: File): Result<VideoFile> {
        val videoFile = VideoFile(
            id = Random.nextLong().toString(),
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            duration = 0, // 未知
            width = 0, // 未知
            height = 0, // 未知
            frameRate = 30.0, // 默认
            bitRate = 0L, // 未知
            format = file.extension.lowercase(),
            codec = "unknown",
            createdAt = file.lastModified()
        )

        return Result.success(videoFile)
    }

    override suspend fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    override suspend fun getFileSize(filePath: String): Long {
        return File(filePath).length()
    }

    override suspend fun deleteFile(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("无法删除文件: $filePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDirectory(directoryPath: String): Result<Unit> {
        return try {
            val directory = File(directoryPath)
            if (directory.mkdirs() || directory.exists()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("无法创建目录: $directoryPath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
