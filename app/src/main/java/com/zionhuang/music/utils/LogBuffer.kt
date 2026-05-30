package com.zionhuang.music.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object LogBuffer {
    private val lock = ReentrantLock()
    private val buffer = ArrayDeque<String>()
    private const val MAX_LINES = 2000
    private const val LIVE_LOG_FILE_NAME = "current.log"

    fun append(tag: String, level: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val header = "$timestamp [$level/$tag] $message"
        lock.withLock {
            buffer.addLast(header)
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                buffer.addLast(sw.toString())
            }
            while (buffer.size > MAX_LINES) buffer.removeFirst()
        }
    }

    fun appendToLiveFile(context: Context, tag: String, level: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val header = "$timestamp [$level/$tag] $message"
        val dir = File(context.cacheDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, LIVE_LOG_FILE_NAME)

        lock.withLock {
            buffer.addLast(header)
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                buffer.addLast(sw.toString())
            }
            while (buffer.size > MAX_LINES) buffer.removeFirst()

            file.appendText(header + System.lineSeparator())
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                file.appendText(sw.toString())
                if (!sw.toString().endsWith(System.lineSeparator())) {
                    file.appendText(System.lineSeparator())
                }
            }
        }
    }

    fun dumpToFile(context: Context): File {
        val dir = File(context.cacheDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "inner_tune_log_${System.currentTimeMillis()}.txt")
        lock.withLock {
            file.printWriter().use { pw ->
                pw.println("InnerTune Log export - ${Date()}")
                pw.println()
                buffer.forEach { pw.println(it) }
            }
        }
        return file
    }

    fun liveLogFile(context: Context): File {
        val dir = File(context.cacheDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, LIVE_LOG_FILE_NAME)
    }

    fun clear() {
        lock.withLock { buffer.clear() }
    }

    fun exportAndShare(context: Context) {
        val file = dumpToFile(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // startActivity requires an Activity context; caller should use Context.startActivity
        context.startActivity(Intent.createChooser(share, "Share logs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
