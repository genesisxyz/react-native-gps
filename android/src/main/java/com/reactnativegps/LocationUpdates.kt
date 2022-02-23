package com.reactnativegps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

fun Location.toMap(): WritableMap {
    val map = WritableNativeMap()

    if (hasAccuracy()) {
        map.putDouble("accuracy", accuracy.toDouble())
    } else {
        map.putNull("accuracy")
    }

    if (hasSpeed()) {
        map.putDouble("speed", speed.toDouble())
    } else {
        map.putNull("speed")
    }

    if (hasBearing()) {
        map.putDouble("bearing", bearing.toDouble())
    } else {
        map.putNull("bearing")
    }

    if (hasAltitude()) {
        map.putDouble("altitude", altitude)
    } else {
        map.putNull("altitude")
    }

    map.putDouble("latitude", latitude)
    map.putDouble("longitude", longitude)
    map.putInt("time", time.toInt()) // TODO: probably not a good idea

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        map.putBoolean("isFromMockProvider", isFromMockProvider)
    }

    return map
}

class LocationUpdates(private val context: Context): ServiceLifecycle, ServiceInterface {
    companion object {
        private const val TAG = "LocationUpdates"

        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var mLocationCallback: LocationCallback

    private var mLocation: Location? = null
    private var mLocationUpdatesTask: Task<*>? = null

    private var started = false

    private var locationInterval = UPDATE_INTERVAL_IN_MILLISECONDS
    private var locationFastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
    private var locationPriority = LocationRequest.PRIORITY_HIGH_ACCURACY
    private var locationSmallestDisplacement = 10.0f

    // region ServiceLifecycle

    override fun onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }
    }

    override fun onDestroy() {
        if (started) {
            removeLocationUpdates()
        }
        started = false
    }

    // endregion

    private fun requestLocationUpdates(): Task<*>? {
        Log.i(TAG, "Request location updates")
        val locationRequest = createLocationRequest()
        return try {
            mLocationUpdatesTask = mFusedLocationClient?.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
            mLocationUpdatesTask
        } catch (unlikely: SecurityException) {
            // Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
            null
        }
    }

    private fun removeLocationUpdates(): Task<Void>? {
        Log.i(TAG, "Removing location updates")
        return try {
            val task = mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            mLocationUpdatesTask = null
            task
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
            null
        }
    }

    val lastLocation: Task<Location>?
        get() {
            return if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                null
            }
            else mFusedLocationClient.lastLocation
        }

    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")
        if (location.hasAccuracy()) {
            if (!location.hasSpeed()) {
                location.speed = 0f
            }
            if (!location.hasBearing()) {
                location.bearing = 0f
            }
            if (!location.hasAltitude()) {
                location.altitude = 0.0
            }

            val myIntent = Intent(context, LocationEventService::class.java)
            myIntent.putExtra("latitude", location.latitude)
            myIntent.putExtra("longitude", location.longitude)
            myIntent.putExtra("speed", location.speed)
            myIntent.putExtra("accuracy", location.accuracy)
            myIntent.putExtra("altitude", location.altitude)
            myIntent.putExtra("bearing", location.bearing)
            myIntent.putExtra("time", location.time)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                myIntent.putExtra("isFromMockProvider", location.isFromMockProvider)
            }

            context.startService(myIntent)
            HeadlessJsTaskService.acquireWakeLockNow(context)
        }
        mLocation = location
    }

    /**
     * Sets the location request parameters.
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = locationInterval
            fastestInterval = locationFastestInterval
            priority = locationPriority
            smallestDisplacement = locationSmallestDisplacement
        }
    }

    fun start() {
        started && return

        requestLocationUpdates()

        started = true
    }

    // region ServiceInterface

    override fun updateOptions(options: HashMap<String, Any>?) {
        val androidOptions = options?.get("android") as HashMap<String, Any>?

        val locationOptions = androidOptions?.get("location") as HashMap<String, Any>?
        val newLocationInterval = locationOptions?.get("interval") as Double?;
        val newLocationFastestInterval = locationOptions?.get("fastestInterval") as Double?;
        val newLocationPriority = locationOptions?.get("priority") as Double?;
        val newLocationSmallestDisplacement = locationOptions?.get("smallestDisplacement") as Double?;

        if (newLocationInterval != null) {
            locationInterval = newLocationInterval.toLong()
        }

        if (newLocationFastestInterval != null) {
            locationFastestInterval = newLocationFastestInterval.toLong()
        }

        if (newLocationPriority != null) {
            locationPriority = newLocationPriority.toInt()
        }

        if (newLocationSmallestDisplacement != null) {
            locationSmallestDisplacement = newLocationSmallestDisplacement.toFloat()
        }

        if (newLocationPriority != null || newLocationInterval != null || newLocationFastestInterval != null || newLocationSmallestDisplacement != null) {
            val locationRequest = createLocationRequest()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mLocationUpdatesTask = mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
            }
        }
    }

    // endregion
}
