package com.example.googlemaps.data


import com.google.firebase.firestore.GeoPoint


class CurrentUser {
    private var currentUser: User?=null
    private var currentLocation:GeoPoint?=null
    private var userList:MutableList<String> = mutableListOf()
    private var userMap:HashMap<String, GeoPoint> = HashMap()
    fun setUser(user: User)
    {
        currentUser=user
    }
    fun getUser(): User?
    {
        return currentUser
    }
    fun setLocation(location:GeoPoint)
    {
        currentLocation=location
    }
    fun getLocation():GeoPoint?
    {
        return currentLocation
    }
    fun setUserList(list:MutableList<String>){
        userList=list
    }
    fun getUserList(): MutableList<String>? {
        return userList
    }

    fun setUserMap(map: HashMap<String, GeoPoint>) {
       userMap=map
    }

    fun getUserMap():HashMap<String, GeoPoint>? {
      return userMap
    }
}