package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AirQualityInfo
import com.example.data.model.HourlyForecast
import com.example.data.model.WeatherInterpreter
import com.example.data.model.WeatherUnits
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.MintGood
import com.example.ui.theme.ModerateYellow
import com.example.ui.theme.PoorOrange
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.ThermalCoral
import com.example.ui.theme.VeryPoorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HourlyForecastSection(
    hourly: List<HourlyForecast>,
    units: WeatherUnits,
    modifier: Modifier = Modifier,
) {
    if (hourly.isEmpty()) return

    var selectedIndex by remember(hourly) { mutableIntStateOf(0) }
    val selectedHour = hourly.getOrElse(selectedIndex) { hourly.first() }
    val selectedVisual = WeatherInterpreter.interpret(selectedHour.weatherCode, selectedHour.isDay)

    WeatherGlassCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.section_hourly),
        titleIcon = Icons.Rounded.Schedule,
    ) {
        // Active hour detail summary pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${selectedHour.hourLabel} · ${selectedVisual.titleRu}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryLight,
                )
                Text(
                    text = "Ветер ${units.formatWind(selectedHour.windSpeedKmh)} · Осадки ${selectedHour.precipitationProbability}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                )
            }
            Text(
                text = units.formatTemp(selectedHour.temperatureC),
                style = MaterialTheme.typography.headlineMedium,
                color = SolarGold,
            )
        }

        // Interactive scrollable temperature spline + hour chips
        val itemWidthDp = 64.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            val totalWidth = itemWidthDp * hourly.size
            val temps = hourly.map { it.temperatureC }
            val minT = temps.minOrNull() ?: 0.0
            val maxT = temps.maxOrNull() ?: 1.0
            val span = max(1.5, maxT - minT)

            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(86.dp),
            ) {
                val colWidth = size.width / hourly.size
                val topPad = 18.dp.toPx()
                val botPad = 14.dp.toPx()
                val usableH = size.height - topPad - botPad

                val points = hourly.mapIndexed { idx, item ->
                    val x = colWidth * idx + colWidth / 2f
                    val norm = ((item.temperatureC - minT) / span).toFloat()
                    val y = topPad + usableH * (1f - norm)
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val ctrlX = (p0.x + p1.x) / 2f
                            cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SolarGold.copy(alpha = 0.28f),
                                Color.Transparent,
                            ),
                        ),
                    )

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(CoolCyan, SolarGold, SolarAmber),
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Draw selected indicator and subtle dots
                points.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    if (isSelected) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(pt.x, pt.y),
                            end = Offset(pt.x, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                        drawCircle(
                            color = SolarGold,
                            radius = 6.dp.toPx(),
                            center = pt,
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = pt,
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.65f),
                            radius = 2.5.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }

            Row(modifier = Modifier.width(totalWidth)) {
                hourly.forEachIndexed { idx, item ->
                    val isSelected = idx == selectedIndex
                    val visual = WeatherInterpreter.interpret(item.weatherCode, item.isDay)
                    Column(
                        modifier = Modifier
                            .width(itemWidthDp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.16f)
                                else Color.Transparent,
                            )
                            .clickable { selectedIndex = idx }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("hourly_item_$idx"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = item.hourLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TextPrimaryLight else TextSecondaryLight,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = visual.titleRu,
                            tint = visual.tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = units.formatTemp(item.temperatureC),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryLight,
                        )
                        Text(
                            text = if (item.precipitationProbability >= 10) "${item.precipitationProbability}%" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolCyan,
                            modifier = Modifier.height(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyRangeBar(
    dayMin: Double,
    dayMax: Double,
    weekMin: Double,
    weekMax: Double,
    currentTemp: Double? = null,
    modifier: Modifier = Modifier,
) {
    val totalSpan = max(1.0, weekMax - weekMin)
    val startFrac = ((dayMin - weekMin) / totalSpan).toFloat().coerceIn(0f, 1f)
    val endFrac = ((dayMax - weekMin) / totalSpan).toFloat().coerceIn(startFrac + 0.06f, 1f)

    Canvas(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth(),
    ) {
        val barHeight = size.height
        val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

        // Track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = corner,
        )

        val leftX = startFrac * size.width
        val rightX = endFrac * size.width
        val barWidth = max(barHeight, rightX - leftX)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(CoolCyan, SolarGold, ThermalCoral),
                startX = 0f,
                endX = size.width,
            ),
            topLeft = Offset(leftX, 0f),
            size = Size(barWidth, barHeight),
            cornerRadius = corner,
        )

        if (currentTemp != null) {
            val dotFrac = ((currentTemp - weekMin) / totalSpan).toFloat().coerceIn(startFrac, endFrac)
            val dotX = dotFrac * size.width
            drawCircle(
                color = Color(0xFF0A192F),
                radius = 5.dp.toPx(),
                center = Offset(dotX, barHeight / 2f),
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(dotX, barHeight / 2f),
            )
        }
    }
}

@Composable
fun SunPathArcCard(
    sunriseIso: String,
    sunsetIso: String,
    modifier: Modifier = Modifier,
) {
    val sunriseTime = sunriseIso.substringAfter("T", "07:00").take(5)
    val sunsetTime = sunsetIso.substringAfter("T", "18:00").take(5)

    val dayProgress = remember(sunriseIso, sunsetIso) {
        computeDaylightProgress(sunriseIso, sunsetIso)
    }

    WeatherGlassCard(
        modifier = modifier,
        title = stringResource(R.string.metric_sun),
        titleIcon = Icons.Rounded.WbTwilight,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
        ) {
            val horizY = size.height * 0.78f
            val leftX = 12.dp.toPx()
            val rightX = size.width - 12.dp.toPx()
            val arcWidth = rightX - leftX
            val arcHeight = size.height * 0.62f

            // Horizon line
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(0f, horizY),
                end = Offset(size.width, horizY),
                strokeWidth = 1.dp.toPx(),
            )

            // Dashed arc path
            val steps = 40
            val arcPath = Path()
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = leftX + arcWidth * t
                val y = horizY - sin(t * PI.toFloat()) * arcHeight
                if (i == 0) arcPath.moveTo(x, y) else arcPath.lineTo(x, y)
            }
            drawPath(
                path = arcPath,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    cap = StrokeCap.Round,
                ),
            )

            // Lit progress arc
            if (dayProgress > 0f) {
                val litPath = Path()
                val litSteps = (steps * dayProgress).roundToInt().coerceAtLeast(1)
                for (i in 0..litSteps) {
                    val t = (i / steps.toFloat()).coerceAtMost(dayProgress)
                    val x = leftX + arcWidth * t
                    val y = horizY - sin(t * PI.toFloat()) * arcHeight
                    if (i == 0) litPath.moveTo(x, y) else litPath.lineTo(x, y)
                }
                drawPath(
                    path = litPath,
                    brush = Brush.horizontalGradient(listOf(SolarAmber, SolarGold)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Current sun position marker
            val clamped = dayProgress.coerceIn(0f, 1f)
            val sunX = leftX + arcWidth * clamped
            val sunY = horizY - sin(clamped * PI.toFloat()) * arcHeight

            drawCircle(
                color = SolarGold.copy(alpha = 0.30f),
                radius = 12.dp.toPx(),
                center = Offset(sunX, sunY),
            )
            drawCircle(
                color = SolarGold,
                radius = 6.dp.toPx(),
                center = Offset(sunX, sunY),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.metric_sunrise),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedLight,
                )
                Text(
                    text = sunriseTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryLight,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.metric_sunset),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedLight,
                )
                Text(
                    text = sunsetTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryLight,
                )
            }
        }
    }
}

