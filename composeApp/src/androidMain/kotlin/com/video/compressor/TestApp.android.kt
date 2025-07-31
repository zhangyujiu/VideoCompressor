package com.video.compressor

import android.os.Build

actual fun getPlatformInfo(): String {
    return "📱 Android平台\n" +
           "设备: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
           "Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
           "架构: ${Build.SUPPORTED_ABIS.joinToString(", ")}"
}

actual fun getFFmpegImplementation(): String {
    return "FFmpeg实现: FFmpeg-Kit (内置) + 硬件加速支持"
}
