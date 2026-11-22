package com.nxteam.irunner.emulator

import org.json.JSONObject

enum class StartOrientation(val argument: String?) {
    AUTOMATIC(null),
    PORTRAIT(null),
    UPSIDE_DOWN("--upside-down"),
    LANDSCAPE_LEFT("--landscape-left"),
    LANDSCAPE_RIGHT("--landscape-right"),
}

enum class DeviceFamilyOverride(val argument: String?) {
    AUTOMATIC(null),
    IPHONE("--device-family=iphone"),
    IPAD("--device-family=ipad"),
}

data class LaunchOptions(
    val orientation: StartOrientation = StartOrientation.AUTOMATIC,
    val deviceFamily: DeviceFamilyOverride = DeviceFamilyOverride.AUTOMATIC,
    val scaleHack: Int = 1,
    val forceComposition: Boolean = false,
    val allowNetworkAccess: Boolean = false,
    val unlimitedFrameRate: Boolean = false,
    val printFrameRate: Boolean = false,
    val extraArguments: String = "",
) {
    fun toArguments(): List<String> = buildList {
        orientation.argument?.let { add(it) }
        deviceFamily.argument?.let { add(it) }
        if (scaleHack > 1) {
            add("--scale-hack=$scaleHack")
        }
        if (forceComposition) {
            add("--force-composition")
        }
        if (allowNetworkAccess) {
            add("--allow-network-access")
        }
        if (unlimitedFrameRate) {
            add("--fps-limit=off")
        }
        if (printFrameRate) {
            add("--print-fps")
        }
        addAll(extraArguments.split(Regex("\\s+")).filter { argument -> argument.isNotBlank() })
    }

    fun toJson(): String = JSONObject()
        .put("orientation", orientation.name)
        .put("deviceFamily", deviceFamily.name)
        .put("scaleHack", scaleHack)
        .put("forceComposition", forceComposition)
        .put("allowNetworkAccess", allowNetworkAccess)
        .put("unlimitedFrameRate", unlimitedFrameRate)
        .put("printFrameRate", printFrameRate)
        .put("extraArguments", extraArguments)
        .toString()

    companion object {
        const val MAXIMUM_SCALE_HACK = 4

        fun fromJson(json: String?): LaunchOptions {
            if (json.isNullOrBlank()) {
                return LaunchOptions()
            }
            return runCatching {
                val objectValue = JSONObject(json)
                LaunchOptions(
                    orientation = enumValueOrDefault(objectValue.optString("orientation"), StartOrientation.AUTOMATIC),
                    deviceFamily = enumValueOrDefault(objectValue.optString("deviceFamily"), DeviceFamilyOverride.AUTOMATIC),
                    scaleHack = objectValue.optInt("scaleHack", 1).coerceIn(1, MAXIMUM_SCALE_HACK),
                    forceComposition = objectValue.optBoolean("forceComposition", false),
                    allowNetworkAccess = objectValue.optBoolean("allowNetworkAccess", false),
                    unlimitedFrameRate = objectValue.optBoolean("unlimitedFrameRate", false),
                    printFrameRate = objectValue.optBoolean("printFrameRate", false),
                    extraArguments = objectValue.optString("extraArguments", ""),
                )
            }.getOrDefault(LaunchOptions())
        }

        private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
            enumValues<T>().firstOrNull { value -> value.name == name } ?: default
    }
}
