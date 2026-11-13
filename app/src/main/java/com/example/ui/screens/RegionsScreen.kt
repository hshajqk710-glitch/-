package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RegionLocation
import com.example.data.model.SavedRegionSummary
import com.example.data.model.WeatherInterpreter
import com.example.data.model.WeatherUnits
import com.example.ui.components.WeatherGlassCard
import com.example.ui.state.RegionsUiState
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun RegionsScreen(
    activeRegion: RegionLocation,
    pinnedRegions: List<RegionLocation>,
    regionsState: RegionsUiState,
    units: WeatherUnits,
    isDetectingLocation: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectRegion: (RegionLocation) -> Unit,
    onTogglePin: (RegionLocation) -> Unit,
    onDetectGps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinnedIds = pinnedRegions.map { it.id }.toSet()

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
            item(key = "regions_banner") {
                RegionsHeroBanner()
            }

            item(key = "search_bar") {
                OutlinedTextField(
                    value = regionsState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("region_search_input"),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_region_hint),
                            color = TextMutedLight,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondaryLight,
                        )
                    },
                    trailingIcon = {
                        if (regionsState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = SolarGold,
                            )
                        } else if (regionsState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                    tint = TextSecondaryLight,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryLight,
                        unfocusedTextColor = TextPrimaryLight,
                        focusedContainerColor = GlassSurfaceElevated,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = SolarGold.copy(alpha = 0.7f),
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = SolarGold,
                    ),
                )
            }

            item(key = "gps_action_card") {
                WeatherGlassCard(
                    onClick = onDetectGps,
                    containerColor = CoolCyan.copy(alpha = 0.16f),
                    modifier = Modifier.testTag("regions_gps_card"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CoolCyan.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
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
                                    contentDescription = null,
                                    tint = CoolCyan,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.use_current_gps),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryLight,
                            )
                            Text(
                                text = stringResource(R.string.gps_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                            )
                        }
                    }
                }
            }

            if (regionsState.searchQuery.trim().length >= 2) {
                item(key = "search_header") {
                    Text(
                        text = stringResource(R.string.search_results).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryLight,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (regionsState.searchError != null) {
                    item(key = "search_error") {
                        WeatherGlassCard {
                            Text(
                                text = regionsState.searchError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryLight,
                            )
                        }
                    }
                } else if (!regionsState.isSearching && regionsState.searchResults.isEmpty()) {
                    item(key = "search_empty") {
                        WeatherGlassCard {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryLight,
                            )
                        }
                    }
                } else {
                    items(regionsState.searchResults, key = { "search_${it.id}" }) { result ->
                        val isPinned = result.id in pinnedIds
                        SearchResultCard(
                            region = result,
                            isPinned = isPinned,
                            onSelect = { onSelectRegion(result) },
                            onTogglePin = { onTogglePin(result) },
                        )
                    }
                }
            } else {
                item(key = "saved_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.saved_regions).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondaryLight,
                        )
                        if (regionsState.isLoadingSummaries) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = TextSecondaryLight,
                            )
                        }
                    }
                }

                items(regionsState.savedSummaries, key = { "saved_${it.location.id}" }) { summary ->
                    val isActive = summary.location.id == activeRegion.id
                    SavedRegionWeatherCard(
                        summary = summary,
                        units = units,
                        isActive = isActive,
                        canUnpin = regionsState.savedSummaries.size > 1,
                        onSelect = { onSelectRegion(summary.location) },
                        onUnpin = { onTogglePin(summary.location) },
                    )
                }
            }

            item(key = "regions_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RegionsHeroBanner() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_region_horizon),
                contentDescription = stringResource(R.string.regions_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x44061021),
                                Color(0xCC071326),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.regions_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryLight,
                )
                Text(
                    text = stringResource(R.string.regions_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    region: RegionLocation,
    isPinned: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
) {
    WeatherGlassCard(
        onClick = onSelect,
        modifier = Modifier.testTag("search_result_${region.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = onTogglePin,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("pin_region_${region.id}"),
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(
                        if (isPinned) R.string.unpin_region else R.string.pin_region,
                    ),
                    tint = if (isPinned) SolarGold else TextSecondaryLight,
                )
            }
        }
    }
}

@Composable
private fun SavedRegionWeatherCard(
    summary: SavedRegionSummary,
    units: WeatherUnits,
    isActive: Boolean,
    canUnpin: Boolean,
    onSelect: () -> Unit,
    onUnpin: () -> Unit,
) {
    val visual = summary.weatherCode?.let { WeatherInterpreter.interpret(it, summary.isDay) }

    WeatherGlassCard(
        onClick = onSelect,
        containerColor = if (isActive) GlassSurfaceElevated else GlassSurface,
        modifier = Modifier.testTag("saved_region_${summary.location.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.location.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = SolarGold,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (summary.location.subtitle.isNotBlank()) {
                    Text(
                        text = summary.location.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (visual != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = visual.titleRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = visual.tint,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (visual != null && summary.temperatureC != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = visual.titleRu,
                                tint = visual.tint,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = units.formatTemp(summary.temperatureC),
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimaryLight,
                            )
                        }
                        if (summary.tempMaxC != null && summary.tempMinC != null) {
                            Text(
                                text = "↑${units.formatTemp(summary.tempMaxC)}  ↓${units.formatTemp(summary.tempMinC)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryLight,
                            )
                        }
                    }
                }

                if (canUnpin) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onUnpin,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("unpin_region_${summary.location.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = stringResource(R.string.unpin_region),
                            tint = SolarGold,
                        )
                    }
                }
            }
        }
    }
}
