package com.reactnativegps

import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class LocationPermissionEventService : HeadlessJsTaskService() {
    companion object {
        private const val TAG = "LocationPermissionEvent"
    }

    override fun getTaskConfig(intent: Intent): HeadlessJsTaskConfig? {
        val extras = intent.extras

        Log.i(TAG, "Sending data to the bridge")

        return HeadlessJsTaskConfig(
                "LocationPermission",
                if (extras != null) Arguments.fromBundle(extras) else Arguments.createMap(),
                5000,
                true)
    }
}
