package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.FilterDrama
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

enum class TempUnit(val symbol: String, val labelRu: String) {
    CELSIUS("°C", "Цельсий (°C)"),
    FAHRENHEIT("°F", "Фаренгейт (°F)"),
}

enum class WindUnit(val symbol: String, val labelRu: String) {
    MS("м/с", "м/с"),
    KMH("км/ч", "км/ч"),
}

enum class PressureUnit(val symbol: String, val labelRu: String) {
    MMHG("мм рт. ст.", "мм рт. ст."),
    HPA("гПа", "гПа"),
}

@JsonClass(generateAdapter = true)
data class WeatherUnits(
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.MS,
    val pressureUnit: PressureUnit = PressureUnit.MMHG,
    val autoDetectLocationOnStart: Boolean = true,
) {
    fun formatTemp(celsius: Double, includeUnit: Boolean = false): String {
        val value = when (tempUnit) {
            TempUnit.CELSIUS -> celsius.roundToInt()
            TempUnit.FAHRENHEIT -> (celsius * 9.0 / 5.0 + 32.0).roundToInt()
        }
        return if (includeUnit) "$value${tempUnit.symbol}" else "$value°"
    }

    fun formatWind(kmh: Double): String {
        return when (windUnit) {
            WindUnit.MS -> {
                val ms = kmh / 3.6
                if (ms < 10.0) String.format(java.util.Locale("ru"), "%.1f %s", ms, windUnit.symbol)
                else "${ms.roundToInt()} ${windUnit.symbol}"
            }
            WindUnit.KMH -> "${kmh.roundToInt()} ${windUnit.symbol}"
        }
    }

    fun formatPressure(hpa: Double): String {
        return when (pressureUnit) {
            PressureUnit.MMHG -> "${(hpa * 0.750062).roundToInt()} ${pressureUnit.symbol}"
            PressureUnit.HPA -> "${hpa.roundToInt()} ${pressureUnit.symbol}"
        }
    }
}

@JsonClass(generateAdapter = true)
data class RegionLocation(
    val id: String,
    val name: String,
    val adminRegion: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean = false,
) {
    val subtitle: String
        get() = listOf(adminRegion, country).filter { it.isNotBlank() && it != name }.joinToString(", ")
}

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val windGustsKmh: Double,
    val pressureHpa: Double,
    val cloudCoverPercent: Int,
    val precipitationMm: Double,
    val uvIndex: Double,
    val visibilityKm: Double,
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    val isoTime: String,
    val hourLabel: String,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipitationProbability: Int,
    val windSpeedKmh: Double,
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val isoDate: String,
    val dayName: String,
    val shortDate: String,
    val weatherCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipitationProbabilityMax: Int,
    val sunriseIso: String,
    val sunsetIso: String,
    val uvIndexMax: Double,
    val windSpeedMaxKmh: Double,
)

@JsonClass(generateAdapter = true)
data class AirQualityInfo(
    val europeanAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double,
    val nitrogenDioxide: Double,
)

@JsonClass(generateAdapter = true)
data class NearbyRegionWeather(
    val directionLabel: String,
    val areaName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
)

@JsonClass(generateAdapter = true)
data class WeatherForecast(
    val location: RegionLocation,
    val fetchedAtMillis: Long,
    val updatedTimeLabel: String,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val airQuality: AirQualityInfo? = null,
    val nearbyPoints: List<NearbyRegionWeather> = emptyList(),
)

data class SavedRegionSummary(
    val location: RegionLocation,
    val temperatureC: Double? = null,
    val tempMaxC: Double? = null,
    val tempMinC: Double? = null,
    val weatherCode: Int? = null,
    val isDay: Boolean = true,
)

enum class AtmosphereMood {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    STORM,
    SNOW,
}

data class WeatherVisualInfo(
    val titleRu: String,
    val icon: ImageVector,
    val tint: Color,
    val mood: AtmosphereMood,
)

object WeatherInterpreter {

