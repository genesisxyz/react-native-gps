package com.reactnativegps

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class GpsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener, PermissionListener {

    companion object {
        private const val TAG = "GpsModule"

        private const val LOCATION_PERMISSIONS_REQUEST_CODE = 1
        private const val ACTIVITY_PERMISSIONS_REQUEST_CODE = 2
    }

    init {
        reactContext.addLifecycleEventListener(this);
    }

    private val gpsServiceConnection: ConnectService by lazy { ConnectService(reactContext, GpsService::class.java) }

    private var mRequestLocationPermissionsPromise: Promise? = null
    private var mRequestActivityPermissionsPromise: Promise? = null

    private var isAppForeground = true

    override fun getName(): String {
        return "Gps"
    }

    @ReactMethod
    fun setOptions(options: ReadableMap) {
        gpsServiceConnection.setOptions(options)

        Notification.updateNotification(reactContext, options.toHashMap())
        if (!isAppForeground) {
            Notification.notificationBuilder?.get()?.run {
                setContentTitle(Notification.notificationContentTitle)
                setContentText(Notification.notificationContentText)

                val smallIconId = Notification.getSmallIconId(reactContext)
                if (smallIconId != 0) {
                    setSmallIcon(smallIconId);
                }

                val notificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                notificationManager.notify(
                        Notification.notificationId,
                        build()
                );
            }
        }
    }

    @ReactMethod
    fun startGpsService(promise: Promise) {
        Log.d(TAG, "GpsService starting...")
        gpsServiceConnection.startService(promise)
    }

    @ReactMethod
    fun stopGpsService(promise: Promise) {
        Log.d(TAG, "GpsService stopping...")
        gpsServiceConnection.stopService(promise)
    }

    @ReactMethod
    fun startLocationUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            locationUpdates?.start()
        }
    }

    @ReactMethod
    fun startGeofenceUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            geofenceUpdates?.start()
        }
    }

    @ReactMethod
    fun startActivityRecognitionUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            activityRecognitionUpdates?.start()
        }
    }

    @ReactMethod
    fun stopLocationUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            locationUpdates?.onDestroy() // TODO: better to create onStop()?
        }
    }

    @ReactMethod
    fun stopGeofenceUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            geofenceUpdates?.onDestroy() // TODO: better to create onStop()?
        }
    }

    @ReactMethod
    fun stopActivityRecognitionUpdates() {
        gpsServiceConnection.mService?.run {
            this as GpsService
            activityRecognitionUpdates?.onDestroy() // TODO: better to create onStop()?
        }
    }

    @ReactMethod
    fun lastLocation(promise: Promise) {
        gpsServiceConnection.mService?.run {
            this as GpsService
            if (locationUpdates?.lastLocation != null) {
                locationUpdates?.lastLocation?.addOnCompleteListener { task ->
                    Log.i(TAG, task.toString())
                    if (task.isSuccessful && task.result != null) {
                        promise.resolve(task.result.toMap())
                    } else {
                        promise.resolve(null)
                    }
                }
            } else {
                promise.resolve(null)
            }
        }
    }

    @ReactMethod
    fun requestLocationPermissions(promise: Promise) {
        currentActivity?.run {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mRequestLocationPermissionsPromise = promise
                this as PermissionAwareActivity
                requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSIONS_REQUEST_CODE,
                        this@GpsModule
                );
            } else {
                promise.resolve(true);
            }
        }
    }

    @ReactMethod
    fun requestActivityPermissions(promise: Promise) {
        currentActivity?.run {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(applicationContext, "com.google.android.gms.permission.ACTIVITY_RECOGNITION") != PackageManager.PERMISSION_GRANTED) {
                mRequestActivityPermissionsPromise = promise
                this as PermissionAwareActivity
                val strings = mutableListOf("com.google.android.gms.permission.ACTIVITY_RECOGNITION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    strings.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                requestPermissions(
                        strings.toTypedArray(),
                        ACTIVITY_PERMISSIONS_REQUEST_CODE,
                        this@GpsModule
                );
            } else {
                promise.resolve(true);
            }
        }
    }

    @ReactMethod
    fun addGeofences(geofences: ReadableArray, promise: Promise) {
        if (gpsServiceConnection.mService != null) {
            val list = ArrayList<Bundle>()
            for (i in 0 until geofences.size()) {
                val bundle = Bundle()
                val map = geofences.getMap(i)!!
                bundle.putString("id", map.getString("id"))
                bundle.putDouble("latitude", map.getDouble("latitude"))
                bundle.putDouble("longitude", map.getDouble("longitude"))
                bundle.putFloat("radius", map.getDouble("radius").toFloat())
                list.add(bundle)
            }

            currentActivity?.run {
                val geofencesTask = (gpsServiceConnection.mService as GpsService).geofenceUpdates?.addGeofences(list)
                geofencesTask?.addOnSuccessListener(this, OnSuccessListener<Void?> {
                    Log.i(TAG, "Geofences added")
                    promise.resolve(true)
                })?.addOnFailureListener(this, OnFailureListener { e ->
                    Log.i(TAG, "Geofences add failure $e")
                    promise.resolve(false)
                }) ?: promise.resolve(true)
            }
        } else {
            promise.resolve(false)
        }
    }

    @ReactMethod
    fun removeGeofences(geofences: ReadableArray, promise: Promise) {
        if (gpsServiceConnection.mService != null) {
            val list = ArrayList<String>()
            for (i in 0 until geofences.size()) {
                val geofence = geofences.getString(i)!!
                list.add(geofence)
            }

            currentActivity?.run {
                val geofencesTask = (gpsServiceConnection.mService as GpsService).geofenceUpdates?.removeGeofences(list)
                geofencesTask?.addOnSuccessListener(this, OnSuccessListener<Void?> {
                    Log.i(TAG, "Geofences removed")
                    promise.resolve(true)
                })
                ?.addOnFailureListener(this, OnFailureListener { e ->
                    Log.i(TAG, "Geofences remove failure $e")
                    promise.resolve(false)
                }) ?: promise.resolve(false)
            }
        } else {
            promise.resolve(false)
        }
    }

    // region LifecycleEventListener

    override fun onHostResume() {
        isAppForeground = true
        gpsServiceConnection.onHostResume()
        Notification.removeNotification()
    }

    override fun onHostPause() {
        isAppForeground = false
        gpsServiceConnection.onHostPause()
    }

    override fun onHostDestroy() {
        isAppForeground = false
        gpsServiceConnection.onHostDestroy()
    }

    // endregion

    // region ActivityCompat.OnRequestPermissionsResultCallback

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        Log.i(TAG, "onRequestPermissionsResult $requestCode $permissions $grantResults")

        var promise: Promise? = null
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            promise = mRequestLocationPermissionsPromise
        } else if (requestCode == ACTIVITY_PERMISSIONS_REQUEST_CODE) {
            promise = mRequestActivityPermissionsPromise
        }

        promise?.run {
            val allGranted = grantResults?.all { it == PackageManager.PERMISSION_GRANTED }
            resolve(allGranted)
        }

        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            mRequestLocationPermissionsPromise = null
        } else if (requestCode == ACTIVITY_PERMISSIONS_REQUEST_CODE) {
            mRequestActivityPermissionsPromise = null
        }

        return true
    }

    // endregion
}
