package com.video.compressor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppContent()
}

@Composable
expect fun AppContent()

expect fun getPlatformModule(): org.koin.core.module.Module