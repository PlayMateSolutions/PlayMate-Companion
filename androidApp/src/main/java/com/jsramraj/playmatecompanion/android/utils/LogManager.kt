package com.jsramraj.playmatecompanion.android.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LogManager private constructor(private val context: Context) {
    private val logFile = File(context.filesDir, "app.log")
    private val maxLogSize = 500 * 1024 // 500KB
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private val _logs = MutableStateFlow<String>("")
    val logs: StateFlow<String> = _logs

    companion object {
        @Volatile
        private var instance: LogManager? = null
        
        fun getInstance(context: Context): LogManager {
            return instance ?: synchronized(this) {
                instance ?: LogManager(context).also { instance = it }
            }
        }
    }

    private fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] ${level.name} - $tag: $message\n"
        
        try {
            if (logFile.length() > maxLogSize) {
                clearLogs()
            }
            
            logFile.appendText(logMessage)
            _logs.value = logFile.readText()
            
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARNING -> Log.w(tag, message)
                LogLevel.ERROR -> Log.e(tag, message)
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write log: ${e.message}")
        }
    }

    fun d(tag: String, message: String) = log(tag, message, LogLevel.DEBUG)
    fun i(tag: String, message: String) = log(tag, message, LogLevel.INFO)
    fun w(tag: String, message: String) = log(tag, message, LogLevel.WARNING)
    fun e(tag: String, message: String) = log(tag, message, LogLevel.ERROR)

    fun getLogs(): String {
        return try {
            logFile.readText()
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    fun clearLogs() {
        try {
            logFile.writeText("")
            _logs.value = ""
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to clear logs: ${e.message}")
        }
    }

    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}
