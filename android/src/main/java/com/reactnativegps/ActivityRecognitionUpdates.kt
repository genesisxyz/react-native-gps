package com.reactnativegps

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.tasks.Task

class ActivityRecognitionUpdates(private val context: Context): ServiceLifecycle, ServiceInterface {

    companion object {
        private const val TAG = "ActivityUpdates"

        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000
    }

    private var mActivityUpdatesTask: Task<*>? = null

    private var started = false

    // region ServiceLifecycle

    override fun onCreate() {

    }

    override fun onDestroy() {
        if (started) {
            removeActivityRecognitionUpdates()
        }
        started = false
    }

    // endregion

    private fun requestActivityRecognitionUpdates(): Task<*>? {
        Log.i(TAG, "Request activity recognition updates")
        return try {
            val intent = Intent(context, ActivityRecognitionIntentService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            mActivityUpdatesTask = ActivityRecognition.getClient(context)
                    .requestActivityUpdates(UPDATE_INTERVAL_IN_MILLISECONDS, pendingIntent)
            mActivityUpdatesTask
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
            null
        }
    }

    private fun removeActivityRecognitionUpdates(): Task<*>? {
        Log.i(TAG, "Removing activity recognition updates")
        return try {
            val intent = Intent(context, ActivityRecognitionIntentService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val task = ActivityRecognition.getClient(context).removeActivityUpdates(pendingIntent)
            mActivityUpdatesTask = null
            task
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
            null
        }
    }

    fun start() {
        started && return
        requestActivityRecognitionUpdates()
        started = true
    }

    // region ServiceInterface

    override fun updateOptions(options: HashMap<String, Any>?) {

    }

    // endregion
}