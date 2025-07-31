package com.video.compressor.utils

/**
 * 跨平台的系统信息接口
 */
expect object PlatformInfo {
    /**
     * 获取当前平台名称
     */
    fun getPlatformName(): String
    
    /**
     * 获取操作系统版本
     */
    fun getOSVersion(): String
    
    /**
     * 获取可用内存信息
     */
    fun getAvailableMemory(): String
    
    /**
     * 获取存储空间信息
     */
    fun getAvailableStorage(): String
}
