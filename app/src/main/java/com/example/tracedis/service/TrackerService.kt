package com.example.tracedis.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.tracedis.LocData
import com.example.tracedis.ui.maps.MapUtil.calculateTheDistance
import com.example.tracedis.util.Constants.ACTION_SERVICE_START
import com.example.tracedis.util.Constants.ACTION_SERVICE_STOP
import com.example.tracedis.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.tracedis.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.tracedis.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.tracedis.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.tracedis.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.SphericalUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


@AndroidEntryPoint
class TrackerService: LifecycleService() {


    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    var user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    companion object {

        val started = MutableLiveData<Boolean>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()

        val locationList  = MutableLiveData<MutableList<LatLng>>()
        val fslocationList  = ArrayList<LatLng>()

    }
    fun getAddress(loc:LatLng): String{
        var addr=""
        var yourAddresses: List<Address>
        val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
        yourAddresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)

        if (yourAddresses.isNotEmpty()) {
            //if(yourAddresses[0])
            val yourAddress: String = yourAddresses[0].getAddressLine(0)
//            val yourCity: String = yourAddresses[0].getAddressLine(1)
//            val yourCountry: String = yourAddresses[0].getAddressLine(2)
          //  addr= "$yourAddress, $yourCity, $yourCountry. "
            addr="$yourAddress"
        }
        return addr
    }
    private fun updateLocationList(location: Location){
        val newLatLng = LatLng(location.latitude,location.longitude)
        locationList.value?.apply {
            //if new loc is greater that 500m from last loc then add
            //to firestore
            var nLoc= LocData()
            if(locationList.value!!.size==1){
                fslocationList.add(newLatLng)
                //add to firestore
                nLoc.userId = user!!.uid
                nLoc.email =user!!.email!!
                nLoc.lat = newLatLng.latitude
                nLoc.lng=newLatLng.longitude
                nLoc.address = getAddress(newLatLng)
                nLoc.longDate = Calendar.getInstance().time.time
                nLoc.strDate = Calendar.getInstance().time.toString()

                firestore.collection("locations").add(nLoc).addOnSuccessListener {

                }


            }else if(locationList.value!!.size>1){
                //compare newlatlng w last latlng add if distance is greater than 500m
                var dist=0.0
                var lastlatlng = fslocationList.last()
                dist =  SphericalUtil.computeDistanceBetween(lastlatlng, newLatLng);
                Log.d("TRACKER SERVICE", "updateLocationList: DISTANCE BETWEEN  $dist")
                if(dist>20) {
                    fslocationList.add(newLatLng)
                    nLoc.userId = user!!.uid
                    nLoc.email =user!!.email!!
                    nLoc.lat = newLatLng.latitude
                    nLoc.lng=newLatLng.longitude
                    nLoc.address = getAddress(newLatLng)
                    nLoc.longDate = Calendar.getInstance().time.time
                    nLoc.strDate = Calendar.getInstance().time.toString()
                    firestore.collection("locations").add(nLoc).addOnSuccessListener {

                    }
                }
            }


            add(newLatLng)
            locationList.postValue(this)
        }
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.let { locations ->
                for (location in locations){
                    updateLocationList(location)
                    updateNotificationPeriodically()
//                    val newLatLng = LatLng(location.latitude, location.longitude)
//                    Log.d("TrackerService",newLatLng.toString())
                }

            }
        }
    }

    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
            setContentText(locationList.value?.let { calculateTheDistance(it) } +"km")
        }
        notificationManager.notify(NOTIFICATION_ID,notification.build())
    }

    private fun setInitialValues(){
        started.postValue(false)
        startTime.postValue(0L)
        stopTime.postValue(0L)

        locationList.postValue(mutableListOf())
    }

    override fun onCreate() {
        setInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_START ->{
                    started.postValue(true)
                    startForegroundService()
                    startLocationUpdates()
                }
                ACTION_SERVICE_STOP ->{
                    started.postValue(false)
                    stopForegroundService()
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopForegroundService() {
        removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }



    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }


    private fun startForegroundService(){
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            notification.build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
//        val locationRequest = LocationRequest().apply {
//            interval = LOCATION_UPDATE_INTERVAL
//            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        startTime.postValue(System.currentTimeMillis())
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

}