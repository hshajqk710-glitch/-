package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.AtmosphericBackground
import com.example.ui.screens.ForecastScreen
import com.example.ui.screens.RegionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.viewmodel.WeatherViewModel

const val ROUTE_FORECAST = "forecast"
const val ROUTE_REGIONS = "regions"
const val ROUTE_SETTINGS = "settings"

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val navItems = listOf(
    NavItem(
        route = ROUTE_FORECAST,
        labelRes = R.string.nav_forecast,
        selectedIcon = Icons.Rounded.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny,
        testTag = "nav_forecast_tab",
    ),
    NavItem(
        route = ROUTE_REGIONS,
        labelRes = R.string.nav_regions,
        selectedIcon = Icons.Rounded.LocationCity,
        unselectedIcon = Icons.Outlined.LocationCity,
        testTag = "nav_regions_tab",
    ),
    NavItem(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings_tab",
    ),
)

@Composable
fun AtmosphereApp(
    viewModel: WeatherViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pinnedRegions by viewModel.pinnedRegionsFlow.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_FORECAST
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.detectAndSelectCurrentLocation()
        } else {
            viewModel.showLocationPermissionDeniedBanner()
        }
    }

    val requestGpsWeather: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.detectAndSelectCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    AtmosphericBackground(mood = state.atmosphereMood) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = Color(0xCC091529),
                            contentColor = TextPrimaryLight,
                            tonalElevation = 0.dp,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SolarGold,
                                        selectedTextColor = SolarGold,
                                        indicatorColor = Color.White.copy(alpha = 0.14f),
                                        unselectedIconColor = TextSecondaryLight,
                                        unselectedTextColor = TextSecondaryLight,
                                    ),
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = Color(0x88091529),
                            contentColor = TextPrimaryLight,
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(Color(0x44091529))
                                .statusBarsPadding(),
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SolarGold,
                                        selectedTextColor = SolarGold,
                                        indicatorColor = Color.White.copy(alpha = 0.14f),
                                        unselectedIconColor = TextSecondaryLight,
                                        unselectedTextColor = TextSecondaryLight,
                                    ),
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_FORECAST,
                        ) {
                            composable(ROUTE_FORECAST) {
                                ForecastScreen(
                                    state = state,
                                    onRefresh = { viewModel.refreshActiveForecast() },
                                    onRequestGps = requestGpsWeather,
                                    onOpenRegions = { navigateTo(ROUTE_REGIONS) },
                                    onSelectNearbyPoint = { point ->
                                        viewModel.selectNearbyPoint(point)
                                    },
                                    onDismissBanner = { viewModel.dismissBanner() },
                                )
                            }

                            composable(ROUTE_REGIONS) {
                                RegionsScreen(
                                    activeRegion = state.activeRegion,
                                    pinnedRegions = pinnedRegions,
                                    regionsState = state.regionsState,
                                    units = state.units,
                                    isDetectingLocation = state.isDetectingLocation,
                                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                    onSelectRegion = { region ->
                                        viewModel.selectRegion(region)
                                        navigateTo(ROUTE_FORECAST)
                                    },
                                    onTogglePin = { viewModel.togglePinRegion(it) },
                                    onDetectGps = {
                                        requestGpsWeather()
                                        navigateTo(ROUTE_FORECAST)
                                    },
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    units = state.units,
                                    onTempUnitChange = { viewModel.updateTempUnit(it) },
                                    onWindUnitChange = { viewModel.updateWindUnit(it) },
                                    onPressureUnitChange = { viewModel.updatePressureUnit(it) },
                                    onAutoDetectChange = { viewModel.updateAutoDetectLocation(it) },
                                    onDetectNow = {
                                        requestGpsWeather()
                                        navigateTo(ROUTE_FORECAST)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
