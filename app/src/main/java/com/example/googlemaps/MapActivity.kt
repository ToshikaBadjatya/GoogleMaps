package com.example.googlemaps

import android.Manifest
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.example.googlemaps.data.CurrentUser
import com.example.googlemaps.data.LatLong
import com.example.googlemaps.data.User
import com.example.googlemaps.data.UserLocation
import com.example.googlemaps.services.Locationservice
import com.example.googlemaps.services.Service2
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TransitMode
import com.google.maps.model.TravelMode
import java.util.concurrent.TimeUnit

class MapActivity : AppCompatActivity(),OnMapReadyCallback,GoogleMap.OnInfoWindowClickListener,GoogleMap.OnPolylineClickListener{
    lateinit var mapView: MapView
    lateinit var viewRoute:Button
    lateinit var duration: Button
    lateinit var mapLayout: ConstraintLayout
    lateinit var source:Spinner
    lateinit var destination:Spinner
    lateinit var googleMap: GoogleMap
    lateinit var database:FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var currentUser: CurrentUser
    var geoApiContext:GeoApiContext?=null
    private var map_state=Constant.STATE_CONTRACT
    var pathVisible=false
    var prevPolyline:Polyline?=null
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
     val broadcastReceiver=object :BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            if(intent?.action==Constant.FETCH_LOCATION)
            {
                Log.e("location","${intent.getStringExtra("location")}")
                Toast.makeText(this@MapActivity,"The location is ${intent.getStringExtra("location")}",Toast.LENGTH_SHORT).show()
            }
            if(intent?.action==Constant.UPDATE)
            {
                Handler().postDelayed(
                    {
                        addMarkers()
                    },2000
                )
            }

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
         initUi()
         mapView.onCreate(savedInstanceState)
         mapView.getMapAsync(this)
        if(geoApiContext==null)
        {
            geoApiContext=GeoApiContext.Builder().apiKey(R.string.api_key.toString()).
            readTimeout(2,TimeUnit.SECONDS).writeTimeout(2,TimeUnit.SECONDS).
            connectTimeout(1,TimeUnit.SECONDS).build()

        }
        currentUser= CurrentUser()
        val intentFilter=IntentFilter()
        intentFilter.addAction(Constant.FETCH_LOCATION)
        intentFilter.addAction(Constant.UPDATE)
       registerReceiver(broadcastReceiver, intentFilter)
        database=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)

    }

  fun initUi()
  {
      mapLayout=findViewById(R.id.c2)
      mapView=findViewById(R.id.mapView)
      viewRoute=findViewById(R.id.viewroute)
      duration=findViewById(R.id.duration)
      source=findViewById(R.id.source)
      destination=findViewById(R.id.destination)
      val expand=findViewById<ImageView>(R.id.expand)
      expand.setOnClickListener {
          if(map_state==Constant.STATE_EXPAND)
          {
              contractMap()
              map_state=Constant.STATE_CONTRACT
          }
          else
          {
              expandMap()
              map_state=Constant.STATE_EXPAND
          }
      }
      viewRoute.setOnClickListener {
          if(source.selectedItem==null||destination.selectedItem==null)
          {
              Toast.makeText(this,"Select source and destination to view route",Toast.LENGTH_SHORT).show()
          }
          else
          {
              val sourceLatLong=currentUser.getUserMap()?.get(source.selectedItem.toString())
              val destinationLatLong=currentUser.getUserMap()?.get(destination.selectedItem.toString())
              if(sourceLatLong!=null&&destinationLatLong!=null)
              {
                  showDirection(com.google.maps.model.LatLng(sourceLatLong!!.latitude,sourceLatLong!!.longitude),com.google.maps.model.LatLng(destinationLatLong!!.latitude,destinationLatLong!!.longitude))
              }
          }
      }
      duration.setOnClickListener {
          showDuration()
      }
  }


    override fun onMapReady(map: GoogleMap) {
      googleMap=map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        map.isMyLocationEnabled=true
        val uiSettings=map.uiSettings
        uiSettings.isZoomGesturesEnabled=true
        map.setOnInfoWindowClickListener(this)
        getCurrentLocation()
      setUserList()


      //  moveCamera()
        Handler().postDelayed(
            {
                addMarkers()
                setUpFields()
            },2000
        )

        val intent=Intent(this, Service2::class.java)
        intent.action=Constant.UPDATE
        startService(intent)

    }

    private fun setUpFields() {
        Log.e("location","setUp fields called")
        currentUser.getUserList()?.let{list->
            Log.e("location","list not empty")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,list)
            source.adapter=adapter
            source.onItemSelectedListener =object:AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    val selected=parent!!.getItemAtPosition(position) as String
                    val deslist= mutableListOf<String>()
                    list.forEach{
                        if(it!=selected) {
                            deslist.add(it)
                        }
                    }
                   destination.adapter=ArrayAdapter(this@MapActivity,android.R.layout.simple_spinner_dropdown_item,deslist)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }
        }



        }



    private fun getCurrentLocation() {
//        val intent= Intent(this,Locationservice::class.java)
//        intent.action=Constant.FETCH_LOCATION
//        startService(intent)
        //        if(intent?.action==Constant.FETCH_LOCATION)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED|| ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        else {
            val task=fusedLocationProviderClient.lastLocation
            task.addOnCompleteListener {
                if(task.isSuccessful)
                {

                    Log.e("location",FirebaseAuth.getInstance().uid.toString())
                    task.result?.let{location ->

                       database.collection("Users").get().addOnCompleteListener {
                           if(it.isSuccessful)
                           {
                               val list=it.result
                               for(e in list)
                               {
                                   if(e.id==FirebaseAuth.getInstance().uid.toString())
                                   {
                                       val user= User(e.get("email").toString(),e.get("name").toString(),e.get("password").toString())
                                       currentUser.setUser(user)
                                       val userLocation= UserLocation(user, GeoPoint(location.latitude,location.longitude),null)
                                       currentUser.setLocation(userLocation.latLong)
                                       moveCamera()
                                      database.collection("UserLocation").document(e.id).set(userLocation).addOnSuccessListener {
                                        Log.e("location","User location object added")

//                                          setUserList()

                                      }

                                   }
                               }
                           }
                       }

                    }
                }
                else
                {
                    Log.e("location","task failed")
                }
            }
        }
    }

    private fun setUserList() {
      val userList= mutableListOf<String>()
        val userMap=HashMap<String,GeoPoint>()
        database.collection("Users").get().addOnCompleteListener { task->
            if(task.isSuccessful){
                task.result?.forEach {
                    val id = it.id

                    val name = it.get("name").toString()
                    userList.add(name)
//                    val location=getGeoPoint(id)
//
//                    Log.e("location","The location is ${location.latitude} ${location.longitude} $name")
                    database.collection("UserLocation").document(id).get()
                        .addOnCompleteListener { task ->
                            if (task?.isSuccessful && task.result != null) {
                                val location = task.result!!.get("latLong") as GeoPoint
                                userMap.put(name, location)
                                Log.e("location", "name is $name and location is $location")
                            }




                            currentUser.setUserList(userList)
                            currentUser.setUserMap(userMap)
                            Log.e("location", "${currentUser.getUserMap()?.size}")
                        }
                }
            }
        }

    }


    private fun moveCamera() {

        currentUser.getLocation()?.let {

            val lat=it.latitude
            val long=it.longitude
            Log.e("location","lat is $lat and long i $long")
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds(LatLng(lat-0.1,long-0.1),
                LatLng(lat+0.1,long+0.1)),0))
            Log.e("location","camera position successfully changed")

        }

    }

    private fun addMarkers() {

     currentUser.getUserMap()?.forEach{
         val position=LatLng(it.value.latitude,it.value.longitude)
         Log.e("location","The position is ${position.latitude} and ${position.longitude} for ${it.key}")
         googleMap.addMarker(MarkerOptions().position(position).title(it.key))

     }
    }
    private fun contractMap() {
        Log.e("location","contract called")
        val viewWrapper=ViewWrapper(mapLayout)
        val objectAnimator=ObjectAnimator.ofFloat(viewWrapper,"weight",100f,70f)
        objectAnimator.duration=1000
        objectAnimator.start()
    }

    private fun expandMap() {
        Log.e("location","expand called")
        val viewWrapper=ViewWrapper(mapLayout)
        val objectAnimator=ObjectAnimator.ofFloat(viewWrapper,"weight",70f,100f)
        objectAnimator.duration=1000
        objectAnimator.start()
//            val anim=ValueAnimator.ofFloat(mapLayout.we)
    }
    private fun showDirection(source: com.google.maps.model.LatLng, destination: com.google.maps.model.LatLng) {
    DirectionsApiRequest(geoApiContext).origin(source)
         .destination(destination).transitMode(TransitMode.BUS).alternatives(true).departureTimeNow().setCallback(object :PendingResult.Callback<DirectionsResult>{
             override fun onResult(result: DirectionsResult?) {
                 result?.let{result->

                   result.routes.forEach { route ->
                      val decodedPath = PolylineEncoding.decode(route.overviewPolyline!!.toString())

                       val newPath = mutableListOf<LatLng>()
                       decodedPath.forEach {
                           newPath.add(LatLng(it.lat, it.lng))
                       }
                       val polyline=googleMap.addPolyline(PolylineOptions().addAll(newPath))
                       polyline.color=R.color.grey
                       polyline.isClickable=true

                   }
                 }
             }

             override fun onFailure(e: Throwable?) {
                Toast.makeText(this@MapActivity,"Failed to fetch result",Toast.LENGTH_SHORT).show()
             }

         })


    }
    private fun showDuration(){
        if(pathVisible){

        }
    }
    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onStart() {
        mapView.onStart()
        super.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        stopLocationUpdate()
        super.onStop()
    }

    override fun onPause() {

        mapView.onPause()
        stopLocationUpdate()
        super.onPause()
    }

    private fun stopLocationUpdate() {

        val intent=Intent(this,Service2::class.java)
        intent.action=Constant.STOP_UPDATE
        startService(intent)
    }

    override fun onDestroy() {
        mapView.onDestroy()
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onInfoWindowClick(marker: Marker) {
       val alert=AlertDialog.Builder(this)
        with(alert){
          setMessage("Do you want to view exact location of ${marker.title}")
            val latLng=marker.position
            setNegativeButton("No"){dialogInterface, which->
            }
            setPositiveButton("Yes"){dialogInterface, which->
                showExactLocation(latLng.latitude,latLng.longitude)
            }
            create().show()
        }

    }

    private fun showExactLocation(latitude: Double, longitude: Double) {
        val intent=Intent(this, Locationservice::class.java)
        intent.action=Constant.FETCH_LOCATION
        intent.putExtra("location", LatLong(latitude,longitude))
        startService(intent)
    }

    override fun onPolylineClick(polyline: Polyline) {
        if(prevPolyline!=null)
        {
            prevPolyline!!.color=R.color.grey
            prevPolyline!!.zIndex=0f
        }
        polyline.color=R.color.purple_700
        polyline.zIndex=1f
        prevPolyline=polyline

    }
}