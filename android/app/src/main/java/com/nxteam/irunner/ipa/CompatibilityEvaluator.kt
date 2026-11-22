package com.nxteam.irunner.ipa

enum class CompatibilityLevel {
    LIKELY,
    PARTIAL,
    UNLIKELY,
    INCOMPATIBLE,
}

enum class CompatibilityReason {
    CLASSIC_IPHONE_OS,
    IOS_FOUR,
    MODERN_IOS,
    ARM64_ONLY,
    ENCRYPTED,
    UNKNOWN_EXECUTABLE,
}

data class CompatibilityReport(
    val level: CompatibilityLevel,
    val reasons: List<CompatibilityReason>,
)

object CompatibilityEvaluator {
    fun evaluate(metadata: IpaMetadata): CompatibilityReport =
        evaluate(metadata.machO, metadata.minimumOsVersion)

    fun evaluate(machO: MachOSummary?, minimumOsVersion: String?): CompatibilityReport {
        val reasons = mutableListOf<CompatibilityReason>()
        if (machO == null) {
            return CompatibilityReport(CompatibilityLevel.UNLIKELY, listOf(CompatibilityReason.UNKNOWN_EXECUTABLE))
        }
        if (!machO.has32BitArmSlice) {
            return CompatibilityReport(CompatibilityLevel.INCOMPATIBLE, listOf(CompatibilityReason.ARM64_ONLY))
        }
        if (!machO.hasRunnable32BitArmSlice) {
            return CompatibilityReport(CompatibilityLevel.INCOMPATIBLE, listOf(CompatibilityReason.ENCRYPTED))
        }
        val version = parseVersion(minimumOsVersion)
        val level = when {
            version == null || version.first < 4 -> {
                reasons += CompatibilityReason.CLASSIC_IPHONE_OS
                CompatibilityLevel.LIKELY
            }
            version.first == 4 && version.second == 0 -> {
                reasons += CompatibilityReason.IOS_FOUR
                CompatibilityLevel.LIKELY
            }
            version.first == 4 -> {
                reasons += CompatibilityReason.IOS_FOUR
                CompatibilityLevel.PARTIAL
            }
            else -> {
                reasons += CompatibilityReason.MODERN_IOS
                CompatibilityLevel.UNLIKELY
            }
        }
        return CompatibilityReport(level, reasons)
    }

    private fun parseVersion(value: String?): Pair<Int, Int>? {
        if (value.isNullOrBlank()) {
            return null
        }
        val parts = value.trim().split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return major to minor
    }
}
