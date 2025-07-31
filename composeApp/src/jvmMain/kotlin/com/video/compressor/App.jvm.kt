package com.video.compressor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.video.compressor.di.appModule
import com.video.compressor.di.jvmModule
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

actual fun getPlatformModule(): Module = jvmModule

/**
 * JVM平台的App内容实现
 * 需要初始化KoinApplication
 */
@Composable
actual fun AppContent() {
    KoinApplication(application = {
        modules(appModule, getPlatformModule())
    }) {
        MaterialTheme {
            VideoCompressorApp()
        }
    }
}
