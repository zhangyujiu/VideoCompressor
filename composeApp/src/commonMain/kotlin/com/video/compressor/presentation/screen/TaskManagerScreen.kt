package com.video.compressor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.model.TaskStatus
import com.video.compressor.domain.model.TranscodeTask
import com.video.compressor.domain.service.TranscodeProgress

/**
 * 任务管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    tasks: List<TranscodeTask> = emptyList(),
    taskProgress: Map<String, TranscodeProgress> = emptyMap(),
    onBack: () -> Unit = {},
    onCancelTask: (String) -> Unit = {},
    onRetryTask: (TranscodeTask) -> Unit = {},
    onDeleteTask: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (tasks.isEmpty()) {
            EmptyTasksCard()
        } else {
            // 任务统计
            TaskStatisticsCard(tasks, taskProgress)

            Spacer(modifier = Modifier.height(16.dp))

            // 任务列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    val progress = taskProgress[task.id]
                    TaskCard(
                        task = task,
                        progress = progress,
                        onCancel = { onCancelTask(task.id) },
                        onRetry = { onRetryTask(task) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = "暂无转码任务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "创建新的转码任务后，可以在这里查看和管理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskStatisticsCard(
    tasks: List<TranscodeTask>,
    taskProgress: Map<String, TranscodeProgress>
) {
    val runningTasks = tasks.count { task ->
        taskProgress[task.id]?.status == TaskStatus.RUNNING
    }
    val completedTasks = tasks.count { task ->
        taskProgress[task.id]?.status == TaskStatus.COMPLETED
    }
    val failedTasks = tasks.count { task ->
        taskProgress[task.id]?.status == TaskStatus.FAILED
    }

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
            Text(
                text = "任务统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    "总计",
                    tasks.size.toString(),
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatisticItem("进行中", runningTasks.toString(), MaterialTheme.colorScheme.primary)
                StatisticItem(
                    "已完成",
                    completedTasks.toString(),
                    MaterialTheme.colorScheme.primary
                )
                StatisticItem("失败", failedTasks.toString(), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: TranscodeTask,
    progress: TranscodeProgress?,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 任务标题和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.inputFile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "输出: ${task.outputPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TaskStatusIcon(progress?.status ?: TaskStatus.PENDING)
            }

            // 进度信息
            if (progress != null) {
                TaskProgressInfo(progress)
            }

            // 操作按钮
            TaskActionButtons(
                status = progress?.status ?: TaskStatus.PENDING,
                onCancel = onCancel,
                onRetry = onRetry,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun TaskStatusIcon(status: TaskStatus) {
    val (icon, color) = when (status) {
        TaskStatus.PENDING -> Icons.Default.Pause to MaterialTheme.colorScheme.outline
        TaskStatus.RUNNING -> Icons.Default.PlayArrow to MaterialTheme.colorScheme.primary
        TaskStatus.PAUSED -> Icons.Default.Pause to MaterialTheme.colorScheme.outline
        TaskStatus.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        TaskStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
        TaskStatus.CANCELLED -> Icons.Default.Cancel to MaterialTheme.colorScheme.outline
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun TaskProgressInfo(progress: TranscodeProgress) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 进度条
        if (progress.status == TaskStatus.RUNNING) {
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = progress.speed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 详细信息
        if (progress.time.isNotEmpty() || progress.estimatedTimeRemaining > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (progress.time.isNotEmpty()) {
                    Text(
                        text = "时间: ${progress.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (progress.estimatedTimeRemaining > 0) {
                    Text(
                        text = "剩余: ${formatTime(progress.estimatedTimeRemaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 错误信息
        progress.errorMessage?.let { error ->
            Text(
                text = "错误: $error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TaskActionButtons(
    status: TaskStatus,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (status) {
            TaskStatus.RUNNING -> {
                OutlinedButton(onClick = onCancel) {
                    Text("取消")
                }
            }

            TaskStatus.FAILED -> {
                OutlinedButton(onClick = onRetry) {
                    Text("重试")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("删除")
                }
            }

            TaskStatus.COMPLETED, TaskStatus.CANCELLED -> {
                OutlinedButton(onClick = onDelete) {
                    Text("删除")
                }
            }

            else -> {
                // 其他状态暂不显示按钮
            }
        }
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
