package com.video.compressor.domain.service

import com.video.compressor.domain.model.VideoFile

/**
 * 文件服务接口
 */
interface FileService {
    
    /**
     * 选择视频文件
     */
    suspend fun selectVideoFile(): Result<String?>
    
    /**
     * 选择输出目录
     */
    suspend fun selectOutputDirectory(): Result<String?>
    
    /**
     * 获取视频文件信息
     */
    suspend fun getVideoInfo(filePath: String): Result<VideoFile>
    
    /**
     * 检查文件是否存在
     */
    suspend fun fileExists(filePath: String): Boolean
    
    /**
     * 获取文件大小
     */
    suspend fun getFileSize(filePath: String): Long
    
    /**
     * 删除文件
     */
    suspend fun deleteFile(filePath: String): Result<Unit>
    
    /**
     * 创建目录
     */
    suspend fun createDirectory(directoryPath: String): Result<Unit>
}
