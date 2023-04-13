package com.example.googlemaps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.audiofx.Equalizer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.example.googlemaps.data.User
import com.example.googlemaps.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.internal.GoogleApiManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Connection

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var database:FirebaseFirestore
    lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth=FirebaseAuth.getInstance()
        database= FirebaseFirestore.getInstance()
        if(auth.currentUser!=null)
        {
            val intent=Intent(this,MapActivity::class.java)
            startActivity(intent)
        }
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val button=findViewById<Button>(R.id.button)
        button.setOnClickListener {
            it?.let{
                getPermission(it)
            }

        }

    }

    private fun getPermission(view: View) {
      if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)
      {
          Log.e("location","app has location permisssion")
          init()
      }
        else
      {
          seekPermission(view)
      }
    }

    private fun seekPermission(view: View) {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_FINE_LOCATION)&&ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)){
            val snack=Snackbar.make(view,"Location Permission",Snackbar.LENGTH_INDEFINITE)
            snack.setText("The app require location permission")
            snack.setAction("Ok"){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION),Constant.LOCATION_PERMISSION)
            }.show()
        }
        else
        {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION),Constant.LOCATION_PERMISSION)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==Constant.LOCATION_PERMISSION&&grantResults.size==1&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            init()
        }
        else
        {
            Toast.makeText(this,"Cannot continue with the app",Toast.LENGTH_SHORT).show()
            System.exit(0)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun init(){
        Log.e("location","init called")
     val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)&&isReady())
        {

            createAccount(binding.name.text.toString(),binding.email.text.toString(),binding.password.text.toString())
            val intent=Intent(this,MapActivity::class.java)
            startActivity(intent)
            //move to next activity
        }
        else{
            val intent=Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent,Constant.GPS_PERMISSION)
        }
    }

    private fun createAccount(name:String,email:String,password:String) {
      val task=auth.createUserWithEmailAndPassword(email,password)
        task?.addOnCompleteListener{
            if(task.isSuccessful){

                Log.e("location","user created")
                val user= User(email,name,password)
                Log.e("location",auth.uid.toString())
                Log.e("location",FirebaseAuth.getInstance().uid.toString())
                val task2=database.collection("Users").document(FirebaseAuth.getInstance().uid.toString()).set(user)
                task2.addOnSuccessListener{
                    Log.e("location","User object  successfully created")
                    val intent=Intent(this,MapActivity::class.java)
                    startActivity(intent)
                }
            }

        }
    }

    private fun isReady(): Boolean {
  val googleService=GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return googleService==ConnectionResult.SUCCESS
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode==Constant.GPS_PERMISSION&&resultCode==PackageManager.PERMISSION_GRANTED)
        {
            init()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}