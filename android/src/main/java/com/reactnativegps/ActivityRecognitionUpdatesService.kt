package com.reactnativegps

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class ActivityRecognitionUpdatesService : Service(), ServiceInterface {
    companion object {
        private const val TAG = "ActivityUpdatesService"

        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000
    }

    private var mServiceHandler: Handler? = null

    private val binder: IBinder = UpdatesBinder()

    private var isBound = false

    private var mActivityUpdatesTask: Task<*>? = null

    inner class UpdatesBinder() : Binder(), ServiceBinderInterface {
        // Return this instance of LocalService so clients can call public methods
        override val service: ActivityRecognitionUpdatesService
            get() =// Return this instance of LocalService so clients can call public methods
                this@ActivityRecognitionUpdatesService
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

        requestActivityRecognitionUpdates()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
    }

    private fun requestActivityRecognitionUpdates(): Task<*>? {
        Log.i(TAG, "Request activity recognition updates")
        return try {
            val intent = Intent(applicationContext, ActivityRecognitionIntentService::class.java)
            val pendingIntent = PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            mActivityUpdatesTask = ActivityRecognition.getClient(applicationContext)
                    .requestActivityUpdates(UPDATE_INTERVAL_IN_MILLISECONDS, pendingIntent)
            mActivityUpdatesTask
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
            null
        }
    }

    fun removeActivityRecognitionUpdates(): Task<*>? {
        Log.i(TAG, "Removing activity recognition updates")
        return try {
            val intent = Intent(applicationContext, ActivityRecognitionIntentService::class.java)
            val pendingIntent = PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val task = ActivityRecognition.getClient(applicationContext).removeActivityUpdates(pendingIntent)
            mActivityUpdatesTask = null
            task
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
            null
        }
    }

    override fun onDestroy() {
        removeActivityRecognitionUpdates()
        mServiceHandler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    private fun startForeground() {
        Notification.createNotificationChannel(applicationContext)
        val notification = Notification.getNotification(applicationContext)
        startForeground(Notification.notificationId, notification)
    }

    override fun updateOptions(options: HashMap<String, Any>?) {

    }
}