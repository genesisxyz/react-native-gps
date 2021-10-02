package com.reactnativegps

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class GpsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener, PermissionListener {

    companion object {
        const val TAG = "GpsModule"
    }

    init {
        reactContext.addLifecycleEventListener(this);
    }

    private var mBound = false

    private var mService: LocationUpdatesService? = null

    private var mStartPromise: Promise? = null

    private var mRequestPermissionsPromise: Promise? = null

    private var intentService: Intent? = null

    // TODO: how to initialize it inline?
    private var options: ReadableMap? = null;

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            Log.i(TAG, "Intent service started")
            Log.i(TAG, "onServiceConnected")

            val binder: LocationUpdatesService.LocationUpdatesBinder = service as LocationUpdatesService.LocationUpdatesBinder
            mService = binder.service
            mBound = true
            mStartPromise?.resolve(null)
            mStartPromise = null
            mService?.updateOptions(options?.toHashMap())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // TODO: why is this never called?
            Log.i(TAG, "onServiceDisconnected")
            mBound = false
        }
    }

    override fun getName(): String {
        return "Gps"
    }

    @ReactMethod
    fun setOptions(options: ReadableMap) {
        this.options = options
        mService?.updateOptions(options.toHashMap())
    }

    @ReactMethod
    fun startService(promise: Promise) {
        if (intentService == null) {
            intentService = Intent(reactContext, LocationUpdatesService::class.java)
            intentService?.putExtra("options", options?.toHashMap());
            mStartPromise = promise
            reactContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reactContext.startService(intentService)
            } else {
                reactContext.startService(intentService)
            }
        } else {
            Log.w(TAG, "Intent service is running")
            promise.resolve(null)
        }
    }

    @ReactMethod
    fun stopService(promise: Promise) {
        if (intentService != null) {
            reactContext.unbindService(connection)
            reactContext.stopService(intentService)
            //
            // mBound = false
            mService = null
            intentService = null
            Log.i(TAG, "Intent service stopped")
        } else {
            Log.w(TAG, "Intent service is not running")
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun requestLocationUpdates(promise: Promise) {
        if (mService != null) {
            mService?.requestLocationUpdates()?.addOnSuccessListener {
                Log.i(TAG, "requestLocationUpdates success")
                promise.resolve(null);
            }?.addOnFailureListener {
                Log.e(TAG, "requestLocationUpdates failure $it")
                promise.reject(it)
            }
        } else {
            promise.reject(Exception("Location updates service not available"))
        }
    }

    @ReactMethod
    fun removeLocationUpdates(promise: Promise) {
        if (mService != null) {
            mService?.removeLocationUpdates()?.addOnSuccessListener {
                Log.i(TAG, "removeLocationUpdates success")
                promise.resolve(null);
            }?.addOnFailureListener {
                Log.e(TAG, "removeLocationUpdates failure $it")
                promise.reject(it)
            }
        } else {
            promise.resolve(null)
        }
    }

    @ReactMethod
    fun requestPermissions(promise: Promise) {
        currentActivity?.run {
            if (ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mRequestPermissionsPromise = promise
                this as PermissionAwareActivity
                requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1, // TODO: what is requestCode?
                        this@GpsModule
                );
            } else {
                promise.resolve(null);
            }
        }
    }

    // region LifecycleEventListener

    override fun onHostResume() {
        Log.i(TAG, "onHostResume")
        if (intentService != null) { // TODO: can't check mBound, onServiceDisconnected never called
            reactContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            mBound = true // TODO: shouldn't be required
        }
    }

    override fun onHostPause() {
        Log.i(TAG, "onHostPause")
        if (mBound) {
            reactContext.unbindService(connection)
            mBound = false // TODO: shouldn't be required
        }
    }

    override fun onHostDestroy() {
        Log.i(TAG, "onHostDestroy")
    }

    // endregion

    // region ActivityCompat.OnRequestPermissionsResultCallback

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        Log.i(TAG, "onRequestPermissionsResult $requestCode $permissions $grantResults")
        mRequestPermissionsPromise?.run {
            val allGranted = grantResults?.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted == true) {
                resolve(null)
            } else {
                reject(java.lang.Exception("Deny"))
            }
        }
        mRequestPermissionsPromise = null
        return true
    }

    // endregion
}
