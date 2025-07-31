package com.video.compressor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.service.FFmpegService
import com.video.compressor.utils.PlatformInfo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 转码诊断界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit = {},
    ffmpegService: FFmpegService = koinInject()
) {
    var diagnosticResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 诊断说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "转码诊断工具",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "此工具将检查FFmpeg环境、编码器支持和系统兼容性，帮助诊断转码失败的原因。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 开始诊断按钮
        Button(
            onClick = {
                scope.launch {
                    isRunning = true
                    diagnosticResults = runDiagnostics(ffmpegService)
                    isRunning = false
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("诊断中...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始诊断")
            }
        }

        // 诊断结果
        if (diagnosticResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "诊断结果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    diagnosticResults.forEach { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 常见问题和解决方案
        TroubleshootingCard()
    }
}


@Composable
private fun TroubleshootingCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "常见问题和解决方案",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TroubleshootingItem(
                problem = "转码失败 - 编码器不支持",
                solution = "尝试使用libx264或libx265软件编码器，避免使用硬件编码器"
            )

            TroubleshootingItem(
                problem = "输出文件无法创建",
                solution = "检查输出路径权限，确保应用有写入权限"
            )

            TroubleshootingItem(
                problem = "输入文件无法读取",
                solution = "确保文件路径正确，文件存在且应用有读取权限"
            )

            TroubleshootingItem(
                problem = "转码速度很慢",
                solution = "降低输出分辨率或比特率，使用fast预设"
            )

            TroubleshootingItem(
                problem = "应用崩溃",
                solution = "检查设备内存是否充足，尝试重启应用"
            )
        }
    }
}

@Composable
private fun TroubleshootingItem(problem: String, solution: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "问题: $problem",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = "解决方案: $solution",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun runDiagnostics(ffmpegService: FFmpegService): List<String> {
    val results = mutableListOf<String>()

    try {
        // 检查FFmpeg可用性
        results.add("🔍 检查FFmpeg可用性...")
        val isAvailable = ffmpegService.isFFmpegAvailable()
        results.add(if (isAvailable) "✅ FFmpeg可用" else "❌ FFmpeg不可用")

        if (isAvailable) {
            // 检查FFmpeg版本
            results.add("🔍 检查FFmpeg版本...")
            val version = ffmpegService.getFFmpegVersion()
            results.add("📋 版本信息: ${version?.take(100) ?: "未知"}")

            // 检查支持的编码器
            results.add("🔍 检查支持的编码器...")
            val encoders = ffmpegService.getSupportedEncoders()
            results.add("🎬 支持的编码器: ${encoders.joinToString(", ")}")

            // 检查支持的格式
            results.add("🔍 检查支持的格式...")
            val formats = ffmpegService.getSupportedFormats()
            results.add("📁 支持的格式: ${formats.joinToString(", ")}")
        }

        // 系统信息
        results.add("🔍 检查系统信息...")
        results.add("📱 平台: ${PlatformInfo.getPlatformName()}")
        results.add("🖥️ 系统: ${PlatformInfo.getOSVersion()}")
        results.add("💾 内存使用: ${PlatformInfo.getAvailableMemory()}")
        results.add("💿 存储使用: ${PlatformInfo.getAvailableStorage()}")

        results.add("✅ 诊断完成")

    } catch (e: Exception) {
        results.add("❌ 诊断过程中出现错误: ${e.message}")
    }

    return results
}


