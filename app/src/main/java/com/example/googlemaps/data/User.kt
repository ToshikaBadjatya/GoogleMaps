package com.example.googlemaps.data

import com.google.firebase.firestore.GeoPoint
import javax.inject.Singleton

@Singleton
data class User(val email:String,val name:String,val password:String)
data class UserData(val name:String,val geoPoint: GeoPoint)