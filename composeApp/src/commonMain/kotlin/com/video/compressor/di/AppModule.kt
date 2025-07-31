package com.video.compressor.di

import com.video.compressor.data.local.database.InMemoryDatabase
import com.video.compressor.data.repository.VideoRepositoryImpl
import com.video.compressor.data.repository.SettingsRepositoryImpl
import com.video.compressor.domain.repository.VideoRepository
import com.video.compressor.domain.repository.SettingsRepository

import com.video.compressor.domain.usecase.GetVideoInfoUseCase
import com.video.compressor.domain.usecase.ManageTasksUseCase
import com.video.compressor.domain.usecase.TranscodeVideoUseCase

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * 应用依赖注入模块
 */
val appModule = module {

    // 数据库
    single { InMemoryDatabase.getInstance() }

    // 仓库
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl() }

    // 用例
    singleOf(::GetVideoInfoUseCase)
    singleOf(::TranscodeVideoUseCase)
    singleOf(::ManageTasksUseCase)

    // ViewModels (暂时注释掉，稍后修复)
    // viewModelOf(::HomeViewModel)
    // viewModelOf(::TranscodeSettingsViewModel)
    // viewModelOf(::TaskManagerViewModel)
}
