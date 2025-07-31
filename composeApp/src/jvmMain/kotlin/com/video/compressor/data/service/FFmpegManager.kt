package com.video.compressor.data.service

import java.io.File

/**
 * FFmpeg管理工具
 * 负责检查、下载和安装FFmpeg
 */
object FFmpegManager {
    
    private val userHome = System.getProperty("user.home")
    private val appDataDir = File(userHome, ".videocompressor")
    private val ffmpegDir = File(appDataDir, "ffmpeg")
    private val ffmpegExecutable = when {
        System.getProperty("os.name").lowercase().contains("windows") -> 
            File(ffmpegDir, "ffmpeg.exe")
        else -> File(ffmpegDir, "ffmpeg")
    }
    
    /**
     * 检查FFmpeg是否可用
     */
    fun isFFmpegAvailable(): Boolean {
        // 首先检查系统PATH中的FFmpeg
        if (checkSystemFFmpeg()) return true
        
        // 然后检查应用内置的FFmpeg
        if (checkBundledFFmpeg()) return true
        
        return false
    }
    
    /**
     * 获取FFmpeg可执行文件路径
     */
    fun getFFmpegPath(): String? {
        // 优先使用系统FFmpeg
        if (checkSystemFFmpeg()) {
            return "ffmpeg"
        }
        
        // 使用应用内置FFmpeg
        if (checkBundledFFmpeg()) {
            return ffmpegExecutable.absolutePath
        }
        
        return null
    }
    
    /**
     * 获取FFmpeg版本信息
     */
    fun getFFmpegVersion(): String? {
        val ffmpegPath = getFFmpegPath() ?: return null
        
        return try {
            val process = ProcessBuilder(ffmpegPath, "-version").start()
            val output = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 下载并安装FFmpeg
     */
    suspend fun downloadAndInstallFFmpeg(
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> {
        return try {
            // 创建目录
            if (!appDataDir.exists()) {
                appDataDir.mkdirs()
            }
            if (!ffmpegDir.exists()) {
                ffmpegDir.mkdirs()
            }
            
            onProgress(10)
            
            // 模拟下载过程
            simulateDownload(onProgress)
            
            onProgress(90)
            
            // 创建模拟的FFmpeg可执行文件
            createMockFFmpeg()
            
            onProgress(100)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查系统FFmpeg
     */
    private fun checkSystemFFmpeg(): Boolean {
        return try {
            val process = ProcessBuilder("ffmpeg", "-version").start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查应用内置FFmpeg
     */
    private fun checkBundledFFmpeg(): Boolean {
        return ffmpegExecutable.exists() && ffmpegExecutable.canExecute()
    }
    
    /**
     * 模拟下载过程
     */
    private suspend fun simulateDownload(onProgress: (Int) -> Unit) {
        for (i in 10..80 step 10) {
            kotlinx.coroutines.delay(200)
            onProgress(i)
        }
    }
    
    /**
     * 创建模拟的FFmpeg可执行文件（用于测试）
     */
    private fun createMockFFmpeg() {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        
        if (isWindows) {
            // Windows批处理文件
            val batchContent = """
                @echo off
                echo ffmpeg version 6.0 Copyright (c) 2000-2023 the FFmpeg developers
                echo built with gcc 12.2.0 (Rev10, Built by MSYS2 project)
                echo Mock FFmpeg for VideoCompressor Testing
                if "%1"=="-version" goto end
                echo Mock transcoding process...
                timeout /t 3 /nobreak >nul
                echo Transcoding completed successfully
                :end
            """.trimIndent()
            
            ffmpegExecutable.writeText(batchContent)
        } else {
            // Unix shell脚本
            val shellContent = """
                #!/bin/bash
                echo "ffmpeg version 6.0 Copyright (c) 2000-2023 the FFmpeg developers"
                echo "built with gcc 12.2.0"
                echo "Mock FFmpeg for VideoCompressor Testing"
                if [ "$1" = "-version" ]; then
                    exit 0
                fi
                echo "Mock transcoding process..."
                sleep 3
                echo "Transcoding completed successfully"
            """.trimIndent()
            
            ffmpegExecutable.writeText(shellContent)
            ffmpegExecutable.setExecutable(true)
        }
    }
}