@Composable
fun WindCompassCard(
    windSpeedKmh: Double,
    windGustsKmh: Double,
    windDirectionDeg: Int,
    units: WeatherUnits,
    modifier: Modifier = Modifier,
) {
    val cardinal = WeatherInterpreter.windCardinalRu(windDirectionDeg)

    WeatherGlassCard(
        modifier = modifier,
        title = stringResource(R.string.metric_wind),
        titleIcon = Icons.Rounded.Air,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = units.formatWind(windSpeedKmh),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryLight,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Направление: $cardinal ($windDirectionDeg°)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                )
                if (windGustsKmh > windSpeedKmh + 2.0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.metric_gusts, units.formatWind(windGustsKmh)),
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolCyan,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Canvas(modifier = Modifier.size(84.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 4.dp.toPx()

                // Outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Cardinal ticks
                for (deg in 0 until 360 step 30) {
                    val rad = Math.toRadians(deg.toDouble())
                    val isCardinal = deg % 90 == 0
                    val innerR = radius - if (isCardinal) 7.dp.toPx() else 4.dp.toPx()
                    drawLine(
                        color = if (isCardinal) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.28f),
                        start = Offset(
                            center.x + (innerR * sin(rad)).toFloat(),
                            center.y - (innerR * cos(rad)).toFloat(),
                        ),
                        end = Offset(
                            center.x + (radius * sin(rad)).toFloat(),
                            center.y - (radius * cos(rad)).toFloat(),
                        ),
                        strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Direction arrow pointing in wind direction
                val angleRad = Math.toRadians(windDirectionDeg.toDouble())
                val tipDist = radius * 0.68f
                val tailDist = radius * 0.45f
                val tip = Offset(
                    center.x + (tipDist * sin(angleRad)).toFloat(),
                    center.y - (tipDist * cos(angleRad)).toFloat(),
                )
                val tail = Offset(
                    center.x - (tailDist * sin(angleRad)).toFloat(),
                    center.y + (tailDist * cos(angleRad)).toFloat(),
                )

                drawLine(
                    color = CoolCyan,
                    start = tail,
                    end = tip,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = SolarGold,
                    radius = 4.5.dp.toPx(),
                    center = tip,
                )

                // Cardinal labels via nativeCanvas for crisp typography
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 235, 245, 255)
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.apply {
                    drawText("С", center.x, center.y - radius + 16.dp.toPx(), paint)
                    drawText("Ю", center.x, center.y + radius - 9.dp.toPx(), paint)
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(
    airQuality: AirQualityInfo,
    modifier: Modifier = Modifier,
) {
    val (label, statusColor) = WeatherInterpreter.aqiDescriptionRu(airQuality.europeanAqi)
    val fraction = (airQuality.europeanAqi / 100f).coerceIn(0.03f, 0.97f)

    WeatherGlassCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.metric_aqi),
        titleIcon = Icons.Rounded.Spa,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "${airQuality.europeanAqi} · $label",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryLight,
                )
                Text(
                    text = "Европейский индекс качества воздуха (EAQI)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spectrum Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            val barH = 6.dp.toPx()
            val yTop = (size.height - barH) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(MintGood, ModerateYellow, PoorOrange, VeryPoorRed),
                ),
                topLeft = Offset(0f, yTop),
                size = Size(size.width, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f),
            )
            val markerX = fraction * size.width
            drawCircle(
                color = Color(0xFF0A192F),
                radius = 6.dp.toPx(),
                center = Offset(markerX, size.height / 2f),
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(markerX, size.height / 2f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PollutantChip("PM2.5", "${airQuality.pm25.roundToInt()} мкг/м³", Modifier.weight(1f))
            PollutantChip("PM10", "${airQuality.pm10.roundToInt()} мкг/м³", Modifier.weight(1f))
            PollutantChip("O₃", "${airQuality.ozone.roundToInt()} мкг/м³", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PollutantChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedLight,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimaryLight,
        )
    }
}

private fun computeDaylightProgress(sunriseIso: String, sunsetIso: String): Float {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    val sunrise = runCatching { fmt.parse(sunriseIso) }.getOrNull() ?: return 0.5f
    val sunset = runCatching { fmt.parse(sunsetIso) }.getOrNull() ?: return 0.5f
    val now = Date()
    val total = (sunset.time - sunrise.time).toFloat()
    if (total <= 0f) return 0.5f
    return ((now.time - sunrise.time).toFloat() / total).coerceIn(0f, 1f)
}
