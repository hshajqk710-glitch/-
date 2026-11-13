package com.example.ui.state

import com.example.data.model.AtmosphereMood
import com.example.data.model.RegionLocation
import com.example.data.model.SavedRegionSummary
import com.example.data.model.WeatherForecast
import com.example.data.model.WeatherUnits

sealed interface ForecastUiState {
    data object Loading : ForecastUiState
    data class Success(
        val forecast: WeatherForecast,
        val isFromCache: Boolean = false,
        val isRefreshing: Boolean = false,
    ) : ForecastUiState
    data class Error(
        val message: String,
        val region: RegionLocation,
    ) : ForecastUiState
}

data class RegionsUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<RegionLocation> = emptyList(),
    val searchError: String? = null,
    val savedSummaries: List<SavedRegionSummary> = emptyList(),
    val isLoadingSummaries: Boolean = false,
)

data class AtmosphereAppState(
    val activeRegion: RegionLocation,
    val units: WeatherUnits = WeatherUnits(),
    val forecastState: ForecastUiState = ForecastUiState.Loading,
    val regionsState: RegionsUiState = RegionsUiState(),
    val atmosphereMood: AtmosphereMood = AtmosphereMood.CLEAR_DAY,
    val isDetectingLocation: Boolean = false,
    val statusBannerMessage: String? = null,
)
