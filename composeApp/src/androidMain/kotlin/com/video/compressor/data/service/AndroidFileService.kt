package com.video.compressor.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.service.FileService
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Android平台文件服务实现
 */
class AndroidFileService(
    private val context: Context
) : FileService {
    
    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var directoryPickerLauncher: ActivityResultLauncher<Intent>? = null
    private var filePickerCallback: ((String?) -> Unit)? = null
    private var directoryPickerCallback: ((String?) -> Unit)? = null
    
    fun initialize(activity: ComponentActivity) {
        // 初始化文件选择器
        filePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            val path = uri?.let { getPathFromUri(it) }
            filePickerCallback?.invoke(path)
            filePickerCallback = null
        }
        
        // 初始化目录选择器
        directoryPickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            val path = uri?.let { getPathFromUri(it) }
            directoryPickerCallback?.invoke(path)
            directoryPickerCallback = null
        }
    }
    
    override suspend fun selectVideoFile(): Result<String?> {
        return try {
            suspendCancellableCoroutine { continuation ->
                filePickerCallback = { path ->
                    if (continuation.isActive) {
                        continuation.resume(Result.success(path))
                    }
                }

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "video/mp4",
                        "video/avi",
                        "video/mkv",
                        "video/mov",
                        "video/wmv",
                        "video/flv",
                        "video/3gp"
                    ))
                }

                filePickerLauncher?.launch(intent) ?: run {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("文件选择器未初始化")))
                    }
                }

                continuation.invokeOnCancellation {
                    filePickerCallback = null
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun selectOutputDirectory(): Result<String?> {
        return try {
            suspendCancellableCoroutine { continuation ->
                directoryPickerCallback = { path ->
                    if (continuation.isActive) {
                        continuation.resume(Result.success(path))
                    }
                }

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

                directoryPickerLauncher?.launch(intent) ?: run {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("目录选择器未初始化")))
                    }
                }

                continuation.invokeOnCancellation {
                    directoryPickerCallback = null
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVideoInfo(filePath: String): Result<VideoFile> {
        return try {
            val uri = Uri.parse(filePath)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DATE_ADDED
                ),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                    val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)) / 1000 // 转换为秒
                    val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
                    val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))
                    val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000

                    // 将内容URI转换为实际文件路径
                    val actualPath = copyUriToTempFile(uri, name ?: "video.mp4")

                    val videoFile = VideoFile(
                        id = Random.nextLong().toString(),
                        name = name ?: "未知文件",
                        path = actualPath,
                        size = size,
                        duration = duration.toInt(),
                        width = width,
                        height = height,
                        frameRate = 30.0, // 默认帧率
                        bitRate = 5000000L, // 默认比特率
                        format = name?.substringAfterLast(".")?.lowercase() ?: "mp4",
                        codec = "h264", // 默认编码器
                        createdAt = dateAdded
                    )
                    
                    Result.success(videoFile)
                } else {
                    Result.failure(Exception("无法获取视频信息"))
                }
            } ?: Result.failure(Exception("无法访问视频文件"))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun fileExists(filePath: String): Boolean {
        return try {
            val uri = Uri.parse(filePath)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getFileSize(filePath: String): Long {
        return try {
            val uri = Uri.parse(filePath)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Video.Media.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    override suspend fun deleteFile(filePath: String): Result<Unit> {
        return try {
            val uri = Uri.parse(filePath)
            val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("无法删除文件"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createDirectory(directoryPath: String): Result<Unit> {
        return try {
            val uri = Uri.parse(directoryPath)
            val createdUri = DocumentsContract.createDocument(
                context.contentResolver,
                uri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                "NewFolder"
            )
            if (createdUri != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("无法创建目录"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getPathFromUri(uri: Uri): String? {
        return try {
            // 对于Android，我们直接返回URI字符串
            // 因为Android使用URI而不是文件路径
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将内容URI复制到临时文件，返回实际文件路径
     * 这样FFmpeg就可以直接访问文件了
     */
    private fun copyUriToTempFile(uri: Uri, fileName: String): String {
        try {
            // 创建临时文件目录
            val tempDir = File(context.cacheDir, "videos")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            // 创建临时文件
            val tempFile = File(tempDir, "temp_${System.currentTimeMillis()}_$fileName")

            // 复制内容URI到临时文件
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            android.util.Log.d("AndroidFileService", "已复制文件到: ${tempFile.absolutePath}")
            return tempFile.absolutePath

        } catch (e: Exception) {
            android.util.Log.e("AndroidFileService", "复制文件失败: ${e.message}")
            // 如果复制失败，尝试获取真实路径
            return getRealPathFromURI(uri) ?: uri.toString()
        }
    }

    /**
     * 尝试从内容URI获取真实文件路径
     */
    private fun getRealPathFromURI(uri: Uri): String? {
        return try {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    when {
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]

                            if ("primary".equals(type, ignoreCase = true)) {
                                "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
                            } else {
                                null
                            }
                        }
                        isMediaDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]

                            val contentUri = when (type) {
                                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }

                            contentUri?.let {
                                getDataColumn(it, "_id=?", arrayOf(split[1]))
                            }
                        }
                        else -> null
                    }
                }
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    getDataColumn(uri, null, null)
                }
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    uri.path
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("AndroidFileService", "获取真实路径失败: ${e.message}")
            null
        }
    }

    private fun getDataColumn(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        return try {
            val column = "_data"
            val projection = arrayOf(column)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    cursor.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}
