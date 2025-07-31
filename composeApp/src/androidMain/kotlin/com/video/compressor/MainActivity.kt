package com.video.compressor

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.video.compressor.data.service.AndroidFileService
import com.video.compressor.di.appModule
import com.video.compressor.utils.PermissionManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val fileService: AndroidFileService by inject()

    // 权限请求启动器
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "权限已授予，可以开始使用应用", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要存储权限才能正常使用应用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 初始化Android文件服务
        fileService.initialize(this)

        // 检查并请求权限
        checkAndRequestPermissions()

        setContent {
            App()
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionManager.hasAllRequiredPermissions(this)) {
            val permissions = PermissionManager.getRequiredPermissions()
            permissionLauncher.launch(permissions)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}