package com.example.data.repository

import com.example.data.local.LocationProvider
import com.example.data.local.WeatherPreferencesDataStore
import com.example.data.model.AirQualityInfo
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.NearbyRegionWeather
import com.example.data.model.RegionLocation
import com.example.data.model.SavedRegionSummary
import com.example.data.model.WeatherForecast
import com.example.data.model.WeatherUnits
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoClient
import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoGeocodingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max

class WeatherRepository(
    private val prefs: WeatherPreferencesDataStore,
    private val locationProvider: LocationProvider,
    private val forecastApi: OpenMeteoForecastApi = OpenMeteoClient.forecastApi,
    private val airQualityApi: OpenMeteoAirQualityApi = OpenMeteoClient.airQualityApi,
    private val geocodingApi: OpenMeteoGeocodingApi = OpenMeteoClient.geocodingApi,
) {
    val activeRegionFlow: Flow<RegionLocation> = prefs.activeRegionFlow
    val pinnedRegionsFlow: Flow<List<RegionLocation>> = prefs.pinnedRegionsFlow
    val unitsFlow: Flow<WeatherUnits> = prefs.unitsFlow
    val cachedForecastFlow: Flow<WeatherForecast?> = prefs.cachedForecastFlow

    fun hasLocationPermission(): Boolean = locationProvider.hasCoarseLocationPermission()

    suspend fun detectCurrentGpsRegion(): RegionLocation? = locationProvider.detectCurrentRegion()

    suspend fun setActiveRegion(region: RegionLocation) {
        prefs.setActiveRegion(region)
    }

    suspend fun savePinnedRegions(regions: List<RegionLocation>) {
        prefs.savePinnedRegions(regions)
    }

    suspend fun updateUnits(units: WeatherUnits) {
        prefs.updateUnits(units)
    }

    suspend fun fetchFullForecast(region: RegionLocation): WeatherForecast = withContext(Dispatchers.IO) {
        coroutineScope {
            val forecastDeferred = async {
                forecastApi.getForecast(
                    latitude = region.latitude,
                    longitude = region.longitude,
                )
            }
            val airQualityDeferred = async {
                runCatching {
                    airQualityApi.getAirQuality(
                        latitude = region.latitude,
                        longitude = region.longitude,
                    )
                }.getOrNull()
            }
            val nearbyDeferred = async {
                runCatching {
                    fetchNearbyPoints(region.latitude, region.longitude)
                }.getOrDefault(emptyList())
            }

            val forecastDto = forecastDeferred.await()
            val aqDto = airQualityDeferred.await()
            val nearby = nearbyDeferred.await()

            val domainForecast = mapToDomain(
                region = region,
                dto = forecastDto,
                airQuality = aqDto?.current?.let { aq ->
                    AirQualityInfo(
                        europeanAqi = aq.europeanAqi ?: 0,
                        pm25 = aq.pm25 ?: 0.0,
                        pm10 = aq.pm10 ?: 0.0,
                        ozone = aq.ozone ?: 0.0,
                        nitrogenDioxide = aq.nitrogenDioxide ?: 0.0,
                    )
                },
                nearbyPoints = nearby,
            )
            prefs.cacheForecast(domainForecast)
            domainForecast
        }
    }

    private suspend fun fetchNearbyPoints(lat: Double, lon: Double): List<NearbyRegionWeather> = coroutineScope {
        val latDelta = 0.36 // ~40 km
        val cosLat = max(0.25, cos(Math.toRadians(lat)))
        val lonDelta = 0.36 / cosLat

        val offsets = listOf(
            Triple("Север", lat + latDelta, lon),
            Triple("Восток", lat, lon + lonDelta),
            Triple("Юг", lat - latDelta, lon),
            Triple("Запад", lat, lon - lonDelta),
        )

        val latsCsv = offsets.joinToString(",") { String.format(Locale.US, "%.4f", it.second) }
        val lonsCsv = offsets.joinToString(",") { String.format(Locale.US, "%.4f", it.third) }

        val batch = forecastApi.getBatchCurrentWeather(
            latitudesCsv = latsCsv,
            longitudesCsv = lonsCsv,
        )

        val names = offsets.map { (_, pLat, pLon) ->
            async {
                locationProvider.reverseGeocodeAreaName(pLat, pLon)
            }
        }.awaitAll()

        offsets.mapIndexedNotNull { index, (direction, pLat, pLon) ->
            val current = batch.getOrNull(index)?.current ?: return@mapIndexedNotNull null
            val resolvedName = names.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "$direction области"
            NearbyRegionWeather(
                directionLabel = direction,
                areaName = resolvedName,
                latitude = pLat,
                longitude = pLon,
                temperatureC = current.temperature2m,
                weatherCode = current.weatherCode,
                isDay = current.isDay == 1,
            )
        }
    }

    suspend fun fetchPinnedSummaries(regions: List<RegionLocation>): List<SavedRegionSummary> =
        withContext(Dispatchers.IO) {
            if (regions.isEmpty()) return@withContext emptyList()
            runCatching {
                val items = if (regions.size == 1) {
                    val item = forecastApi.getSingleCurrentWeather(
                        latitude = regions.first().latitude,
                        longitude = regions.first().longitude,
                    )
                    listOf(item)
                } else {
                    forecastApi.getBatchCurrentWeather(
                        latitudesCsv = regions.joinToString(",") { String.format(Locale.US, "%.4f", it.latitude) },
                        longitudesCsv = regions.joinToString(",") { String.format(Locale.US, "%.4f", it.longitude) },
                    )
                }

                regions.mapIndexed { idx, region ->
                    val dto = items.getOrNull(idx)
                    SavedRegionSummary(
                        location = region,
                        temperatureC = dto?.current?.temperature2m,
                        tempMaxC = dto?.daily?.temperature2mMax?.firstOrNull(),
                        tempMinC = dto?.daily?.temperature2mMin?.firstOrNull(),
                        weatherCode = dto?.current?.weatherCode,
                        isDay = (dto?.current?.isDay ?: 1) == 1,
                    )
                }
            }.getOrElse {
                regions.map { SavedRegionSummary(location = it) }
            }
        }

    suspend fun searchRegions(query: String): List<RegionLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val response = geocodingApi.searchLocations(query = trimmed)
        response.results.orEmpty().map { item ->
            RegionLocation(
                id = item.id.toString(),
                name = item.name,
                adminRegion = item.admin1 ?: item.admin2 ?: "",
                country = item.country ?: "",
                latitude = item.latitude,
                longitude = item.longitude,
                isCurrentGps = false,
            )
        }
    }

    private fun mapToDomain(
        region: RegionLocation,
        dto: OpenMeteoForecastResponse,
        airQuality: AirQualityInfo?,
        nearbyPoints: List<NearbyRegionWeather>,
    ): WeatherForecast {
        val cur = dto.current
        val currentHourIso = cur?.time?.take(13) ?: ""

        val hourlyDto = dto.hourly
        val allTimes = hourlyDto?.time.orEmpty()
        val startIndex = if (currentHourIso.isNotEmpty()) {
            allTimes.indexOfFirst { it.startsWith(currentHourIso) }.takeIf { it >= 0 } ?: 0
        } else 0

        val currentUv = hourlyDto?.uvIndex?.getOrNull(startIndex) ?: 0.0
        val currentVisMeters = hourlyDto?.visibility?.getOrNull(startIndex) ?: 10000.0

        val hourlyList = (startIndex until minOf(startIndex + 24, allTimes.size)).map { i ->
            val iso = allTimes[i]
            HourlyForecast(
                isoTime = iso,
                hourLabel = if (i == startIndex) "Сейчас" else iso.substringAfter("T", "00:00").take(5),
                temperatureC = hourlyDto?.temperature2m?.getOrNull(i) ?: (cur?.temperature2m ?: 0.0),
                weatherCode = hourlyDto?.weatherCode?.getOrNull(i) ?: (cur?.weatherCode ?: 0),
                isDay = (hourlyDto?.isDay?.getOrNull(i) ?: (cur?.isDay ?: 1)) == 1,
                precipitationProbability = hourlyDto?.precipitationProbability?.getOrNull(i) ?: 0,
                windSpeedKmh = hourlyDto?.windSpeed10m?.getOrNull(i) ?: 0.0,
            )
        }

        val dailyDto = dto.daily
        val dayDates = dailyDto?.time.orEmpty()
        val dailyList = dayDates.indices.map { i ->
            val isoDate = dayDates[i]
            val (dayName, shortDate) = formatRussianDay(isoDate, i)
            DailyForecast(
                isoDate = isoDate,
                dayName = dayName,
                shortDate = shortDate,
                weatherCode = dailyDto?.weatherCode?.getOrNull(i) ?: 0,
                tempMaxC = dailyDto?.temperature2mMax?.getOrNull(i) ?: 0.0,
                tempMinC = dailyDto?.temperature2mMin?.getOrNull(i) ?: 0.0,
                precipitationProbabilityMax = dailyDto?.precipitationProbabilityMax?.getOrNull(i) ?: 0,
                sunriseIso = dailyDto?.sunrise?.getOrNull(i) ?: "",
                sunsetIso = dailyDto?.sunset?.getOrNull(i) ?: "",
                uvIndexMax = dailyDto?.uvIndexMax?.getOrNull(i) ?: 0.0,
                windSpeedMaxKmh = dailyDto?.windSpeed10mMax?.getOrNull(i) ?: 0.0,
            )
        }

        val timeFmt = SimpleDateFormat("HH:mm", Locale("ru"))
        val updatedLabel = timeFmt.format(Date())

        return WeatherForecast(
            location = region,
            fetchedAtMillis = System.currentTimeMillis(),
            updatedTimeLabel = updatedLabel,
            current = CurrentWeather(
                temperatureC = cur?.temperature2m ?: 0.0,
                apparentTemperatureC = cur?.apparentTemperature ?: 0.0,
                humidityPercent = cur?.relativeHumidity2m ?: 0,
                weatherCode = cur?.weatherCode ?: 0,
                isDay = (cur?.isDay ?: 1) == 1,
                windSpeedKmh = cur?.windSpeed10m ?: 0.0,
                windDirectionDeg = cur?.windDirection10m ?: 0,
                windGustsKmh = cur?.windGusts10m ?: 0.0,
                pressureHpa = cur?.surfacePressure ?: 1013.0,
                cloudCoverPercent = cur?.cloudCover ?: 0,
                precipitationMm = cur?.precipitation ?: 0.0,
                uvIndex = currentUv,
                visibilityKm = currentVisMeters / 1000.0,
            ),
            hourly = hourlyList,
            daily = dailyList,
            airQuality = airQuality,
            nearbyPoints = nearbyPoints,
        )
    }

    private fun formatRussianDay(isoDate: String, index: Int): Pair<String, String> {
        val ru = Locale("ru")
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = runCatching { parser.parse(isoDate) }.getOrNull() ?: return "День ${index + 1}" to isoDate
        val shortFmt = SimpleDateFormat("d MMM", ru)
        val shortStr = shortFmt.format(date).replace(".", "")

        val dayName = when (index) {
            0 -> "Сегодня"
            1 -> "Завтра"
            else -> {
                val cal = Calendar.getInstance().apply { time = date }
                when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Понедельник"
                    Calendar.TUESDAY -> "Вторник"
                    Calendar.WEDNESDAY -> "Среда"
                    Calendar.THURSDAY -> "Четверг"
                    Calendar.FRIDAY -> "Пятница"
                    Calendar.SATURDAY -> "Суббота"
                    Calendar.SUNDAY -> "Воскресенье"
                    else -> isoDate
                }
            }
        }
        return dayName to shortStr
    }
}
