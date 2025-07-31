package com.video.compressor.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.service.FFmpegService
import com.video.compressor.domain.service.TranscodeProgress
import com.video.compressor.presentation.screen.DiagnosticsScreen
import com.video.compressor.presentation.screen.MainScreen
import com.video.compressor.presentation.screen.SettingsScreen
import com.video.compressor.presentation.screen.TaskManagerScreen
import com.video.compressor.presentation.screen.TranscodeConfigScreen
import com.video.compressor.presentation.screen.TranscodeProgressScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.random.Random

/**
 * 应用导航管理
 */
sealed class Screen {
    object Main : Screen()
    data class TranscodeConfig(val videoFile: VideoFile) : Screen()
    data class TranscodeProgress(val task: TranscodeTask) : Screen()
    object TaskManager : Screen()
    object Settings : Screen()
    object Diagnostics : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    ffmpegService: FFmpegService = koinInject()
) {
    // 返回栈管理
    var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Main)) }
    val currentScreen = navigationStack.last()
    var currentProgress by remember { mutableStateOf<TranscodeProgress?>(null) }

    // 导航函数
    fun navigateTo(screen: Screen) {
        navigationStack = navigationStack + screen
        println("AppNavigation: 导航到 $screen, 栈深度: ${navigationStack.size}")
    }

    fun navigateBack(): Boolean {
        return if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
            println("AppNavigation: 返回到 ${navigationStack.last()}, 栈深度: ${navigationStack.size}")
            true
        } else {
            println("AppNavigation: 已在根页面，无法返回")
            false
        }
    }

    // 任务管理状态
    var allTasks by remember { mutableStateOf<List<TranscodeTask>>(emptyList()) }
    var taskProgressMap by remember { mutableStateOf<Map<String, TranscodeProgress>>(emptyMap()) }

    // FFmpeg状态缓存
    var ffmpegAvailable by remember { mutableStateOf<Boolean?>(null) } // null表示未检测
    var ffmpegCheckTime by remember { mutableStateOf(0L) } // 上次检测时间

    val scope = rememberCoroutineScope()

    // FFmpeg状态检测（带缓存）
    LaunchedEffect(Unit) {
        val currentTime = System.currentTimeMillis()
        val cacheValidTime = 5 * 60 * 1000L // 5分钟缓存

        // 如果没有检测过，或者缓存已过期，则重新检测
        if (ffmpegAvailable == null || (currentTime - ffmpegCheckTime) > cacheValidTime) {
            println("AppNavigation: 检测FFmpeg状态...")
            val isAvailable = ffmpegService.isFFmpegAvailable()
            ffmpegAvailable = isAvailable
            ffmpegCheckTime = currentTime
            println("AppNavigation: FFmpeg状态检测完成 - 可用: $isAvailable")
        } else {
            println("AppNavigation: 使用缓存的FFmpeg状态 - 可用: $ffmpegAvailable")
        }
    }

    // 获取当前页面标题
    val currentTitle = when (currentScreen) {
        is Screen.Main -> "视频压缩器"
        is Screen.TranscodeConfig -> "转码设置"
        is Screen.TranscodeProgress -> "转码进度"
        is Screen.TaskManager -> "任务管理"
        is Screen.Settings -> "设置"
        is Screen.Diagnostics -> "诊断信息"
    }

    // 是否显示返回按钮
    val showBackButton = currentScreen !is Screen.Main

    // 处理系统返回键（Android平台）
    PlatformBackHandler(enabled = showBackButton) {
        navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                navigateBack()
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    // 只在主页面显示任务管理和设置按钮
                    if (currentScreen is Screen.Main) {
                        IconButton(
                            onClick = {
                                navigateTo(Screen.TaskManager)
                            }
                        ) {
                            Icon(Icons.Default.List, contentDescription = "任务管理")
                        }
                        IconButton(
                            onClick = {
                                navigateTo(Screen.Settings)
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
        is Screen.Main -> {
            MainScreen(
                onVideoSelected = { videoFile ->
                    navigateTo(Screen.TranscodeConfig(videoFile))
                },
                ffmpegAvailable = ffmpegAvailable,
                onRefreshFFmpegStatus = {
                    // 强制重新检测FFmpeg状态
                    scope.launch {
                        println("AppNavigation: 强制重新检测FFmpeg状态...")
                        val isAvailable = ffmpegService.isFFmpegAvailable()
                        ffmpegAvailable = isAvailable
                        ffmpegCheckTime = System.currentTimeMillis()
                        println("AppNavigation: FFmpeg状态重新检测完成 - 可用: $isAvailable")
                    }
                }
            )
        }

        is Screen.TranscodeConfig -> {
            TranscodeConfigScreen(
                videoFile = screen.videoFile,
                onBack = {
                    navigateBack()
                },
                onStartTranscode = { videoParams, audioParams, outputPath ->
                    val task = TranscodeTask(
                        id = Random.nextLong().toString(),
                        inputFile = screen.videoFile,
                        outputPath = outputPath,
                        videoParameters = videoParams,
                        audioParameters = audioParams,
                        status = TaskStatus.PENDING,
                        createdAt = System.currentTimeMillis()
                    )

                    // 添加任务到列表
                    allTasks = allTasks + task

                    navigateTo(Screen.TranscodeProgress(task))

                    // 开始转码
                    scope.launch {
                        println("AppNavigation: 开始监听转码进度 - 任务ID: ${task.id}")
                        ffmpegService.startTranscode(task).collect { progress ->
                            println("AppNavigation: 收到进度更新 - 状态: ${progress.status}, 进度: ${progress.progress}")
                            currentProgress = progress
                            taskProgressMap = taskProgressMap + (task.id to progress)
                            if (progress.status == TaskStatus.COMPLETED) {
                                println("AppNavigation: 转码完成，状态已更新")
                            }
                        }
                        println("AppNavigation: 转码Flow已结束")
                    }

                    // 移除复杂的持续监听，依赖Flow的自然完成
                }
            )
        }
        
        is Screen.TranscodeProgress -> {
            val progress = currentProgress ?: TranscodeProgress(
                taskId = screen.task.id,
                status = TaskStatus.PENDING,
                progress = 0f
            )
            
            TranscodeProgressScreen(
                progress = progress,
                onCancel = {
                    scope.launch {
                        ffmpegService.stopTranscode(screen.task.id)
                        navigateBack()
                    }
                },
                onComplete = {
                    // 转码完成，可以添加完成后的操作
                    navigateBack()
                }
            )
        }

        is Screen.TaskManager -> {
            TaskManagerScreen(
                tasks = allTasks,
                taskProgress = taskProgressMap,
                onBack = {
                    navigateBack()
                },
                onCancelTask = { taskId ->
                    scope.launch {
                        ffmpegService.stopTranscode(taskId)
                    }
                },
                onRetryTask = { task ->
                    // 重新开始转码
                    scope.launch {
                        ffmpegService.startTranscode(task).collect { progress ->
                            taskProgressMap = taskProgressMap + (task.id to progress)
                        }
                    }
                },
                onDeleteTask = { taskId ->
                    allTasks = allTasks.filter { it.id != taskId }
                    taskProgressMap = taskProgressMap - taskId
                }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                onBack = {
                    navigateBack()
                },
                onDiagnosticsClick = {
                    navigateTo(Screen.Diagnostics)
                }
            )
        }

        is Screen.Diagnostics -> {
            DiagnosticsScreen(
                onBack = {
                    navigateBack()
                }
            )
        }
            }
        }
    }
}
