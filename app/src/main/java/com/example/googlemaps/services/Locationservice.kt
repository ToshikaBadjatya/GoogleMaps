package com.example.googlemaps.services
import android.app.IntentService
import android.content.Intent
import android.location.Geocoder
import android.util.Log
import com.example.googlemaps.Constant
import com.example.googlemaps.data.LatLong
import java.util.*

class Locationservice():IntentService("Location") {
    override fun onHandleIntent(intent: Intent?) {
          if(intent!=null&&intent?.action==Constant.FETCH_LOCATION){

            val latLong: LatLong? = intent.getParcelableExtra<LatLong>("location")
            val geocoder = Geocoder(this, Locale.getDefault())
            latLong?.let { latLong ->

                val exactLocation = geocoder.getFromLocation(latLong.lattitude, latLong.longitude, 1)
                exactLocation?.get(0)?.let {
                    Intent().also { intent ->
                        intent.action = Constant.FETCH_LOCATION
                        intent.putExtra("location", it.getAddressLine(0).toString())
                        sendBroadcast(intent)
                        Log.e("location", "broadcast send")
                    }
                }

            }


        }
    }
}