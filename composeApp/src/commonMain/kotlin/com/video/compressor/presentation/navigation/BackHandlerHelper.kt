package com.video.compressor.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * 跨平台的返回键处理接口
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
)
