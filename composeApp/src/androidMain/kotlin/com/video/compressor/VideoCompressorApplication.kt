package com.video.compressor

import android.app.Application
import com.video.compressor.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Android Application类
 * 负责初始化Koin依赖注入
 */
class VideoCompressorApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化Koin
        startKoin {
            androidContext(this@VideoCompressorApplication)
            modules(appModule, getPlatformModule())
        }
    }
}
