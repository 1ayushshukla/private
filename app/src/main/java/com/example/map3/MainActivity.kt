package com.example.map3

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() , OnMapReadyCallback, LocationListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener{

    private var mMap: GoogleMap? = null
    lateinit var mapView: MapView
    private var MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"

    lateinit var tvCurrentAddress: TextView


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
        mMap!!.setOnCameraMoveListener(this)
        mMap!!.setOnCameraMoveStartedListener(this)
        mMap!!.setOnCameraIdleListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById<MapView>(R.id.map1)
        tvCurrentAddress = findViewById<TextView>(R.id.tvAdd)

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
                        val currentLocation = loc.result
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


    //-------------------------ADDRESS FUNCTIONALITY BEGIN--------------------------//
    override fun onLocationChanged(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        var address: List<Address>? = null
        try {
            address = geocoder.getFromLocation(location.latitude, location.longitude,1)
        }catch (e: IOException){
            e.printStackTrace()
        }
        setAddress(address!![0])
    }

    private fun setAddress(address: Address) {
        if(address != null){
            if(address.getAddressLine(0) != null){
                tvCurrentAddress!!.text = address.getAddressLine(0)
            }
            if(address.getAddressLine(1) != null){
                tvCurrentAddress!!.text = tvCurrentAddress.text.toString() + address.getAddressLine(1).toString()
            }
        }
    }

    override fun onCameraMove() {

    }

    override fun onCameraMoveStarted(p0: Int) {

    }

    override fun onCameraIdle() {
        val geocoder = Geocoder(this, Locale.getDefault())
        var address: List<Address>? = null
        try {
            address = geocoder.getFromLocation(mMap!!.cameraPosition.target.latitude, mMap!!.cameraPosition.target.longitude, 1)
            setAddress(address!![0])
        }catch (e: java.lang.IndexOutOfBoundsException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    //-------------------------ADDRESS FUNCTIONALITY END--------------------------//

}