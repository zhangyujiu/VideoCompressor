package com.video.compressor.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.service.FileService
import com.video.compressor.domain.service.FFmpegService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onVideoSelected: (VideoFile) -> Unit = {},
    onFormatConvert: (VideoFile) -> Unit = {},
    ffmpegAvailable: Boolean? = null, // null表示检测中
    onRefreshFFmpegStatus: () -> Unit = {},
    fileService: FileService = koinInject(),
    ffmpegService: FFmpegService = koinInject()
) {
    var selectedVideos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        // 主要内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 移除顶部操作栏，按钮已移到标题栏
            // 移除FloatingActionButton，现在通过点击空白区域选择视频
            // FFmpeg状态检查
            when (ffmpegAvailable) {
                null -> {
                    // 检测中
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在检测FFmpeg状态...")
                        }
                    }
                }

                false -> {
                    // FFmpeg不可用
                    FFmpegWarningCard(
                        onDownloadClick = {
                            scope.launch {
                                ffmpegService.downloadFFmpeg { progress ->
                                    statusMessage = "下载FFmpeg: $progress%"
                                }.onSuccess {
                                    onRefreshFFmpegStatus() // 刷新状态
                                    statusMessage = "FFmpeg安装成功"
                                }.onFailure { error ->
                                    statusMessage = "FFmpeg安装失败: ${error.message}"
                                }
                            }
                        }
                    )
                }

                true -> {
                    // FFmpeg可用，不显示警告卡片
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 状态消息
        if (statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 视频列表
        if (selectedVideos.isEmpty()) {
            EmptyStateCard(
                onSelectVideo = {
                    scope.launch {
                        isLoading = true
                        fileService.selectVideoFile()
                            .onSuccess { filePath ->
                                if (filePath != null) {
                                    fileService.getVideoInfo(filePath)
                                        .onSuccess { videoFile ->
                                            selectedVideos = selectedVideos + videoFile
                                            statusMessage = "已选择视频: ${videoFile.name}"
                                        }
                                        .onFailure { error ->
                                            statusMessage = "获取视频信息失败: ${error.message}"
                                        }
                                } else {
                                    statusMessage = "未选择文件"
                                }
                            }
                            .onFailure { error ->
                                statusMessage = "文件选择失败: ${error.message}"
                            }
                        isLoading = false
                    }
                }
            )
        } else {
            Text(
                text = "已选择的视频 (${selectedVideos.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedVideos) { video ->
                    VideoFileCard(
                        videoFile = video,
                        onTranscode = { onVideoSelected(video) },
                        onFormatConvert = { onFormatConvert(video) },
                        onRemove = {
                            selectedVideos = selectedVideos - video
                        }
                    )
                }
            }
        }

        // 加载指示器
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
private fun FFmpegWarningCard(onDownloadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ FFmpeg未安装",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "FFmpeg是视频转码的核心组件，需要先安装才能使用转码功能。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("安装FFmpeg")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    onSelectVideo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectVideo() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📹",
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = "还没有选择视频",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "点击这里选择要压缩的视频文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VideoFileCard(
    videoFile: VideoFile,
    onTranscode: () -> Unit,
    onFormatConvert: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = videoFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onRemove) {
                    Text("移除")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFileSize(videoFile.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatDuration(videoFile.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (videoFile.width > 0 && videoFile.height > 0) {
                Text(
                    text = "${videoFile.width}x${videoFile.height} • ${videoFile.codec}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTranscode,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("转码压缩")
                }
                
                OutlinedButton(
                    onClick = onFormatConvert,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("格式转换")
                }
            }
        }
    }
}

// 辅助函数
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "未知时长"

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, secs)
        else -> "%d:%02d".format(minutes, secs)
    }
}
