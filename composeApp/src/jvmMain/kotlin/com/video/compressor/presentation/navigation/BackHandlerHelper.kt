package com.video.compressor.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * JVM平台的返回键处理（空实现）
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // JVM平台不需要处理系统返回键
}
