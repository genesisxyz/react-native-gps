package com.reactnativegps

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

data class GeofenceTransition(
    val id: String,
    var transition: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
)


class GeofenceUpdates(private val context: Context): ServiceLifecycle, ServiceInterface {
    companion object {
        private const val TAG = "GeofenceUpdates"

        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    private lateinit var mGeofencingClient: GeofencingClient
    private var mGeofenceUpdatesTask: Task<Void>? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private var mLocationUpdatesTask: Task<*>? = null

    private var geofences = mutableListOf<GeofenceTransition>()

    private var started = false

    // region ServiceLifecycle

    override fun onCreate() {

    }

    override fun onDestroy() {
        if (started) {
            if (!canAccessBackgroundLocation) {
                removeLocationUpdates()
            }
        }
        started = false
    }

    // endregion

    private val canAccessBackgroundLocation: Boolean
        get() {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    (permissions?.any { perm -> perm == Manifest.permission.ACCESS_BACKGROUND_LOCATION }
                            ?: false)
        }

    private fun requestLocationUpdates(): Task<*>? {
        Log.i(TAG, "Request location updates")
        val locationRequest = createLocationRequest()
        return try {
            mLocationUpdatesTask = mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
            mLocationUpdatesTask
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
            null
        }
    }

    private fun removeLocationUpdates(): Task<Void>? {
        Log.i(TAG, "Removing location updates")
        return try {
            val task = mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            mLocationUpdatesTask = null
            task
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
            null
        }
    }

    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")
        if (location.hasAccuracy()) {

            val oldGeofences = geofences.toList()
            val geofencesInside = mutableListOf<GeofenceTransition>();
            val geofencesOutside = mutableListOf<GeofenceTransition>();

            geofences.forEachIndexed { i, it ->
                val results = FloatArray(1)
                Location.distanceBetween(it.latitude, it.longitude, location.latitude, location.longitude, results);
                val distance = results[0]

                if (distance <= it.radius) {
                    if (oldGeofences[i].transition != Geofence.GEOFENCE_TRANSITION_ENTER) {
                        geofencesInside.add(it);
                    }

                    it.transition = Geofence.GEOFENCE_TRANSITION_ENTER
                } else {
                    if (oldGeofences[i].transition != Geofence.GEOFENCE_TRANSITION_EXIT) {
                        geofencesOutside.add(it);
                    }

                    it.transition = Geofence.GEOFENCE_TRANSITION_EXIT
                }
            }

            if (geofencesInside.size > 0) {
                val myIntent = Intent(context, GeofenceEventService::class.java)
                myIntent.putExtra("transition", Geofence.GEOFENCE_TRANSITION_ENTER)
                val ids = ArrayList<String>()
                for (geofence in geofencesInside) {
                    ids.add(geofence.id)
                }
                myIntent.putExtra("ids", ids)
                context.startService(myIntent)

                HeadlessJsTaskService.acquireWakeLockNow(context)
            }

            if (geofencesOutside.size > 0) {
                val myIntent = Intent(context, GeofenceEventService::class.java)
                myIntent.putExtra("transition", Geofence.GEOFENCE_TRANSITION_EXIT)
                val ids = ArrayList<String>()
                for (geofence in geofencesOutside) {
                    ids.add(geofence.id)
                }
                myIntent.putExtra("ids", ids)
                context.startService(myIntent)

                HeadlessJsTaskService.acquireWakeLockNow(context)
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            smallestDisplacement = 10.0f
        }
    }

    fun start() {
        started && return
        if (canAccessBackgroundLocation) {
            mGeofencingClient = LocationServices.getGeofencingClient(context)
        } else {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    onNewLocation(locationResult.lastLocation)
                }
            }

            requestLocationUpdates()
        }
        started = true
    }

    fun addGeofences(geofences: List<Bundle>): Task<Void>? {
        this.geofences.addAll(geofences.map {
            GeofenceTransition(
                    id = it.getString("id")!!,
                    latitude = it.getDouble("latitude"),
                    longitude = it.getDouble("longitude"),
                    radius = it.getFloat("radius")

            )
        })

        if (!canAccessBackgroundLocation) return null;

        val geofenceList: MutableList<Geofence> = ArrayList()

        for (geofence in this.geofences) {
            geofenceList.add(
                    Geofence.Builder()
                            .setRequestId(geofence.id)
                            .setCircularRegion(geofence.latitude, geofence.longitude, geofence.radius)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setLoiteringDelay(1000 * 60 * 5) // 5 minutes
                            .setTransitionTypes(
                                    Geofence.GEOFENCE_TRANSITION_ENTER or
                                            Geofence.GEOFENCE_TRANSITION_EXIT or
                                            Geofence.GEOFENCE_TRANSITION_DWELL
                            )
                            .build()
            )
        }
        val builder = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofenceList)
                .build()
        val intent = Intent(context, GeofenceIntentService::class.java)
        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGeofenceUpdatesTask = mGeofencingClient?.addGeofences(builder, pendingIntent)
            return mGeofenceUpdatesTask
        }
        return null
    }

    fun removeGeofences(geofences: List<String>): Task<Void>? {
        if (!canAccessBackgroundLocation) {
            this.geofences = mutableListOf()
            return null
        }
        return mGeofencingClient?.removeGeofences(geofences)
    }

    // region ServiceInterface

    override fun updateOptions(options: HashMap<String, Any>?) {

    }

    // endregion
}
