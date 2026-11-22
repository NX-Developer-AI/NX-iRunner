package com.nxteam.irunner.emulator

import android.app.ActivityManager
import android.content.Context
import android.os.Process

object EmulatorLauncher {
    fun launchApp(context: Context, ipaPath: String, options: LaunchOptions) {
        launch(context, listOf(ipaPath) + options.toArguments())
    }

    fun launchCorePicker(context: Context) {
        launch(context, emptyList())
    }

    private fun launch(context: Context, arguments: List<String>) {
        stopRunningEmulator(context)
        context.startActivity(EmulatorActivity.createIntent(context, arguments))
    }

    fun stopRunningEmulator(context: Context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val emulatorProcessName = context.packageName + EmulatorActivity.PROCESS_SUFFIX
        activityManager.runningAppProcesses
            ?.filter { processInfo -> processInfo.processName == emulatorProcessName }
            ?.forEach { processInfo -> Process.killProcess(processInfo.pid) }
    }
}