    fun interpret(code: Int, isDay: Boolean): WeatherVisualInfo {
        return when (code) {
            0 -> WeatherVisualInfo(
                titleRu = if (isDay) "Ясно" else "Ясная ночь",
                icon = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                tint = if (isDay) Color(0xFFFFCA58) else Color(0xFFBBD7FF),
                mood = if (isDay) AtmosphereMood.CLEAR_DAY else AtmosphereMood.CLEAR_NIGHT,
            )
            1 -> WeatherVisualInfo(
                titleRu = "Преимущественно ясно",
                icon = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                tint = if (isDay) Color(0xFFFFD166) else Color(0xFFC4DCFF),
                mood = if (isDay) AtmosphereMood.CLEAR_DAY else AtmosphereMood.CLEAR_NIGHT,
            )
            2 -> WeatherVisualInfo(
                titleRu = "Переменная облачность",
                icon = Icons.Rounded.FilterDrama,
                tint = Color(0xFFE2EEFF),
                mood = AtmosphereMood.PARTLY_CLOUDY,
            )
            3 -> WeatherVisualInfo(
                titleRu = "Пасмурно",
                icon = Icons.Rounded.Cloud,
                tint = Color(0xFFCAD8EB),
                mood = AtmosphereMood.CLOUDY,
            )
            45, 48 -> WeatherVisualInfo(
                titleRu = if (code == 48) "Изморозь и туман" else "Туман",
                icon = Icons.Rounded.Dehaze,
                tint = Color(0xFFD0DCEB),
                mood = AtmosphereMood.FOG,
            )
            51, 53, 55, 56, 57 -> WeatherVisualInfo(
                titleRu = "Морось",
                icon = Icons.Rounded.Grain,
                tint = Color(0xFF7DD3FC),
                mood = AtmosphereMood.RAIN,
            )
            61, 80 -> WeatherVisualInfo(
                titleRu = "Небольшой дождь",
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF60A5FA),
                mood = AtmosphereMood.RAIN,
            )
            63, 81 -> WeatherVisualInfo(
                titleRu = "Умеренный дождь",
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF38BDF8),
                mood = AtmosphereMood.RAIN,
            )
            65, 82, 66, 67 -> WeatherVisualInfo(
                titleRu = "Сильный дождь",
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF3B82F6),
                mood = AtmosphereMood.RAIN,
            )
            71, 73, 75, 77, 85, 86 -> WeatherVisualInfo(
                titleRu = when (code) {
                    71, 85 -> "Небольшой снег"
                    75, 86 -> "Снегопад"
                    else -> "Снег"
                },
                icon = Icons.Rounded.AcUnit,
                tint = Color(0xFFE0F2FE),
                mood = AtmosphereMood.SNOW,
            )
            95, 96, 99 -> WeatherVisualInfo(
                titleRu = if (code == 95) "Гроза" else "Гроза с градом",
                icon = if (code == 95) Icons.Rounded.Bolt else Icons.Rounded.Thunderstorm,
                tint = Color(0xFFFDE047),
                mood = AtmosphereMood.STORM,
            )
            else -> WeatherVisualInfo(
                titleRu = "Переменчиво",
                icon = Icons.Rounded.Cloud,
                tint = Color(0xFFDCE8F7),
                mood = AtmosphereMood.CLOUDY,
            )
        }
    }

    fun windCardinalRu(degrees: Int): String {
        val dirs = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
        val index = (((degrees % 360) + 360) % 360 + 22.5) / 45.0
        return dirs[index.toInt() % 8]
    }

    fun uvLevelRu(uv: Double): String = when {
        uv < 3.0 -> "Низкий"
        uv < 6.0 -> "Умеренный"
        uv < 8.0 -> "Высокий"
        uv < 11.0 -> "Очень высокий"
        else -> "Экстремальный"
    }

    fun aqiDescriptionRu(eaqi: Int): Pair<String, Color> = when {
        eaqi <= 20 -> "Отличное" to Color(0xFF4ADE80)
        eaqi <= 40 -> "Хорошее" to Color(0xFFA3E635)
        eaqi <= 60 -> "Умеренное" to Color(0xFFFACC15)
        eaqi <= 80 -> "Пониженное" to Color(0xFFFB923C)
        eaqi <= 100 -> "Плохое" to Color(0xFFF87171)
        else -> "Опасное" to Color(0xFFE879F9)
    }
}
