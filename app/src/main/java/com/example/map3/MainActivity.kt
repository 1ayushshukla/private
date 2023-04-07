package com.example.map3

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import java.nio.file.attribute.AclEntry.Builder
import java.util.SimpleTimeZone

class MainActivity : AppCompatActivity() , OnMapReadyCallback{

    private var mMap: GoogleMap? = null
    lateinit var mapView: MapView
    private var MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"


    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false


    // ZOOM LEVELS - 1 - World, 5 - Landmass/continent, 10 - City, 15 - Streets, 20 - Buildings
    private val DEFAULT_ZOOM = 15f
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null


    override fun onMapReady(p0: GoogleMap) {
        mapView.onResume() //resume the map state in mob phn
        mMap = p0

        requestPermission()

        if(ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )!= PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap!!.isMyLocationEnabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById<MapView>(R.id.map1)

        //-------------------------PERMISSION BLOCK BEGIN--------------------------//
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            isFineLocationPermissionGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: isFineLocationPermissionGranted
            isCoarseLocationPermissionGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: isCoarseLocationPermissionGranted
        }
        requestPermission()
        //-------------------------PERMISSION BLOCK END--------------------------//

        var mapViewBundle: Bundle? = null
        if(savedInstanceState != null){
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //-------------------------PERMISSION BLOCK BEGIN--------------------------//
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            isFineLocationPermissionGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: isFineLocationPermissionGranted
            isCoarseLocationPermissionGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: isCoarseLocationPermissionGranted
        }
        requestPermission()
        //-------------------------PERMISSION BLOCK END--------------------------//

        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if(mapViewBundle == null){
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }





    //-------------------------PERMISSION FUN BEGIN--------------------------//
    private fun requestPermission(){

        isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val permissionRequest : MutableList<String> = ArrayList()

        if(!isCoarseLocationPermissionGranted){
            permissionRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if(!isFineLocationPermissionGranted){
            permissionRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if(permissionRequest.isNotEmpty()){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
        getCurrentLocation()
    }
    //-------------------------PERMISSION FUN END--------------------------//




    //-------------------------LOCATION FUN BEGIN--------------------------//
    private fun getCurrentLocation() {

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MainActivity)

        try {
            @SuppressLint("MissingPermission")
            val location =
                fusedLocationProviderClient!!.lastLocation

            location.addOnCompleteListener(object : OnCompleteListener<Location> {
                override fun onComplete(loc: Task<Location>) {
                    if(loc.isSuccessful){
                        val currentLocation = loc.result as Location?
                        if(currentLocation != null){
                            Toast.makeText(this@MainActivity, "Current Location Found.", Toast.LENGTH_SHORT).show()
                            //call the moveCamera() function
                            moveCamera(LatLng(currentLocation.latitude, currentLocation.longitude), DEFAULT_ZOOM)
                        }
                    }else {
                        Toast.makeText(this@MainActivity, "Current Location Not Found.", Toast.LENGTH_SHORT).show()
                        requestPermission()
                    }
                }
            })
        }catch (se: Exception){
            Log.e("TAG", "Security Exception")
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float){
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        mMap!!.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("You're Here")
        )
    }
    //-------------------------LOCATION FUN END--------------------------//


}