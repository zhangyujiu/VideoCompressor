package com.video.compressor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.model.VideoParameters
import com.video.compressor.domain.model.AudioParameters

/**
 * 格式转换界面 - 专门用于格式转换，保持原有参数不变
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatConvertScreen(
    videoFile: VideoFile,
    onBack: () -> Unit = {},
    onStartConvert: (VideoParameters, AudioParameters, String) -> Unit = { _, _, _ -> }
) {
    var selectedFormat by remember { mutableStateOf("mp4") }
    var outputFileName by remember { mutableStateOf(generateConvertFileName(videoFile.name, selectedFormat)) }

    // 当格式改变时，自动更新文件名
    LaunchedEffect(selectedFormat) {
        outputFileName = generateConvertFileName(videoFile.name, selectedFormat)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 源文件信息
        SourceFileInfoCard(videoFile)

        // 格式转换设置
        FormatConvertCard(
            currentFormat = videoFile.format,
            selectedFormat = selectedFormat,
            onFormatChange = { selectedFormat = it },
            outputFileName = outputFileName,
            onFileNameChange = { outputFileName = it }
        )

        // 转换说明
        ConvertInfoCard()

        // 开始转换按钮
        Button(
            onClick = {
                val videoParams = createPreserveVideoParameters(videoFile, selectedFormat)
                val audioParams = createPreserveAudioParameters()
                val outputPath = createOutputPath(outputFileName, selectedFormat)
                onStartConvert(videoParams, audioParams, outputPath)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始格式转换")
        }
    }
}

@Composable
private fun FormatConvertCard(
    currentFormat: String,
    selectedFormat: String,
    onFormatChange: (String) -> Unit,
    outputFileName: String,
    onFileNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "格式转换设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 当前格式显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "当前格式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentFormat.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "转换",
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = "目标格式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedFormat.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 格式选择
            DropdownSelector(
                label = "选择输出格式",
                selectedValue = selectedFormat,
                options = listOf("mp4", "mov", "avi", "mkv", "webm", "flv"),
                onValueChange = onFormatChange
            )

            // 文件名输入
            OutlinedTextField(
                value = outputFileName,
                onValueChange = onFileNameChange,
                label = { Text("输出文件名") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("文件将保存为: $outputFileName.$selectedFormat")
                }
            )
        }
    }
}

@Composable
private fun ConvertInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 转换说明",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "• 格式转换将保持原有的分辨率、质量和编码设置",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• 仅改变视频容器格式，转换速度较快",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• 如需修改视频参数，请使用完整转码功能",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 生成格式转换的输出文件名
 */
private fun generateConvertFileName(originalName: String, targetFormat: String): String {
    val nameWithoutExtension = originalName.substringBeforeLast('.')
    return "${nameWithoutExtension}_converted"
}

/**
 * 创建保持原有参数的视频参数
 */
private fun createPreserveVideoParameters(videoFile: VideoFile, outputFormat: String): VideoParameters {
    return VideoParameters(
        outputFormat = outputFormat,
        encoder = "copy", // 使用copy表示不重新编码
        width = videoFile.width,
        height = videoFile.height,
        frameRate = videoFile.frameRate.toInt(),
        bitRate = null, // 保持原有比特率
        compressionLevel = "preserve" // 保持原有质量
    )
}

/**
 * 创建保持原有参数的音频参数
 */
private fun createPreserveAudioParameters(): AudioParameters {
    return AudioParameters(
        codec = "copy", // 使用copy表示不重新编码
        bitRate = 128 // 默认音频比特率，实际会被copy覆盖
    )
}

/**
 * 创建输出路径
 */
private fun createOutputPath(fileName: String, format: String): String {
    return "$fileName.$format"
}

// 从TranscodeConfigScreen复制的辅助函数和组件
@Composable
private fun SourceFileInfoCard(videoFile: VideoFile) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "源文件信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InfoRow("文件名", videoFile.name)
            InfoRow("大小", formatFileSize(videoFile.size))
            InfoRow("时长", formatDuration(videoFile.duration))
            InfoRow("分辨率", "${videoFile.width}x${videoFile.height}")
            InfoRow("帧率", "${videoFile.frameRate} fps")
            InfoRow("编码器", videoFile.codec)
            InfoRow("格式", videoFile.format.uppercase())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
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
    if (seconds <= 0) return "未知"

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, secs)
        else -> "%d:%02d".format(minutes, secs)
    }
}