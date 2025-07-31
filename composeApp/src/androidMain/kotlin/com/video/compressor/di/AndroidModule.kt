package com.video.compressor.di

import com.video.compressor.data.service.AndroidFileService
import com.video.compressor.data.service.AndroidFFmpegService
import com.video.compressor.domain.service.FileService
import com.video.compressor.domain.service.FFmpegService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android平台特定的依赖注入模块
 */
val androidModule = module {

    // 平台特定服务实例
    single { AndroidFileService(androidContext()) }
    single { AndroidFFmpegService(androidContext()) }

    // 接口绑定
    single<FileService> { get<AndroidFileService>() }
    single<FFmpegService> { get<AndroidFFmpegService>() }
}
