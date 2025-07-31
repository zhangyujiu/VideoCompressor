package com.video.compressor.data.local.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 内存数据库实现
 * 注意：这是临时实现，实际项目中应该使用持久化数据库
 */
class InMemoryDatabase {
    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    
    fun getAllTasks(): Flow<List<TaskEntity>> = _tasks
    
    fun getActiveTasks(): Flow<List<TaskEntity>> = _tasks.map { tasks ->
        tasks.filter { task ->
            task.status in listOf("PENDING", "RUNNING", "PAUSED")
        }
    }
    
    fun getTaskHistory(): Flow<List<TaskEntity>> = _tasks.map { tasks ->
        tasks.filter { task ->
            task.status in listOf("COMPLETED", "FAILED", "CANCELLED")
        }.sortedByDescending { it.createdAt }
    }
    
    suspend fun insertTask(task: TaskEntity) {
        val currentTasks = _tasks.value.toMutableList()
        currentTasks.add(task)
        _tasks.value = currentTasks
    }
    
    suspend fun updateTask(taskId: String, updater: (TaskEntity) -> TaskEntity) {
        val currentTasks = _tasks.value.toMutableList()
        val index = currentTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            currentTasks[index] = updater(currentTasks[index])
            _tasks.value = currentTasks
        }
    }
    
    suspend fun deleteTask(taskId: String) {
        val currentTasks = _tasks.value.toMutableList()
        currentTasks.removeAll { it.id == taskId }
        _tasks.value = currentTasks
    }
    
    suspend fun clearHistory() {
        val currentTasks = _tasks.value.toMutableList()
        currentTasks.removeAll { task ->
            task.status in listOf("COMPLETED", "FAILED", "CANCELLED")
        }
        _tasks.value = currentTasks
    }
    
    companion object {
        @Volatile
        private var INSTANCE: InMemoryDatabase? = null
        
        fun getInstance(): InMemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InMemoryDatabase().also { INSTANCE = it }
            }
        }
    }
}
