package dev.legendsayantan.extendroid

import android.app.Application
import dev.legendsayantan.extendroid.lib.Logging

/**
 * Stores uncaught exceptions in the in-app log before the process dies, so a crash report can
 * be copied from the Logs screen without a computer attached.
 */
class ExtendroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Logging(this).saveCrash(thread, throwable)
            } catch (_: Throwable) {
                // Never let logging replace the original crash.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
