package com.reactnativegps

import android.Manifest
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

    private val locationServiceConnection = ConnectService(reactContext, LocationUpdatesService::class.java)
    private val geofenceServiceConnection = ConnectService(reactContext, GeofenceUpdatesService::class.java)
    private val activityRecognitionServiceConnection = ConnectService(reactContext, ActivityRecognitionUpdatesService::class.java)

    private var mRequestLocationPermissionsPromise: Promise? = null
    private var mRequestActivityPermissionsPromise: Promise? = null

    override fun getName(): String {
        return "Gps"
    }

    @ReactMethod
    fun setOptions(options: ReadableMap) {
        locationServiceConnection.setOptions(options)
        geofenceServiceConnection.setOptions(options)
        Notification.updateNotification(reactContext.applicationContext, options.toHashMap())
    }

    @ReactMethod
    fun startLocationService(promise: Promise) {
        locationServiceConnection.startService(promise)
    }

    @ReactMethod
    fun stopLocationService(promise: Promise) {
        locationServiceConnection.stopService(promise)
    }

    @ReactMethod
    fun startGeofenceService(promise: Promise) {
        geofenceServiceConnection.startService(promise)
    }

    @ReactMethod
    fun stopGeofenceService(promise: Promise) {
        geofenceServiceConnection.stopService(promise)
    }

    @ReactMethod
    fun startActivityRecognitionService(promise: Promise) {
        activityRecognitionServiceConnection.startService(promise)
    }

    @ReactMethod
    fun stopActivityRecognitionService(promise: Promise) {
        activityRecognitionServiceConnection.stopService(promise)
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
                promise.resolve(null);
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
                promise.resolve(null);
            }
        }
    }

    @ReactMethod
    fun addGeofences(geofences: ReadableArray, promise: Promise) {
        if (geofenceServiceConnection.mService != null) {
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
                val addGeofencesTask = (geofenceServiceConnection.mService as GeofenceUpdatesService).addGeofences(list)
                addGeofencesTask?.addOnSuccessListener(this, OnSuccessListener<Void?> {
                    Log.i(TAG, "Geofences added")
                    promise.resolve(null)
                })?.addOnFailureListener(this, OnFailureListener { e ->
                    Log.i(TAG, "Geofences add failure $e")
                    promise.reject(e)
                }) ?: promise.resolve(null)
            }
        } else {
            promise.resolve(null)
        }
    }

    @ReactMethod
    fun removeGeofences(geofences: ReadableArray, promise: Promise) {
        if (geofenceServiceConnection.mService != null) {
            val list = ArrayList<String>()
            for (i in 0 until geofences.size()) {
                val geofence = geofences.getString(i)!!
                list.add(geofence)
            }

            currentActivity?.run {
                (geofenceServiceConnection.mService as GeofenceUpdatesService).removeGeofences(list)
                        ?.addOnSuccessListener(this, OnSuccessListener<Void?> {
                            Log.i(TAG, "Geofences removed")
                            promise.resolve(null)
                        })
                        ?.addOnFailureListener(this, OnFailureListener { e ->
                            Log.i(TAG, "Geofences remove failure $e")
                            promise.reject(e)
                        })
            }
        } else {
            promise.resolve(null)
        }
    }

    // region LifecycleEventListener

    override fun onHostResume() {
        locationServiceConnection.onHostResume()
        geofenceServiceConnection.onHostResume()
        Notification.removeNotification()
    }

    override fun onHostPause() {
        locationServiceConnection.onHostPause()
        geofenceServiceConnection.onHostPause()
    }

    override fun onHostDestroy() {
        locationServiceConnection.onHostDestroy()
        geofenceServiceConnection.onHostDestroy()
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
            if (allGranted == true) {
                resolve(null)
            } else {
                reject(java.lang.Exception("Deny"))
            }
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
