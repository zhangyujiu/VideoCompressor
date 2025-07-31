package com.video.compressor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.presentation.navigation.AppNavigation
import kotlinx.coroutines.launch

/**
 * 主应用组件
 * 支持在测试模式和完整应用模式之间切换
 */
@Composable
fun VideoCompressorApp() {
    // 直接进入完整应用模式
    AppNavigation()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestAppWithNavigation(
    onLaunchFullApp: () -> Unit
) {
    TestAppEnhanced(onLaunchFullApp = onLaunchFullApp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullAppWithNavigation(
    onBackToTest: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频压缩器") },
                navigationIcon = {
                    IconButton(onClick = onBackToTest) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回测试模式")
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
            AppNavigation()
        }
    }
}

/**
 * 增强版TestApp，添加了导航功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestAppEnhanced(
    onLaunchFullApp: () -> Unit
) {
    // 复用原有的TestApp逻辑，但添加导航按钮
    val fileService: com.video.compressor.domain.service.FileService = org.koin.compose.koinInject()
    val ffmpegService: com.video.compressor.domain.service.FFmpegService = org.koin.compose.koinInject()
    
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
                    title = { Text("视频压缩器 - 测试模式") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 模式切换卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🧪 测试模式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "当前运行在测试模式下，可以测试基础功能和平台集成。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Button(
                            onClick = onLaunchFullApp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("启动完整应用")
                        }
                    }
                }
                
                // 平台信息卡片
                SimplePlatformInfoCard()

                // FFmpeg状态卡片
                SimpleFFmpegStatusCard(
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
                
                // 文件选择状态
                selectedFilePath?.let { path ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "已选择文件:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = path.substringAfterLast("/").substringAfterLast("\\"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                // 状态消息
                if (statusMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = statusMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                // 测试按钮
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
                        Text("测试文件选择")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            statusMessage = "平台信息: ${getPlatformInfo()}"
                        }
                    ) {
                        Text("测试平台信息")
                    }
                }
            }
        }
    }
}

enum class AppMode {
    TEST,
    FULL
}

@Composable
private fun SimplePlatformInfoCard() {
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
            Text(
                text = "平台信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

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

@Composable
private fun SimpleFFmpegStatusCard(
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
                        Text("下载FFmpeg")
                    }
                }
            }
        }
    }
}
