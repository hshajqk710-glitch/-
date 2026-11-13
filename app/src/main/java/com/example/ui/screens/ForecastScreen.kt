package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.NearbyRegionWeather
import com.example.data.model.RegionLocation
import com.example.data.model.WeatherForecast
import com.example.data.model.WeatherInterpreter
import com.example.data.model.WeatherUnits
import com.example.ui.components.AirQualityCard
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.SunPathArcCard
import com.example.ui.components.WeatherGlassCard
import com.example.ui.components.WeeklyRangeBar
import com.example.ui.components.WindCompassCard
import com.example.ui.state.AtmosphereAppState
import com.example.ui.state.ForecastUiState
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    state: AtmosphereAppState,
    onRefresh: () -> Unit,
    onRequestGps: () -> Unit,
    onOpenRegions: () -> Unit,
    onSelectNearbyPoint: (NearbyRegionWeather) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRefreshing = (state.forecastState as? ForecastUiState.Success)?.isRefreshing == true

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "top_bar") {
                    ForecastTopBar(
                        region = state.activeRegion,
                        isDetectingLocation = state.isDetectingLocation,
                        onOpenRegions = onOpenRegions,
                        onRequestGps = onRequestGps,
                        onRefresh = onRefresh,
                    )
                }

                if (state.statusBannerMessage != null) {
                    item(key = "status_banner") {
                        Surface(
                            onClick = onDismissBanner,
                            shape = RoundedCornerShape(16.dp),
                            color = SolarAmber.copy(alpha = 0.22f),
                        ) {
                            Text(
                                text = state.statusBannerMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                when (val forecastState = state.forecastState) {
                    is ForecastUiState.Loading -> {
                        item(key = "loading") {
                            ForecastLoadingPlaceholder()
                        }
                    }

                    is ForecastUiState.Error -> {
                        item(key = "error") {
                            ForecastErrorCard(
                                message = forecastState.message,
                                onRetry = onRefresh,
                                onChooseRegion = onOpenRegions,
                            )
                        }
                    }

                    is ForecastUiState.Success -> {
                        val forecast = forecastState.forecast
                        item(key = "hero") {
                            HeroWeatherSection(
                                forecast = forecast,
                                units = state.units,
                                isFromCache = forecastState.isFromCache,
                            )
                        }

                        if (forecast.nearbyPoints.isNotEmpty()) {
                            item(key = "nearby_region") {
                                NearbyRegionStrip(
                                    points = forecast.nearbyPoints,
                                    units = state.units,
                                    onSelectPoint = onSelectNearbyPoint,
                                )
                            }
                        }

                        item(key = "hourly") {
                            HourlyForecastSection(
                                hourly = forecast.hourly,
                                units = state.units,
                            )
                        }

                        if (forecast.daily.isNotEmpty()) {
                            item(key = "daily") {
                                SevenDayForecastCard(
                                    forecast = forecast,
                                    units = state.units,
                                )
                            }
                        }

                        item(key = "sun_and_wind") {
                            val today = forecast.daily.firstOrNull()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SunPathArcCard(
                                    sunriseIso = today?.sunriseIso ?: "",
                                    sunsetIso = today?.sunsetIso ?: "",
                                    modifier = Modifier.weight(1f),
                                )
                                WindCompassCard(
                                    windSpeedKmh = forecast.current.windSpeedKmh,
                                    windGustsKmh = forecast.current.windGustsKmh,
                                    windDirectionDeg = forecast.current.windDirectionDeg,
                                    units = state.units,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (forecast.airQuality != null) {
                            item(key = "air_quality") {
                                AirQualityCard(airQuality = forecast.airQuality)
                            }
                        }

                        item(key = "bento_metrics_row_1") {
                            val cur = forecast.current
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SmallMetricTile(
                                    title = stringResource(R.string.metric_uv),
                                    icon = Icons.Rounded.WbSunny,
                                    value = cur.uvIndex.roundToInt().toString(),
                                    subtitle = WeatherInterpreter.uvLevelRu(cur.uvIndex),
                                    accent = SolarGold,
                                    modifier = Modifier.weight(1f),
                                )
                                SmallMetricTile(
                                    title = stringResource(R.string.metric_humidity),
                                    icon = Icons.Rounded.WaterDrop,
                                    value = "${cur.humidityPercent}%",
                                    subtitle = if (cur.precipitationMm > 0.0) {
                                        "Осадки: ${String.format(Locale("ru"), "%.1f", cur.precipitationMm)} мм"
                                    } else {
                                        "Без осадков сейчас"
                                    },
                                    accent = CoolCyan,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        item(key = "bento_metrics_row_2") {
                            val cur = forecast.current
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SmallMetricTile(
                                    title = stringResource(R.string.metric_pressure),
                                    icon = Icons.Rounded.Compress,
                                    value = state.units.formatPressure(cur.pressureHpa),
                                    subtitle = when {
                                        cur.pressureHpa < 1005 -> "Пониженное"
                                        cur.pressureHpa > 1022 -> "Повышенное"
                                        else -> "В пределах нормы"
                                    },
                                    accent = TextPrimaryLight,
                                    modifier = Modifier.weight(1f),
                                )
                                SmallMetricTile(
                                    title = stringResource(R.string.metric_clouds),
                                    icon = Icons.Rounded.CloudQueue,
                                    value = "${cur.cloudCoverPercent}%",
                                    subtitle = "Видимость ${cur.visibilityKm.roundToInt()} км",
                                    accent = TextSecondaryLight,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastTopBar(
    region: RegionLocation,
    isDetectingLocation: Boolean,
    onOpenRegions: () -> Unit,
    onRequestGps: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onOpenRegions,
            shape = CircleShape,
            color = GlassSurfaceElevated,
            modifier = Modifier
                .weight(1f, fill = false)
                .minimumInteractiveComponentSize()
                .testTag("region_picker_button"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = if (region.isCurrentGps) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                    contentDescription = stringResource(R.string.action_select_region),
                    tint = if (region.isCurrentGps) CoolCyan else SolarGold,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = region.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (region.subtitle.isNotBlank()) {
                        Text(
                            text = region.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondaryLight,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onRequestGps,
                enabled = !isDetectingLocation,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .minimumInteractiveComponentSize()
                    .testTag("gps_detect_button"),
            ) {
                if (isDetectingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CoolCyan,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = stringResource(R.string.action_my_location),
                        tint = TextPrimaryLight,
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .minimumInteractiveComponentSize()
                    .testTag("refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                    tint = TextPrimaryLight,
                )
            }
        }
    }
}

@Composable
private fun HeroWeatherSection(
    forecast: WeatherForecast,
    units: WeatherUnits,
    isFromCache: Boolean,
) {
    val cur = forecast.current
    val visual = WeatherInterpreter.interpret(cur.weatherCode, cur.isDay)
    val today = forecast.daily.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(visible = isFromCache) {
            Surface(
                shape = CircleShape,
                color = SolarAmber.copy(alpha = 0.25f),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.offline_cache_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.titleRu,
                tint = visual.tint,
                modifier = Modifier.size(60.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = units.formatTemp(cur.temperatureC),
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimaryLight,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = visual.titleRu,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimaryLight,
        )

        Spacer(modifier = Modifier.height(4.dp))

        val highLowText = if (today != null) {
            stringResource(
                R.string.high_low,
                units.formatTemp(today.tempMaxC),
                units.formatTemp(today.tempMinC),
            ) + " · "
        } else ""

        Text(
            text = highLowText + stringResource(
                R.string.feels_like,
                units.formatTemp(cur.apparentTemperatureC),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryLight,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.updated_at, forecast.updatedTimeLabel),
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedLight,
        )
    }
}

@Composable
private fun NearbyRegionStrip(
    points: List<NearbyRegionWeather>,
    units: WeatherUnits,
    onSelectPoint: (NearbyRegionWeather) -> Unit,
) {
    WeatherGlassCard(
        title = stringResource(R.string.section_nearby_region),
        titleIcon = Icons.Rounded.Explore,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(points, key = { it.directionLabel }) { pt ->
                val visual = WeatherInterpreter.interpret(pt.weatherCode, pt.isDay)
                Column(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onSelectPoint(pt) }
                        .padding(12.dp)
                        .testTag("nearby_point_${pt.directionLabel}"),
                ) {
                    Text(
                        text = pt.directionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolCyan,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pt.areaName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = visual.titleRu,
                            tint = visual.tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = units.formatTemp(pt.temperatureC),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SevenDayForecastCard(
    forecast: WeatherForecast,
    units: WeatherUnits,
) {
    val weekMin = forecast.daily.minOfOrNull { it.tempMinC } ?: 0.0
    val weekMax = forecast.daily.maxOfOrNull { it.tempMaxC } ?: 1.0

    WeatherGlassCard(
        title = stringResource(R.string.section_daily),
        titleIcon = Icons.Rounded.CalendarMonth,
    ) {
        forecast.daily.forEachIndexed { idx, day ->
            val visual = WeatherInterpreter.interpret(day.weatherCode, isDay = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.width(104.dp)) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = day.shortDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedLight,
                    )
                }

                Row(
                    modifier = Modifier.width(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = visual.titleRu,
                        tint = visual.tint,
                        modifier = Modifier.size(22.dp),
                    )
                    if (day.precipitationProbabilityMax >= 15) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${day.precipitationProbabilityMax}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolCyan,
                        )
                    }
                }

                Text(
                    text = units.formatTemp(day.tempMinC),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight,
                    modifier = Modifier.width(38.dp),
                )

                WeeklyRangeBar(
                    dayMin = day.tempMinC,
                    dayMax = day.tempMaxC,
                    weekMin = weekMin,
                    weekMax = weekMax,
                    currentTemp = if (idx == 0) forecast.current.temperatureC else null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )

                Text(
                    text = units.formatTemp(day.tempMaxC),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryLight,
                    modifier = Modifier.width(38.dp),
                )
            }

            if (idx < forecast.daily.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }
    }
}

@Composable
private fun SmallMetricTile(
    title: String,
    icon: ImageVector,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    WeatherGlassCard(
        modifier = modifier,
        title = title,
        titleIcon = icon,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryLight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ForecastLoadingPlaceholder() {
    WeatherGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = SolarGold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Загружаем данные о погоде в регионе…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryLight,
            )
        }
    }
}

@Composable
private fun ForecastErrorCard(
    message: String,
    onRetry: () -> Unit,
    onChooseRegion: () -> Unit,
) {
    WeatherGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = null,
                tint = SolarAmber,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.error_loading_weather),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color(0xFF0A192F)),
                    modifier = Modifier.testTag("retry_forecast_button"),
                ) {
                    Text(stringResource(R.string.retry))
                }
                Button(
                    onClick = onChooseRegion,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceElevated, contentColor = TextPrimaryLight),
                ) {
                    Text(stringResource(R.string.action_select_region))
                }
            }
        }
    }
}
