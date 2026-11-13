package com.example.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.RegionLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentRegion(): RegionLocation? {
        if (!hasCoarseLocationPermission()) return null

        val location = suspendCancellableCoroutine<Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { current ->
                    if (current != null) {
                        cont.resume(current)
                    } else {
                        fusedClient.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    fusedClient.lastLocation
                        .addOnSuccessListener { last -> cont.resume(last) }
                        .addOnFailureListener { cont.resume(null) }
                }
        } ?: return null

        return reverseGeocode(location.latitude, location.longitude)
    }

    suspend fun reverseGeocodeAreaName(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            val addresses = fetchAddresses(latitude, longitude)
            val addr = addresses.firstOrNull() ?: return@withContext null
            addr.locality
                ?: addr.subAdminArea
                ?: addr.adminArea
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): RegionLocation {
        val address = withContext(Dispatchers.IO) {
            fetchAddresses(latitude, longitude).firstOrNull()
        }
        val city = address?.locality
            ?: address?.subAdminArea
            ?: address?.adminArea
            ?: "Текущий регион"
        val admin = address?.adminArea ?: ""
        val country = address?.countryName ?: ""

        return RegionLocation(
            id = "gps_${String.format(Locale.US, "%.2f_%.2f", latitude, longitude)}",
            name = city,
            adminRegion = if (admin == city) "" else admin,
            country = country,
            latitude = latitude,
            longitude = longitude,
            isCurrentGps = true,
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun fetchAddresses(latitude: Double, longitude: Double): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(context, Locale("ru"))
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        cont.resume(list)
                    }
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }
}
