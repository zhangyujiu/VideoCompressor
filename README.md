## 1. 产品概述

**应用名称**：VideoCompressor

### 2.1 视频格式转换

**功能描述**：

- 支持输入常见视频格式：MP4, MOV, AVI, MKV, FLV, WMV, 3GP等
- 支持输出格式：MP4, MOV, AVI, MKV, GIF等

### 2.2 视频压缩

**功能描述**：

- 可调节压缩级别（低/中/高/自定义）
- 智能压缩算法（保持画质的同时减小体积）

### 2.3 视频参数调整

**功能描述**：

- 分辨率调整（支持自定义或预设：480p, 720p, 1080p, 2K, 4K）
- 帧率调整（24fps, 30fps, 60fps等）
- 比特率控制（自动/手动）

## 3. 技术方案

### 3.1 核心技术

- 使用Compose Multiplatform技术，支持Android/JVM两个平台
- 视频处理框架：FFmpeg（核心转码引擎）
- 界面开发：Jetpack Compose

### 3.2 架构设计

- 清晰分层架构（UI层、Domain层、Data层）
- 模块化设计（转码核心模块可独立更新）
- 响应式编程（Kotlin Flow/Coroutines）
