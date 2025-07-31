package com.video.compressor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.video.compressor.di.androidModule
import org.koin.core.module.Module

actual fun getPlatformModule(): Module = androidModule

/**
 * Android平台的App内容实现
 * 由于Koin已经在Application中初始化，这里直接使用MaterialTheme
 */
@Composable
actual fun AppContent() {
    MaterialTheme {
        VideoCompressorApp()
    }
}
