package rs.ruffle

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val TAG = "RuffleDebug"
    private const val FILE_NAME = "ruffle_crash_trace.log"

    fun log(context: Context?, message: String) {
        // 1. 输出到 Logcat
        Log.d(TAG, message)

        // 2. 追加到文件 (如果你传了 context)
        if (context != null) {
            try {
                // 路径: /sdcard/Android/data/rs.ruffle/files/ruffle_crash_trace.log
                val file = File(context.getExternalFilesDir(null), FILE_NAME)
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
                
                // 追加模式写入
                val writer = FileWriter(file, true)
                writer.append("[$timestamp] $message\n")
                writer.close()
            } catch (e: Exception) {
                // 忽略文件写入错误，避免死循环
            }
        }
    }

    // 每次启动 App 时调用，清空旧日志，避免文件无限大
    fun clear(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) { }
    }
}