package com.reactnativegps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

interface ServiceBinderInterface: IBinder {
    val service: ServiceInterface
}

interface ServiceInterface {
    fun updateOptions(options: HashMap<String, Any>?)
}

class ConnectService(private val reactContext: ReactApplicationContext, private val serviceClass: Class<*>) {
    companion object {
        const val TAG = "ConnectService"
    }

    private var mBound = false

    var mService: ServiceInterface? = null
        private set

    private var mStartPromise: Promise? = null

    private var intentService: Intent? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            Log.i(TAG, "Intent service started")
            Log.i(TAG, "onServiceConnected")

            val binder: ServiceBinderInterface = service as ServiceBinderInterface
            mService = binder.service
            mBound = true
            mStartPromise?.resolve(null)
            mStartPromise = null
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // TODO: why is this never called?
            Log.i(TAG, "onServiceDisconnected")
            mBound = false
        }
    }

    fun setOptions(options: ReadableMap) {
        mService?.updateOptions(options.toHashMap())
    }

    fun startService(promise: Promise) {
        if (intentService == null) {
            intentService = Intent(reactContext, serviceClass)
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

    fun stopService(promise: Promise) {
        if (intentService != null) {
            reactContext.unbindService(connection)
            reactContext.stopService(intentService)
            mBound = false
            mService = null
            intentService = null
            Log.i(TAG, "Intent service stopped")
        } else {
            Log.w(TAG, "Intent service is not running")
        }
        promise.resolve(null)
    }

    fun onHostResume() {
        Log.i(TAG, "onHostResume")
        if (intentService != null) { // TODO: can't check mBound, onServiceDisconnected never called
            reactContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            mBound = true // TODO: shouldn't be required
        }
    }

    fun onHostPause() {
        Log.i(GpsModule.TAG, "onHostPause")
        if (mBound) {
            reactContext.unbindService(connection)
            mBound = false // TODO: shouldn't be required
        }
    }

    fun onHostDestroy() {
        Log.i(TAG, "onHostDestroy")
    }
}