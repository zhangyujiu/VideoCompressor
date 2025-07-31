package com.video.compressor.core.utils

/**
 * 应用常量定义
 */
object Constants {
    
    // 支持的视频格式
    object VideoFormats {
        val INPUT_FORMATS = listOf("mp4", "mov", "avi", "mkv", "flv", "wmv", "3gp")
        val OUTPUT_FORMATS = listOf("mp4", "mov", "avi", "mkv", "gif", "webm")
    }
    
    // 预设分辨率
    object Resolutions {
        const val RES_480P = "854x480"
        const val RES_720P = "1280x720"
        const val RES_1080P = "1920x1080"
        const val RES_2K = "2560x1440"
        const val RES_4K = "3840x2160"
        
        val PRESET_RESOLUTIONS = listOf(
            RES_480P,
            RES_720P,
            RES_1080P,
            RES_2K,
            RES_4K
        )
    }
    
    // 预设帧率
    object FrameRates {
        const val FPS_24 = 24
        const val FPS_30 = 30
        const val FPS_60 = 60
        
        val PRESET_FRAME_RATES = listOf(FPS_24, FPS_30, FPS_60)
    }
    
    // 压缩级别
    object CompressionLevels {
        const val LOW = "low"
        const val MEDIUM = "medium"
        const val HIGH = "high"
        const val CUSTOM = "custom"
    }
    
    // 编码器
    object Encoders {
        const val H264 = "libx264"
        const val H265 = "libx265"
    }
    
    // 数据存储键
    object DataStoreKeys {
        const val DEFAULT_OUTPUT_FORMAT = "default_output_format"
        const val DEFAULT_COMPRESSION_LEVEL = "default_compression_level"
        const val DEFAULT_OUTPUT_DIRECTORY = "default_output_directory"
        const val HARDWARE_ACCELERATION_ENABLED = "hardware_acceleration_enabled"
    }
}
