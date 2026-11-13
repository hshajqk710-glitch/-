package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.RegionLocation
import com.example.data.model.WeatherForecast
import com.example.data.model.WeatherUnits
import com.example.data.remote.OpenMeteoClient
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "atmosphere_prefs")

class WeatherPreferencesDataStore(private val context: Context) {

    private val moshi = OpenMeteoClient.moshi
    private val regionAdapter = moshi.adapter(RegionLocation::class.java)
    private val unitsAdapter = moshi.adapter(WeatherUnits::class.java)
    private val forecastAdapter = moshi.adapter(WeatherForecast::class.java)
    private val regionListType = Types.newParameterizedType(List::class.java, RegionLocation::class.java)
    private val regionListAdapter = moshi.adapter<List<RegionLocation>>(regionListType)

    private object Keys {
        val ACTIVE_REGION = stringPreferencesKey("active_region_json")
        val PINNED_REGIONS = stringPreferencesKey("pinned_regions_json")
        val WEATHER_UNITS = stringPreferencesKey("weather_units_json")
        val CACHED_FORECAST = stringPreferencesKey("cached_forecast_json")
    }

    val activeRegionFlow: Flow<RegionLocation> = context.weatherDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_REGION]?.let { json ->
            runCatching { regionAdapter.fromJson(json) }.getOrNull()
        } ?: defaultRegion()
    }

    val pinnedRegionsFlow: Flow<List<RegionLocation>> = context.weatherDataStore.data.map { prefs ->
        prefs[Keys.PINNED_REGIONS]?.let { json ->
            runCatching { regionListAdapter.fromJson(json) }.getOrNull()
        } ?: defaultPinnedRegions()
    }

    val unitsFlow: Flow<WeatherUnits> = context.weatherDataStore.data.map { prefs ->
        prefs[Keys.WEATHER_UNITS]?.let { json ->
            runCatching { unitsAdapter.fromJson(json) }.getOrNull()
        } ?: WeatherUnits()
    }

    val cachedForecastFlow: Flow<WeatherForecast?> = context.weatherDataStore.data.map { prefs ->
        prefs[Keys.CACHED_FORECAST]?.let { json ->
            runCatching { forecastAdapter.fromJson(json) }.getOrNull()
        }
    }

    suspend fun setActiveRegion(region: RegionLocation) {
        context.weatherDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_REGION] = regionAdapter.toJson(region)
        }
    }

    suspend fun savePinnedRegions(regions: List<RegionLocation>) {
        context.weatherDataStore.edit { prefs ->
            prefs[Keys.PINNED_REGIONS] = regionListAdapter.toJson(regions)
        }
    }

    suspend fun updateUnits(units: WeatherUnits) {
        context.weatherDataStore.edit { prefs ->
            prefs[Keys.WEATHER_UNITS] = unitsAdapter.toJson(units)
        }
    }

    suspend fun cacheForecast(forecast: WeatherForecast) {
        context.weatherDataStore.edit { prefs ->
            prefs[Keys.CACHED_FORECAST] = forecastAdapter.toJson(forecast)
        }
    }

    companion object {
        fun defaultRegion(): RegionLocation = RegionLocation(
            id = "524901",
            name = "Москва",
            adminRegion = "Центральный регион",
            country = "Россия",
            latitude = 55.7558,
            longitude = 37.6173,
            isCurrentGps = false,
        )

        fun defaultPinnedRegions(): List<RegionLocation> = listOf(
            defaultRegion(),
            RegionLocation(
                id = "498817",
                name = "Санкт-Петербург",
                adminRegion = "Северо-Запад",
                country = "Россия",
                latitude = 59.9343,
                longitude = 30.3351,
            ),
            RegionLocation(
                id = "551487",
                name = "Казань",
                adminRegion = "Татарстан",
                country = "Россия",
                latitude = 55.7961,
                longitude = 49.1064,
            ),
            RegionLocation(
                id = "491422",
                name = "Сочи",
                adminRegion = "Краснодарский край",
                country = "Россия",
                latitude = 43.5855,
                longitude = 39.7231,
            ),
            RegionLocation(
                id = "1486209",
                name = "Екатеринбург",
                adminRegion = "Свердловская область",
                country = "Россия",
                latitude = 56.8389,
                longitude = 60.6057,
            ),
            RegionLocation(
                id = "1496747",
                name = "Новосибирск",
                adminRegion = "Сибирь",
                country = "Россия",
                latitude = 55.0084,
                longitude = 82.9357,
            ),
        )
    }
}
