package com.reactnativegps

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.ActivityRecognitionResult

class ActivityRecognitionIntentService : IntentService(TAG) {
    companion object {
        private const val TAG = "ActivityIntentService"
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent");
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                val detectedActivity = result?.mostProbableActivity
                Log.d(TAG, detectedActivity.toString())

                if (detectedActivity != null) {
                    val context = applicationContext
                    val myIntent = Intent(context, ActivityRecognitionEventService::class.java)
                    myIntent.putExtra("type", detectedActivity.type)
                    myIntent.putExtra("confidence", detectedActivity.confidence)
                    myIntent.putExtra("time", result.time)
                    context.startService(myIntent)
                    HeadlessJsTaskService.acquireWakeLockNow(context)
                }
            }
        }
    }
}
