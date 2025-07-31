package com.video.compressor.di

import com.video.compressor.data.service.JvmFileService
import com.video.compressor.data.service.JvmFFmpegService
import com.video.compressor.domain.service.FileService
import com.video.compressor.domain.service.FFmpegService
import org.koin.dsl.module

/**
 * JVM平台特定的依赖注入模块
 */
val jvmModule = module {
    
    // 平台特定服务
    single<FileService> { JvmFileService() }
    single<FFmpegService> { JvmFFmpegService() }
}
