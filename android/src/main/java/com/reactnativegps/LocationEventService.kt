package com.reactnativegps

import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class LocationEventService : HeadlessJsTaskService() {
    companion object {
        private const val TAG = "LocationEventService"
    }

    override fun getTaskConfig(intent: Intent): HeadlessJsTaskConfig? {
        val extras = intent.extras

        Log.i(TAG, "Sending data to the bridge")

        return HeadlessJsTaskConfig(
                "Location",
                if (extras != null) Arguments.fromBundle(extras) else Arguments.createMap(),
                5000,
                true)
    }
}
