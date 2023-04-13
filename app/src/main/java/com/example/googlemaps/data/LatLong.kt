package com.example.googlemaps.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LatLong(val lattitude:Double,val longitude:Double) : Parcelable