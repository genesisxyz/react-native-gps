package com.reactnativegps

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest


class GpsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener, PermissionListener {

    companion object {
        private const val TAG = "GpsModule"

        private const val LOCATION_PERMISSIONS_REQUEST_CODE = 1
        private const val ACTIVITY_PERMISSIONS_REQUEST_CODE = 2
    }

    init {
        reactContext.addLifecycleEventListener(this);

        val app = reactContext.packageManager.getApplicationInfo(reactContext.packageName, PackageManager.GET_META_DATA)
        val bundle = app.metaData

        Places.initialize(reactContext, bundle.getString("com.google.android.geo.API_KEY")!!)
    }

    private val gpsServiceConnection: ConnectService by lazy { ConnectService(reactContext, GpsService::class.java) }

    private var mRequestLocationPermissionsPromise: Promise? = null
    private var mRequestActivityPermissionsPromise: Promise? = null

    private var isAppForeground = true

    private val placesClient = Places.createClient(reactContext);
    private var sessionToken: AutocompleteSessionToken? = null

    override fun getName(): String {
        return "Gps"
    }

    @ReactMethod
    fun setOptions(options: ReadableMap) {
        gpsServiceConnection.setOptions(options)

        Notification.updateNotification(reactContext, options.toHashMap())

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

    @ReactMethod
    fun startGpsService(promise: Promise) {
        Log.d(TAG, "GpsService starting...")
        gpsServiceConnection.startService(promise)

        watchLocationPermissions()
        watchActivityPermissions()
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

    @ReactMethod
    fun startGooglePlacesAutocompleteSession() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }

    @ReactMethod
    fun findAutocompletePredictions(query: String, options: ReadableMap, promise: Promise) {

        val northEastBounds = options.getMap("northEastBounds")
        val southWestBounds = options.getMap("southWestBounds")

        var bounds: RectangularBounds? = null

        if (northEastBounds != null && southWestBounds != null) {

            bounds = RectangularBounds.newInstance(
                    LatLng(northEastBounds.getDouble("latitude"), northEastBounds.getDouble("longitude")),
                    LatLng(southWestBounds.getDouble("latitude"), southWestBounds.getDouble("longitude"))
            )
        }

        val request =
                FindAutocompletePredictionsRequest.builder()
                        .setLocationBias(bounds)
                        .setOrigin(LatLng(-33.8749937, 151.2041382))
                        .setCountries("IT")
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(sessionToken)
                        .setQuery(query)
                        .build()

        // val results = mutableListOf<HashMap<String, Any>>()
        val results = Arguments.createArray()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->

            for (prediction in response.autocompletePredictions) {

                val result = Arguments.createMap()

                result.putString("attributedFullText", prediction.getFullText(null).toString())
                result.putString("attributedPrimaryText", prediction.getPrimaryText(null).toString())
                result.putString("attributedSecondaryText", prediction.getSecondaryText(null).toString())
                result.putString("placeID", prediction.placeId)
                // result.putArray("types", prediction.placeTypes)

                results.pushMap(result)
            }


            Log.d(TAG, results.toString())

            promise.resolve(results)

        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e(TAG, "Place not found: $exception")
            }

            promise.resolve(results)
        }
    }

    @ReactMethod
    fun getPredictionByPlaceId(placeId: String, promise: Promise) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { response ->

            val result = Arguments.createMap()

            val coordinate = Arguments.createMap()
            coordinate.putDouble("latitude", response.place.latLng!!.latitude)
            coordinate.putDouble("longitude", response.place.latLng!!.longitude)

            result.putString("name", response.place.name)
            result.putString("placeID", response.place.id)
            result.putString("formattedAddress", response.place.address)
            result.putMap("coordinate", coordinate)

            promise.resolve(result)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e(TAG, "Place not found: $exception")
            }

            promise.resolve(null)
        }
    }

    private fun watchLocationPermissions() {
        var granted = false
        currentActivity?.run {
            granted = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        val myIntent = Intent(reactContext, LocationPermissionEventService::class.java)
        myIntent.putExtra("granted", granted)

        reactContext.startService(myIntent)
        HeadlessJsTaskService.acquireWakeLockNow(reactContext)
    }

    private fun watchActivityPermissions() {
        var granted = false
        currentActivity?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                granted = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else {
                granted = ActivityCompat.checkSelfPermission(applicationContext, "com.google.android.gms.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_GRANTED
            }
        }
        val myIntent = Intent(reactContext, ActivityPermissionEventService::class.java)
        myIntent.putExtra("granted", granted)

        reactContext.startService(myIntent)
        HeadlessJsTaskService.acquireWakeLockNow(reactContext)
    }

    // region LifecycleEventListener

    override fun onHostResume() {
        isAppForeground = true
        gpsServiceConnection.onHostResume()
        // Notification.removeNotification()

        watchLocationPermissions()
        watchActivityPermissions()
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
            watchLocationPermissions()
        } else if (requestCode == ACTIVITY_PERMISSIONS_REQUEST_CODE) {
            promise = mRequestActivityPermissionsPromise
            watchActivityPermissions()
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
