package com.video.compressor.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
 * 转码配置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscodeConfigScreen(
    videoFile: VideoFile,
    onBack: () -> Unit = {},
    onStartTranscode: (VideoParameters, AudioParameters, String) -> Unit = { _, _, _ -> }
) {
    var selectedEncoder by remember { mutableStateOf("libx264") }
    var selectedResolution by remember { mutableStateOf("原始分辨率") }
    var selectedQuality by remember { mutableStateOf("高质量") }
    var selectedFormat by remember { mutableStateOf("mp4") }
    var outputFileName by remember { mutableStateOf(generateOutputFileName(videoFile.name)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 源文件信息
        SourceFileInfoCard(videoFile)

        // 视频设置
        VideoSettingsCard(
            selectedEncoder = selectedEncoder,
            onEncoderChange = { selectedEncoder = it },
            selectedResolution = selectedResolution,
            onResolutionChange = { selectedResolution = it },
            selectedQuality = selectedQuality,
            onQualityChange = { selectedQuality = it }
        )

        // 输出设置
        OutputSettingsCard(
            selectedFormat = selectedFormat,
            onFormatChange = { selectedFormat = it },
            outputFileName = outputFileName,
            onFileNameChange = { outputFileName = it }
        )

        // 开始转码按钮
        Button(
            onClick = {
                val videoParams = createVideoParameters(
                    selectedEncoder,
                    selectedResolution,
                    selectedQuality,
                    videoFile
                )
                val audioParams = createAudioParameters()
                val outputPath = createOutputPath(outputFileName, selectedFormat)
                onStartTranscode(videoParams, audioParams, outputPath)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始转码")
        }
    }
}


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

@Composable
private fun VideoSettingsCard(
    selectedEncoder: String,
    onEncoderChange: (String) -> Unit,
    selectedResolution: String,
    onResolutionChange: (String) -> Unit,
    selectedQuality: String,
    onQualityChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "视频设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 编码器选择
            DropdownSelector(
                label = "编码器",
                selectedValue = selectedEncoder,
                options = listOf("libx264", "libx265", "h264_nvenc", "hevc_nvenc"),
                onValueChange = onEncoderChange
            )

            // 分辨率选择
            DropdownSelector(
                label = "分辨率",
                selectedValue = selectedResolution,
                options = listOf("原始分辨率", "1920x1080", "1280x720", "854x480"),
                onValueChange = onResolutionChange
            )

            // 质量选择
            DropdownSelector(
                label = "质量",
                selectedValue = selectedQuality,
                options = listOf("高质量", "中等质量", "压缩优先"),
                onValueChange = onQualityChange
            )
        }
    }
}

@Composable
private fun OutputSettingsCard(
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
                text = "输出设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 格式选择
            DropdownSelector(
                label = "输出格式",
                selectedValue = selectedFormat,
                options = listOf("mp4", "mov", "avi", "mkv"),
                onValueChange = onFormatChange
            )

            // 文件名输入
            OutlinedTextField(
                value = outputFileName,
                onValueChange = onFileNameChange,
                label = { Text("输出文件名") },
                modifier = Modifier.fillMaxWidth()
            )
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

// 辅助函数
private fun generateOutputFileName(originalName: String): String {
    val nameWithoutExtension = originalName.substringBeforeLast(".")
    return "${nameWithoutExtension}_compressed"
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

private fun createVideoParameters(
    encoder: String,
    resolution: String,
    quality: String,
    originalVideo: VideoFile
): VideoParameters {
    val (width, height) = when (resolution) {
        "1920x1080" -> Pair(1920, 1080)
        "1280x720" -> Pair(1280, 720)
        "854x480" -> Pair(854, 480)
        else -> Pair(originalVideo.width, originalVideo.height)
    }

    val bitRate = when (quality) {
        "高质量" -> 8000
        "中等质量" -> 4000
        "压缩优先" -> 2000
        else -> 4000
    }

    return VideoParameters(
        encoder = encoder,
        width = if (width > 0) width else null,
        height = if (height > 0) height else null,
        frameRate = originalVideo.frameRate.toInt(),
        bitRate = bitRate.toLong(),
        outputFormat = "mp4",
        compressionLevel = "23"
    )
}

private fun createAudioParameters(): AudioParameters {
    return AudioParameters(
        codec = "aac",
        bitRate = 128,
        sampleRate = 44100,
        channels = 2
    )
}

private fun createOutputPath(fileName: String, format: String): String {
    return "$fileName.$format"
}
