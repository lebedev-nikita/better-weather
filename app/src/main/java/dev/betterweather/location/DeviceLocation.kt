package dev.betterweather.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** One foreground location request. No Google Play dependency or background tracking. */
suspend fun deviceLocation(context: Context): Location? {
    if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val manager = context.getSystemService(LocationManager::class.java)
    val precise = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val available = manager.getProviders(true)
    // GPS requires precise permission on some devices; fused/network can serve coarse fixes.
    val providers = (listOf("fused", LocationManager.NETWORK_PROVIDER) + if (precise) listOf(LocationManager.GPS_PROVIDER) else emptyList())
        .filter { it in available }
    if (providers.isEmpty()) return null
    providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .filter { TimeUnit.NANOSECONDS.toMinutes(android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) < 15 }
        .minByOrNull { it.accuracy }
        ?.let { return it }

    return withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            val registered = providers.count { provider ->
                try {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    true
                } catch (error: SecurityException) { false }
            }
            if (registered == 0 && continuation.isActive) continuation.resume(null)
        }
    }
}
