package fr.solremi.minerspace.android.platform

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

internal class LocalCrashReporter private constructor(
    private val context: Context,
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val directory = context.filesDir.resolve("crash-reports").apply { mkdirs() }
            val report = directory.resolve("last-crash.txt")
            val writer = StringWriter()
            PrintWriter(writer).use { output ->
                output.println("timestamp=${Instant.now()}")
                output.println("thread=${thread.name.take(80)}")
                output.println("exception=${throwable::class.java.name}")
                throwable.stackTrace.take(80).forEach { frame ->
                    output.println("at=${frame.className}.${frame.methodName}:${frame.lineNumber}")
                }
                throwable.cause?.let { cause ->
                    output.println("cause=${cause::class.java.name}")
                    cause.stackTrace.take(30).forEach { frame ->
                        output.println("causeAt=${frame.className}.${frame.methodName}:${frame.lineNumber}")
                    }
                }
            }
            report.writeText(writer.toString(), Charsets.UTF_8)
        }
        delegate?.uncaughtException(thread, throwable)
    }

    companion object {
        private val installed = AtomicBoolean(false)
        fun install(context: Context) {
            if (!installed.compareAndSet(false, true)) return
            val current = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(LocalCrashReporter(context.applicationContext, current))
        }
    }
}
