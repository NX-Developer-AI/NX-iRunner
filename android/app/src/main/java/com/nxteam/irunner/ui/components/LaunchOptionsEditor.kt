package com.nxteam.irunner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nxteam.irunner.R
import com.nxteam.irunner.emulator.DeviceFamilyOverride
import com.nxteam.irunner.emulator.LaunchOptions
import com.nxteam.irunner.emulator.StartOrientation
import kotlin.math.roundToInt

@Composable
fun LaunchOptionsEditor(
    options: LaunchOptions,
    onOptionsChange: (LaunchOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SelectionRow(
            label = stringResource(R.string.options_orientation),
            values = StartOrientation.entries,
            selected = options.orientation,
            valueLabel = { value -> stringResource(value.labelResource()) },
            onSelected = { value -> onOptionsChange(options.copy(orientation = value)) },
        )
        SelectionRow(
            label = stringResource(R.string.options_device_family),
            values = DeviceFamilyOverride.entries,
            selected = options.deviceFamily,
            valueLabel = { value -> stringResource(value.labelResource()) },
            onSelected = { value -> onOptionsChange(options.copy(deviceFamily = value)) },
        )
        Text(
            text = stringResource(R.string.options_scale_hack, options.scaleHack),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Slider(
            value = options.scaleHack.toFloat(),
            onValueChange = { value -> onOptionsChange(options.copy(scaleHack = value.roundToInt())) },
            valueRange = 1f..LaunchOptions.MAXIMUM_SCALE_HACK.toFloat(),
            steps = LaunchOptions.MAXIMUM_SCALE_HACK - 2,
        )
        SwitchRow(
            title = stringResource(R.string.options_force_composition),
            summary = stringResource(R.string.options_force_composition_summary),
            checked = options.forceComposition,
            onCheckedChange = { checked -> onOptionsChange(options.copy(forceComposition = checked)) },
        )
        SwitchRow(
            title = stringResource(R.string.options_network),
            summary = stringResource(R.string.options_network_summary),
            checked = options.allowNetworkAccess,
            onCheckedChange = { checked -> onOptionsChange(options.copy(allowNetworkAccess = checked)) },
        )
        SwitchRow(
            title = stringResource(R.string.options_unlimited_fps),
            summary = null,
            checked = options.unlimitedFrameRate,
            onCheckedChange = { checked -> onOptionsChange(options.copy(unlimitedFrameRate = checked)) },
        )
        SwitchRow(
            title = stringResource(R.string.options_print_fps),
            summary = null,
            checked = options.printFrameRate,
            onCheckedChange = { checked -> onOptionsChange(options.copy(printFrameRate = checked)) },
        )
        OutlinedTextField(
            value = options.extraArguments,
            onValueChange = { value -> onOptionsChange(options.copy(extraArguments = value)) },
            label = { Text(stringResource(R.string.options_extra_arguments)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

private fun StartOrientation.labelResource(): Int = when (this) {
    StartOrientation.AUTOMATIC -> R.string.orientation_automatic
    StartOrientation.PORTRAIT -> R.string.orientation_portrait
    StartOrientation.UPSIDE_DOWN -> R.string.orientation_upside_down
    StartOrientation.LANDSCAPE_LEFT -> R.string.orientation_landscape_left
    StartOrientation.LANDSCAPE_RIGHT -> R.string.orientation_landscape_right
}

private fun DeviceFamilyOverride.labelResource(): Int = when (this) {
    DeviceFamilyOverride.AUTOMATIC -> R.string.device_family_automatic
    DeviceFamilyOverride.IPHONE -> R.string.device_iphone
    DeviceFamilyOverride.IPAD -> R.string.device_ipad
}

@Composable
private fun <T> SelectionRow(
    label: String,
    values: List<T>,
    selected: T,
    valueLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { expanded = true }) {
            Text(valueLabel(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(valueLabel(value)) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    },
                )
            }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
