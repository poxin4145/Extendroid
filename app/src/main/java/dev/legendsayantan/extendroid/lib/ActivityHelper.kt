package dev.legendsayantan.extendroid.lib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import java.lang.reflect.Method


/**
 * @author legendsayantan
 */
class ActivityHelper {
    companion object {
        var SHELL_PACKAGE: String = "com.android.shell"

        fun getLauncherActivityComponentName(
            context: Context,
            packageName: String
        ): ComponentName? {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.setPackage(packageName)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            if (resolveInfos.isNotEmpty()) {
                val info = resolveInfos[0].activityInfo
                return ComponentName(info.packageName, info.name)
            }
            return null
        }

        /**
         * Starts [component] on [displayId].
         *
         * `am start --display` is used because it builds the ActivityOptions exactly like the
         * shell does; the old IActivityManager reflection silently dropped them on Android 14+
         * and the activity ended up on display 0. When the app already has a task elsewhere the
         * start is just ignored, so it is restarted with `-S` in that case.
         */
        fun launchActivityOnDisplayID(component: ComponentName, displayId: Int): String {
            var output = amStart(component, displayId, false)
            if (!output.contains("Starting:") || output.contains("Activity not started")) {
                output = amStart(component, displayId, true)
            }
            return if (output.contains("Starting:")) {
                "Launched on display $displayId"
            } else {
                "Error: " + output.trim().take(300)
            }
        }

        private fun amStart(component: ComponentName, displayId: Int, forceStop: Boolean): String {
            return try {
                val args = mutableListOf("am", "start", "--display", displayId.toString())
                if (forceStop) args.add("-S")
                args.add("-n")
                args.add(component.flattenToString())
                val process = Runtime.getRuntime().exec(args.toTypedArray())
                val out = process.inputStream.bufferedReader().use { it.readText() }
                val err = process.errorStream.bufferedReader().use { it.readText() }
                process.waitFor()
                out + err
            } catch (t: Throwable) {
                "Error: ${t.message}"
            }
        }

        @SuppressLint("PrivateApi")
        fun removePackageTask(context: Context, pkg: String?) {
            // 1) Reflectively get the ActivityTaskManager service binder
            val sm = Class.forName("android.os.ServiceManager")
            val getService = sm.getMethod("getService", String::class.java)
            val b = getService.invoke(null, "activity_task") as IBinder?

            // 2) Get IActivityTaskManager
            val stub = Class.forName("android.app.IActivityTaskManager\$Stub")
            val asInterface = stub.getMethod("asInterface", IBinder::class.java)
            val atm = asInterface.invoke(null, b)

            // 3) Enumerate recent tasks to find task IDs for 'pkg'
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val recent = am.getRecentTasks(
                100,
                ActivityManager.RECENT_WITH_EXCLUDED
            )

            // 4) For each matching task, call removeTask(taskId)
            val removeTask = atm.javaClass.getMethod("removeTask", Int::class.javaPrimitiveType)
            for (info in recent) {
                if (info.baseIntent.component!!.packageName == pkg) {
                    removeTask.invoke(atm, info.taskId)
                }
            }
        }


    }

}