package com.reactnativegps

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceIntentService : IntentService(TAG) {
    companion object {
        private val TAG = "GeofenceIntentService"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }
            Log.d(TAG, geofencingEvent.toString())
            val context = applicationContext
            val myIntent = Intent(context, GeofenceEventService::class.java)
            myIntent.putExtra("transition", geofencingEvent.geofenceTransition)
            val ids = ArrayList<String>()
            for (geofence in geofencingEvent.triggeringGeofences) {
                ids.add(geofence.requestId)
            }
            myIntent.putExtra("ids", ids)
            context.startService(myIntent)
            HeadlessJsTaskService.acquireWakeLockNow(context)
        }
    }
}