package com.video.compressor.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.service.TranscodeProgress

/**
 * 转码进度界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscodeProgressScreen(
    progress: TranscodeProgress,
    onCancel: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    // 监听转码状态变化，但不自动退出
    var currentProgress by remember { mutableStateOf(progress) }

    LaunchedEffect(progress) {
        println("TranscodeProgressScreen: 接收到进度更新 - 状态: ${progress.status}, 进度: ${progress.progress}")
        currentProgress = progress
        if (progress.status == TaskStatus.COMPLETED) {
            println("TranscodeProgressScreen: 转码完成，显示完成状态")
        } else if (progress.status == TaskStatus.RUNNING) {
            println("TranscodeProgressScreen: 转码进行中 - 进度: ${progress.progress}")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 状态图标
        StatusIcon(currentProgress.status)

        // 状态文本
        Text(
            text = getStatusText(currentProgress.status),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 进度信息卡片
        ProgressInfoCard(currentProgress)

        // 进度条
        if (currentProgress.status == TaskStatus.RUNNING) {
            ProgressIndicator(currentProgress.progress)
        }

        // 详细信息
        if (currentProgress.status == TaskStatus.RUNNING) {
            DetailedInfoCard(currentProgress)
        }

        // 转码完成后显示结果信息
        if (currentProgress.status == TaskStatus.COMPLETED) {
            // 平台特定的保存位置信息
            PlatformSpecificSaveLocationInfo()
        }

        Spacer(modifier = Modifier.weight(1f))

        // 操作按钮
        ActionButtons(
            status = currentProgress.status,
            onCancel = onCancel,
            onComplete = onComplete
        )
        }
    }

@Composable
private fun StatusIcon(status: TaskStatus) {
    val icon = when (status) {
        TaskStatus.RUNNING -> Icons.Default.Pause
        TaskStatus.COMPLETED -> Icons.Default.CheckCircle
        TaskStatus.FAILED -> Icons.Default.Error
        TaskStatus.CANCELLED -> Icons.Default.Cancel
        else -> Icons.Default.Pause
    }
    
    val color = when (status) {
        TaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = color
    )
}

@Composable
private fun ProgressInfoCard(progress: TranscodeProgress) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "转码信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (progress.status == TaskStatus.RUNNING) {
                InfoRow("进度", "${(progress.progress * 100).toInt()}%")
                InfoRow("速度", progress.speed)
                if (progress.estimatedTimeRemaining > 0) {
                    InfoRow("剩余时间", formatTime(progress.estimatedTimeRemaining))
                }
            }
            
            if (progress.errorMessage != null) {
                Text(
                    text = "错误信息: ${progress.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicator(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progress"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailedInfoCard(progress: TranscodeProgress) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (progress.fps > 0) {
                InfoRow("帧率", "${progress.fps} fps")
            }
            
            if (progress.bitrate.isNotEmpty()) {
                InfoRow("比特率", progress.bitrate)
            }
            
            if (progress.size.isNotEmpty()) {
                InfoRow("文件大小", progress.size)
            }
            
            if (progress.time.isNotEmpty()) {
                InfoRow("当前时间", progress.time)
            }

            // 显示输出路径（转码完成时）
            if (progress.status == TaskStatus.COMPLETED) {
                InfoRow("保存位置", "已保存到设备存储")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🎉 转码完成！",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        // 平台特定的保存位置信息
                        PlatformSpecificSaveLocationInfo()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    status: TaskStatus,
    onCancel: () -> Unit,
    onComplete: () -> Unit = {}
) {
    when (status) {
        TaskStatus.RUNNING -> {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("取消转码")
            }
        }
        TaskStatus.COMPLETED -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* 可以添加打开文件夹功能 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查看文件")
                }

                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("返回主页")
                }
            }
        }
        TaskStatus.FAILED, TaskStatus.CANCELLED -> {
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("返回")
            }
        }
        else -> {
            // 其他状态暂不显示按钮
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

private fun getStatusText(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "准备中..."
        TaskStatus.RUNNING -> "转码中..."
        TaskStatus.PAUSED -> "已暂停"
        TaskStatus.COMPLETED -> "转码完成"
        TaskStatus.FAILED -> "转码失败"
        TaskStatus.CANCELLED -> "已取消"
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> "%d小时%d分钟".format(hours, minutes)
        minutes > 0 -> "%d分钟%d秒".format(minutes, secs)
        else -> "%d秒".format(secs)
    }
}

/**
 * 平台特定的保存位置信息
 */
@Composable
expect fun PlatformSpecificSaveLocationInfo()
