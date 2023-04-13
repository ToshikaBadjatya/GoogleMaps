package com.example.googlemaps.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.googlemaps.Constant
import com.example.googlemaps.MapActivity
import com.example.googlemaps.data.CurrentUser
import com.example.googlemaps.data.UserLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.Polyline
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint


class Service2: IntentService("Updates") {
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    val locationCallback=object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.e("location","Location update request received")
            locationResult.lastLocation?.let { location->
                val user= CurrentUser().getUser()
                user?.let{user->
                    val userLocation=
                        UserLocation(user, GeoPoint(location.latitude,location.longitude),null)
                    saveLocation(userLocation)
                }

            }
            super.onLocationResult(locationResult)
        }
    }



    override fun onHandleIntent(intent: Intent?) {
        intent?.let{intent->
            fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
            val locationRequest=LocationRequest()
            locationRequest.priority=Priority.PRIORITY_HIGH_ACCURACY
            locationRequest.fastestInterval=1000
            locationRequest.maxWaitTime=5000
            locationRequest.interval=2000
            val locationRequestBuilder= LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val settingClient=LocationServices.getSettingsClient(this)
            settingClient.checkLocationSettings(locationRequestBuilder.build()).addOnCompleteListener { task->
                if(task.isSuccessful&&task.result!=null)
                {
                    if(intent.action==Constant.UPDATE) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
                    }
                    else
                    {
                        Log.e("location","location updates stopped")
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null)
                    }
                }
            }

        }
    }

    private fun manageNotifications() {
        Log.e("location","manage notifications called")
        val notificationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
       {
           val channel=NotificationChannel(Constant.NOTIFICATION_CHANNEL.toString(),"My Channel", NotificationManager.IMPORTANCE_HIGH)
           channel.enableVibration(true)
           channel.enableLights(true)
           notificationManager.createNotificationChannel(channel)
       }
        val pendingIntent=PendingIntent.getActivity(this,0,Intent(this, MapActivity::class.java),0)
        val notification=NotificationCompat.Builder(this, Constant.NOTIFICATION_CHANNEL.toString())
        notification.setContentTitle("Googlemaps").setContentText("Location update service running in background")
        notification.setContentIntent(pendingIntent)
//        startForeground(Constant.NOTIFICATION_CHANNEL,notification.build())
        notificationManager.notify(Constant.NOTIFICATION_CHANNEL,notification.build())
    }
    private fun saveLocation(userLocation: UserLocation) {
        val database=FirebaseFirestore.getInstance()
        database.collection("UserLocation").document(FirebaseAuth.getInstance().uid.toString()).set(userLocation).addOnSuccessListener{
            Log.e("location","location updated")
           Intent().also{
             it.action= Constant.UPDATE
               sendBroadcast(it)
           }
        }



    }

}