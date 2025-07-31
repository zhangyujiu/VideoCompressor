package com.video.compressor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform