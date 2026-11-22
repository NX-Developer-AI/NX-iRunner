package com.nxteam.irunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nxteam.irunner.R
import com.nxteam.irunner.ipa.CompatibilityLevel
import com.nxteam.irunner.ipa.CompatibilityReason

fun CompatibilityLevel.color(): Color = when (this) {
    CompatibilityLevel.LIKELY -> Color(0xFF2E9E5B)
    CompatibilityLevel.PARTIAL -> Color(0xFFD99A1E)
    CompatibilityLevel.UNLIKELY -> Color(0xFFE0682B)
    CompatibilityLevel.INCOMPATIBLE -> Color(0xFFD23C3C)
}

fun CompatibilityLevel.labelResource(): Int = when (this) {
    CompatibilityLevel.LIKELY -> R.string.compatibility_likely
    CompatibilityLevel.PARTIAL -> R.string.compatibility_partial
    CompatibilityLevel.UNLIKELY -> R.string.compatibility_unlikely
    CompatibilityLevel.INCOMPATIBLE -> R.string.compatibility_incompatible
}

fun CompatibilityReason.messageResource(): Int = when (this) {
    CompatibilityReason.CLASSIC_IPHONE_OS -> R.string.reason_classic
    CompatibilityReason.IOS_FOUR -> R.string.reason_ios_four
    CompatibilityReason.MODERN_IOS -> R.string.reason_modern
    CompatibilityReason.ARM64_ONLY -> R.string.reason_arm64
    CompatibilityReason.ENCRYPTED -> R.string.reason_encrypted
    CompatibilityReason.UNKNOWN_EXECUTABLE -> R.string.reason_unknown
}

@Composable
fun CompatibilityChip(level: CompatibilityLevel, modifier: Modifier = Modifier) {
    val color = level.color()
    Text(
        text = stringResource(level.labelResource()),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
