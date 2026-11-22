package com.nxteam.irunner.emulator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import org.libsdl.app.SDLActivity

class EmulatorActivity : SDLActivity() {
    private var launchArguments: Array<String> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        launchArguments = intent?.getStringArrayExtra(EXTRA_ARGUMENTS) ?: emptyArray()
        super.onCreate(savedInstanceState)
    }

    override fun getLibraries(): Array<String> = arrayOf("SDL2", "touchHLE")

    override fun getArguments(): Array<String> = launchArguments

    override fun onDestroy() {
        super.onDestroy()
        Process.killProcess(Process.myPid())
    }

    companion object {
        const val EXTRA_ARGUMENTS = "com.nxteam.irunner.extra.ARGUMENTS"
        const val PROCESS_SUFFIX = ":emulator"

        fun createIntent(context: Context, arguments: List<String>): Intent =
            Intent(context, EmulatorActivity::class.java)
                .putExtra(EXTRA_ARGUMENTS, arguments.toTypedArray())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
}
