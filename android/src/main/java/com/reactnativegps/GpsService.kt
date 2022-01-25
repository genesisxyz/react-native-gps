package com.reactnativegps

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class GpsService : Service(), ServiceInterface {
    companion object {
        private const val TAG = "GpsService"
    }

    var locationUpdates: LocationUpdates? = null
    var geofenceUpdates: GeofenceUpdates? = null
    var activityRecognitionUpdates: ActivityRecognitionUpdates? = null

    private var mServiceHandler: Handler? = null

    private val binder: IBinder = UpdatesBinder()

    private var isBound = false

    inner class UpdatesBinder() : Binder(), ServiceBinderInterface {
        // Return this instance of LocalService so clients can call public methods
        override val service: GpsService
            get() =// Return this instance of LocalService so clients can call public methods
                this@GpsService
    }

    // region binding

    override fun onBind(intent: Intent): IBinder {
        Log.i(TAG, "onBind")

        isBound = true

        return binder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)

        Log.i(TAG, "onUnbind")

        isBound = false

        return true
    }


    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        Log.i(TAG, "onRebind")

        isBound = true
    }

    // endregion

    // region lifecycle

    override fun onCreate() {
        super.onCreate()

        locationUpdates = LocationUpdates(applicationContext)
        geofenceUpdates = GeofenceUpdates(applicationContext)
        activityRecognitionUpdates = ActivityRecognitionUpdates(applicationContext)

        locationUpdates?.onCreate()
        geofenceUpdates?.onCreate()
        activityRecognitionUpdates?.onCreate()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
    }

    override fun onDestroy() {
        locationUpdates?.onDestroy()
        geofenceUpdates?.onDestroy()
        activityRecognitionUpdates?.onDestroy()

        mServiceHandler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_NOT_STICKY
    }

    // endregion

    private fun startForeground() {
        Notification.createNotificationChannel(applicationContext)
        val notification = Notification.getNotification(applicationContext)
        startForeground(Notification.notificationId, notification)
    }

    // region ServiceInterface

    override fun updateOptions(options: HashMap<String, Any>?) {
        locationUpdates?.updateOptions(options)
        geofenceUpdates?.updateOptions(options)
        activityRecognitionUpdates?.updateOptions(options)
    }

    // endregion
}