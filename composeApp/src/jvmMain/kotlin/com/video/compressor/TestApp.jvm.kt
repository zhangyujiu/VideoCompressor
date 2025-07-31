package com.video.compressor

actual fun getPlatformInfo(): String {
    val osName = System.getProperty("os.name")
    val osVersion = System.getProperty("os.version")
    val javaVersion = System.getProperty("java.version")
    
    return "🖥️ JVM桌面平台\n" +
           "操作系统: $osName $osVersion\n" +
           "Java版本: $javaVersion"
}

actual fun getFFmpegImplementation(): String {
    return "FFmpeg实现: 系统FFmpeg (优先) + 应用内置FFmpeg (备用)"
}
