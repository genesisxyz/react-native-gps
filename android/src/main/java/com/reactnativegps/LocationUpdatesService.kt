package com.reactnativegps

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationUpdatesService : Service() {
    companion object {
        private const val TAG = "LocationUpdatesService"
        private const val SERVICE_NOTIFICATION_ID = 12345678
        private const val CHANNEL_ID = "location"

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value.
         */
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Callback for changes in location.
     */
    private lateinit var mLocationCallback: LocationCallback

    /**
     * The current location.
     */
    private var mLocation: Location? = null
    private var mServiceHandler: Handler? = null
    private var mLocationUpdatesTask: Task<*>? = null

    private val binder: IBinder = LocationUpdatesBinder()

    private var isBound = false

    inner class LocationUpdatesBinder() : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: LocationUpdatesService
            get() =// Return this instance of LocalService so clients can call public methods
                this@LocationUpdatesService
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, "Tracking enabled", importance)
            channel.description = "Tracking enabled"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    //region binding

    override fun onBind(intent: Intent): IBinder {
        Log.i(TAG, "onBind")

        isBound = true

        stopForeground(true)

        return binder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)

        Log.i(TAG, "onUnbind")

        isBound = false

        startForeground()

        return true
    }


    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        Log.i(TAG, "onRebind")

        isBound = true

        stopForeground(true)
    }

    //endregion

    override fun onCreate() {
        super.onCreate()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
    }

    fun requestLocationUpdates(): Task<*>? {
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

    fun removeLocationUpdates(): Task<Void>? {
        Log.i(TAG, "Removing location updates")
        return try {
            val task = mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
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
                mFusedLocationClient?.lastLocation?.addOnCompleteListener { task ->
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
            val context = applicationContext

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

    override fun onDestroy() {
        mServiceHandler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotificationChannel()

        return if (intent != null) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Gps")
                .setContentText("Tracking enabled")
                // .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build()
        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }
}