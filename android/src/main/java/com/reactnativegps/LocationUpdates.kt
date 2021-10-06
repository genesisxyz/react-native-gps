package com.reactnativegps

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

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

    // TODO: make a function
    private val lastLocation: Unit
        get() {
            try {
                mFusedLocationClient.lastLocation?.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
            } catch (unlikely: SecurityException) {
                Log.e(TAG, "Lost location permission.$unlikely")
            }
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
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 10.0f
        }
    }

    fun start() {
        started && return

        requestLocationUpdates()

        started = true
    }

    // region ServiceInterface

    override fun updateOptions(options: HashMap<String, Any>?) {

    }

    // endregion
}