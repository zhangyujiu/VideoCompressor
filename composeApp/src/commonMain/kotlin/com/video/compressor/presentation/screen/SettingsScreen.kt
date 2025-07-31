package com.video.compressor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.service.FFmpegService
import com.video.compressor.domain.service.FileService
import com.video.compressor.getFFmpegImplementation
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onDiagnosticsClick: () -> Unit = {},
    ffmpegService: FFmpegService = koinInject(),
    fileService: FileService = koinInject()
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FFmpeg设置
        FFmpegSettingsCard(
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



        // 诊断工具
        DiagnosticsCard(onDiagnosticsClick)

        // 应用信息
        AppInfoCard()

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
    }
}


@Composable
private fun FFmpegSettingsCard(
    isAvailable: Boolean,
    version: String?,
    isDownloading: Boolean,
    downloadProgress: Int,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "FFmpeg设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // FFmpeg状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAvailable) "✅ FFmpeg已安装" else "❌ FFmpeg未安装",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    version?.let { v ->
                        Text(
                            text = "版本: ${v.take(50)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isAvailable) {
                    Button(
                        onClick = onDownloadClick,
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("下载中... $downloadProgress%")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("安装FFmpeg")
                        }
                    }
                }
            }

            // FFmpeg说明
            Text(
                text = "FFmpeg是视频转码的核心组件，用于处理各种视频格式的转换和压缩。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@Composable
private fun DiagnosticsCard(onDiagnosticsClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "诊断工具",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "如果转码失败，可以使用诊断工具检查系统环境和FFmpeg配置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onDiagnosticsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("运行诊断")
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "应用信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            InfoRow("应用名称", "视频压缩器")
            InfoRow("版本", "1.0.0")
            InfoRow("平台", "跨平台应用")
            InfoRow("FFmpeg实现", "平台优化")

            Text(
                text = "这是一个跨平台的视频压缩应用，支持Android和桌面平台，使用FFmpeg进行高质量的视频转码。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
