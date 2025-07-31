package com.video.compressor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import com.video.compressor.domain.service.FileService
import com.video.compressor.domain.service.FFmpegService
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

/**
 * 简单的测试应用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TestApp(
    fileService: FileService = koinInject(),
    ffmpegService: FFmpegService = koinInject()
) {
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var ffmpegAvailable by remember { mutableStateOf(false) }
    var ffmpegVersion by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // 检查FFmpeg状态
    LaunchedEffect(Unit) {
        ffmpegAvailable = ffmpegService.isFFmpegAvailable()
        ffmpegVersion = ffmpegService.getFFmpegVersion()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VideoCompressor - 测试版") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "VideoCompressor",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "视频转码应用测试版",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 平台信息卡片
                PlatformInfoCard()

                Spacer(modifier = Modifier.height(16.dp))

                // FFmpeg状态卡片
                FFmpegStatusCard(
                    isAvailable = ffmpegAvailable,
                    version = ffmpegVersion,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    onDownloadClick = {
                        scope.launch {
                            isDownloading = true
                            ffmpegService.downloadFFmpeg { progress ->
                                downloadProgress = progress
                            }.onSuccess {
                                ffmpegAvailable = true
                                ffmpegVersion = ffmpegService.getFFmpegVersion()
                                statusMessage = "FFmpeg安装成功！"
                            }.onFailure { error ->
                                statusMessage = "FFmpeg安装失败: ${error.message}"
                            }
                            isDownloading = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 文件选择状态
                selectedFilePath?.let { path ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "已选择文件:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = path.substringAfterLast("/").substringAfterLast("\\"),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "路径: $path",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
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
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    fileService.selectVideoFile()
                                        .onSuccess { filePath ->
                                            selectedFilePath = filePath
                                            statusMessage = if (filePath != null) {
                                                "文件选择成功！"
                                            } else {
                                                "未选择文件"
                                            }
                                        }
                                        .onFailure { error ->
                                            statusMessage = "文件选择失败: ${error.message}"
                                        }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择视频")
                        }

                        OutlinedButton(
                            onClick = {
                                statusMessage = "设置功能开发中..."
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("设置")
                        }
                    }

                    // 新增：切换到完整应用的按钮
                    Button(
                        onClick = { /* TODO: 切换到完整应用 */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("启动完整应用")
                    }
                }
            }
        }
    }
}

/**
 * 平台信息卡片组件
 */
@Composable
private fun PlatformInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "平台信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = getPlatformInfo(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = getFFmpegImplementation(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// 平台特定函数
expect fun getPlatformInfo(): String
expect fun getFFmpegImplementation(): String

/**
 * FFmpeg状态卡片组件
 */
@Composable
private fun FFmpegStatusCard(
    isAvailable: Boolean,
    version: String?,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    onDownloadClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isAvailable) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Text(
                    text = "FFmpeg状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            Text(
                text = if (isAvailable) {
                    "✅ FFmpeg已安装并可用"
                } else {
                    "❌ FFmpeg未找到"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )

            version?.let { v ->
                Text(
                    text = "版本: ${v.take(50)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    }
                )
            }

            if (!isAvailable) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDownloadClick,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下载中... $downloadProgress%")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下载FFmpeg")
                    }
                }

                Text(
                    text = "FFmpeg是视频转码的核心组件，需要先安装才能使用转码功能。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
