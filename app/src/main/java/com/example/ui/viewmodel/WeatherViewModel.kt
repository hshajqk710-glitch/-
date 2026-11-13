package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocationProvider
import com.example.data.local.WeatherPreferencesDataStore
import com.example.data.model.AtmosphereMood
import com.example.data.model.NearbyRegionWeather
import com.example.data.model.PressureUnit
import com.example.data.model.RegionLocation
import com.example.data.model.TempUnit
import com.example.data.model.WeatherForecast
import com.example.data.model.WeatherInterpreter
import com.example.data.model.WeatherUnits
import com.example.data.model.WindUnit
import com.example.data.repository.WeatherRepository
import com.example.ui.state.AtmosphereAppState
import com.example.ui.state.ForecastUiState
import com.example.ui.state.RegionsUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(
        prefs = WeatherPreferencesDataStore(application),
        locationProvider = LocationProvider(application),
    )

    private val forecastStateFlow = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    private val regionsStateFlow = MutableStateFlow(RegionsUiState())
    private val isDetectingLocationFlow = MutableStateFlow(false)
    private val statusBannerFlow = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow("")

    private var fetchJob: Job? = null
    private var summariesJob: Job? = null
    private var latestForecast: WeatherForecast? = null

    val uiState: StateFlow<AtmosphereAppState> = combine(
        repository.activeRegionFlow,
        repository.unitsFlow,
        forecastStateFlow,
        regionsStateFlow,
        isDetectingLocationFlow,
        statusBannerFlow,
    ) { values ->
        val activeRegion = values[0] as RegionLocation
        val units = values[1] as WeatherUnits
        val forecastState = values[2] as ForecastUiState
        val regionsState = values[3] as RegionsUiState
        val isDetecting = values[4] as Boolean
        val banner = values[5] as String?

        val mood = when (forecastState) {
            is ForecastUiState.Success -> {
                val cur = forecastState.forecast.current
                WeatherInterpreter.interpret(cur.weatherCode, cur.isDay).mood
            }
            else -> AtmosphereMood.CLEAR_DAY
        }

        AtmosphereAppState(
            activeRegion = activeRegion,
            units = units,
            forecastState = forecastState,
            regionsState = regionsState,
            atmosphereMood = mood,
            isDetectingLocation = isDetecting,
            statusBannerMessage = banner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AtmosphereAppState(activeRegion = WeatherPreferencesDataStore.defaultRegion()),
    )

    val pinnedRegionsFlow: StateFlow<List<RegionLocation>> = repository.pinnedRegionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherPreferencesDataStore.defaultPinnedRegions(),
    )

    init {
        initializeWeather()
        observePinnedRegions()
        observeSearchQueries()
    }

    private fun initializeWeather() {
        viewModelScope.launch {
            val cached = repository.cachedForecastFlow.first()
            val active = repository.activeRegionFlow.first()
            val units = repository.unitsFlow.first()

            if (cached != null && cached.location.id == active.id) {
                latestForecast = cached
                forecastStateFlow.value = ForecastUiState.Success(
                    forecast = cached,
                    isFromCache = true,
                    isRefreshing = true,
                )
            }

            if (units.autoDetectLocationOnStart && repository.hasLocationPermission()) {
                val gpsRegion = repository.detectCurrentGpsRegion()
                if (gpsRegion != null) {
                    repository.setActiveRegion(gpsRegion)
                    loadForecast(gpsRegion, showFullLoading = cached == null)
                    return@launch
                }
            }

            loadForecast(active, showFullLoading = cached == null)
        }
    }

    private fun observePinnedRegions() {
        viewModelScope.launch {
            repository.pinnedRegionsFlow.collect { pinned ->
                refreshPinnedSummaries(pinned)
            }
        }
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun loadForecast(region: RegionLocation, showFullLoading: Boolean = false) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val current = forecastStateFlow.value
            if (showFullLoading || current !is ForecastUiState.Success) {
                forecastStateFlow.value = ForecastUiState.Loading
            } else {
                forecastStateFlow.value = current.copy(isRefreshing = true)
            }

            runCatching {
                repository.fetchFullForecast(region)
            }.onSuccess { forecast ->
                latestForecast = forecast
                forecastStateFlow.value = ForecastUiState.Success(
                    forecast = forecast,
                    isFromCache = false,
                    isRefreshing = false,
                )
            }.onFailure { error ->
                val fallback = latestForecast
                if (fallback != null && fallback.location.id == region.id) {
                    forecastStateFlow.value = ForecastUiState.Success(
                        forecast = fallback,
                        isFromCache = true,
                        isRefreshing = false,
                    )
                } else {
                    forecastStateFlow.value = ForecastUiState.Error(
                        message = error.localizedMessage ?: "Проверьте подключение к интернету",
                        region = region,
                    )
                }
            }
        }
    }

    fun refreshActiveForecast() {
        viewModelScope.launch {
            val active = repository.activeRegionFlow.first()
            loadForecast(active, showFullLoading = false)
            refreshPinnedSummaries(pinnedRegionsFlow.value)
        }
    }

    fun refreshPinnedSummaries(regions: List<RegionLocation> = pinnedRegionsFlow.value) {
        summariesJob?.cancel()
        summariesJob = viewModelScope.launch {
            regionsStateFlow.value = regionsStateFlow.value.copy(isLoadingSummaries = true)
            val summaries = repository.fetchPinnedSummaries(regions)
            regionsStateFlow.value = regionsStateFlow.value.copy(
                savedSummaries = summaries,
                isLoadingSummaries = false,
            )
        }
    }

    fun selectRegion(region: RegionLocation) {
        viewModelScope.launch {
            repository.setActiveRegion(region)
            loadForecast(region, showFullLoading = true)
        }
    }

    fun selectNearbyPoint(point: NearbyRegionWeather) {
        val region = RegionLocation(
            id = "nearby_${point.latitude}_${point.longitude}",
            name = point.areaName,
            adminRegion = point.directionLabel,
            country = "",
            latitude = point.latitude,
            longitude = point.longitude,
            isCurrentGps = false,
        )
        selectRegion(region)
    }

    fun togglePinRegion(region: RegionLocation) {
        viewModelScope.launch {
            val current = repository.pinnedRegionsFlow.first().toMutableList()
            val existingIndex = current.indexOfFirst { it.id == region.id }
            if (existingIndex >= 0) {
                if (current.size > 1) {
                    current.removeAt(existingIndex)
                }
            } else {
                current.add(0, region.copy(isCurrentGps = false))
            }
            repository.savePinnedRegions(current)
        }
    }

    fun onSearchQueryChanged(query: String) {
        regionsStateFlow.value = regionsStateFlow.value.copy(
            searchQuery = query,
            searchError = null,
        )
        searchQueryFlow.value = query
    }

    private suspend fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            regionsStateFlow.value = regionsStateFlow.value.copy(
                isSearching = false,
                searchResults = emptyList(),
                searchError = null,
            )
            return
        }

        regionsStateFlow.value = regionsStateFlow.value.copy(
            isSearching = true,
            searchError = null,
        )

        runCatching {
            repository.searchRegions(trimmed)
        }.onSuccess { results ->
            regionsStateFlow.value = regionsStateFlow.value.copy(
                isSearching = false,
                searchResults = results,
                searchError = null,
            )
        }.onFailure {
            regionsStateFlow.value = regionsStateFlow.value.copy(
                isSearching = false,
                searchResults = emptyList(),
                searchError = "Ошибка поиска. Проверьте сеть.",
            )
        }
    }

    fun detectAndSelectCurrentLocation() {
        viewModelScope.launch {
            isDetectingLocationFlow.value = true
            statusBannerFlow.value = null
            val gpsRegion = repository.detectCurrentGpsRegion()
            isDetectingLocationFlow.value = false
            if (gpsRegion != null) {
                repository.setActiveRegion(gpsRegion)
                loadForecast(gpsRegion, showFullLoading = true)
            } else {
                statusBannerFlow.value = "Не удалось получить координаты. Проверьте GPS или выберите регион вручную."
            }
        }
    }

    fun showLocationPermissionDeniedBanner() {
        statusBannerFlow.value = "Доступ к геолокации отклонён. Выберите город во вкладке «Регионы»."
    }

    fun dismissBanner() {
        statusBannerFlow.value = null
    }

    fun updateTempUnit(unit: TempUnit) {
        viewModelScope.launch {
            val current = repository.unitsFlow.first()
            repository.updateUnits(current.copy(tempUnit = unit))
        }
    }

    fun updateWindUnit(unit: WindUnit) {
        viewModelScope.launch {
            val current = repository.unitsFlow.first()
            repository.updateUnits(current.copy(windUnit = unit))
        }
    }

    fun updatePressureUnit(unit: PressureUnit) {
        viewModelScope.launch {
            val current = repository.unitsFlow.first()
            repository.updateUnits(current.copy(pressureUnit = unit))
        }
    }

    fun updateAutoDetectLocation(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.unitsFlow.first()
            repository.updateUnits(current.copy(autoDetectLocationOnStart = enabled))
        }
    }
}
