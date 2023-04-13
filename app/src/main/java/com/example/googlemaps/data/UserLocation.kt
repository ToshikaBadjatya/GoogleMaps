package com.example.googlemaps.data


import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*


data class UserLocation(val user: User, val latLong: GeoPoint, @ServerTimestamp var timeStamp:  Date?=null)