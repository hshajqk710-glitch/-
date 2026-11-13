package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PressureUnit
import com.example.data.model.TempUnit
import com.example.data.model.WeatherUnits
import com.example.data.model.WindUnit
import com.example.ui.components.WeatherGlassCard
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    units: WeatherUnits,
    onTempUnitChange: (TempUnit) -> Unit,
    onWindUnitChange: (WindUnit) -> Unit,
    onPressureUnitChange: (PressureUnit) -> Unit,
    onAutoDetectChange: (Boolean) -> Unit,
    onDetectNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "settings_title") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            item(key = "units_card") {
                WeatherGlassCard(
                    title = stringResource(R.string.settings_units),
                    titleIcon = Icons.Rounded.Tune,
                ) {
                    Text(
                        text = stringResource(R.string.unit_temperature),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TempUnit.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = units.tempUnit == unit,
                                onClick = { onTempUnitChange(unit) },
                                shape = SegmentedButtonDefaults.itemShape(index, TempUnit.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = SolarGold,
                                    activeContentColor = Color(0xFF0A192F),
                                    inactiveContainerColor = GlassSurfaceElevated,
                                    inactiveContentColor = TextPrimaryLight,
                                ),
                                modifier = Modifier.testTag("temp_unit_${unit.name}"),
                            ) {
                                Text(unit.labelRu)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.unit_wind),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        WindUnit.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = units.windUnit == unit,
                                onClick = { onWindUnitChange(unit) },
                                shape = SegmentedButtonDefaults.itemShape(index, WindUnit.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = SolarGold,
                                    activeContentColor = Color(0xFF0A192F),
                                    inactiveContainerColor = GlassSurfaceElevated,
                                    inactiveContentColor = TextPrimaryLight,
                                ),
                                modifier = Modifier.testTag("wind_unit_${unit.name}"),
                            ) {
                                Text(unit.labelRu)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.unit_pressure),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PressureUnit.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = units.pressureUnit == unit,
                                onClick = { onPressureUnitChange(unit) },
                                shape = SegmentedButtonDefaults.itemShape(index, PressureUnit.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = SolarGold,
                                    activeContentColor = Color(0xFF0A192F),
                                    inactiveContainerColor = GlassSurfaceElevated,
                                    inactiveContentColor = TextPrimaryLight,
                                ),
                                modifier = Modifier.testTag("pressure_unit_${unit.name}"),
                            ) {
                                Text(unit.labelRu)
                            }
                        }
                    }
                }
            }

            item(key = "location_card") {
                WeatherGlassCard(
                    title = stringResource(R.string.settings_location),
                    titleIcon = Icons.Rounded.MyLocation,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.auto_detect_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryLight,
                            )
                            Text(
                                text = stringResource(R.string.auto_detect_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                            )
                        }
                        Switch(
                            checked = units.autoDetectLocationOnStart,
                            onCheckedChange = onAutoDetectChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0A192F),
                                checkedTrackColor = CoolCyan,
                            ),
                            modifier = Modifier.testTag("auto_detect_switch"),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDetectNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoolCyan.copy(alpha = 0.24f),
                            contentColor = TextPrimaryLight,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_detect_gps_button"),
                    ) {
                        Text(stringResource(R.string.use_current_gps))
                    }
                }
            }

            item(key = "about_card") {
                WeatherGlassCard(
                    title = stringResource(R.string.settings_about),
                    titleIcon = Icons.Rounded.Info,
                ) {
                    Text(
                        text = stringResource(R.string.about_source_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.about_source_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                    )
                }
            }
        }
    }
}
