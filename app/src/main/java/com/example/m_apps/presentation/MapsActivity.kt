package com.example.m_apps.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.m_apps.core.utils.DirectionsParser
import com.example.m_apps.core.utils.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.m_apps.core.utils.PermissionUtils.isPermissionGranted
import com.example.m_apps.R
import com.example.m_apps.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MapsActivity : AppCompatActivity(),
    OnMyLocationButtonClickListener,
    OnMyLocationClickListener, OnMapReadyCallback,
    OnRequestPermissionsResultCallback {

    private var permissionDenied = false
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: MapsViewModel
    private lateinit var currentLocation: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MapsViewModel::class.java]

        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableMyLocation()
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)

        getAllMarkers()
        setOnMarkerClick()
        setOnMapLongClick()
        map.isBuildingsEnabled = true
        map.isIndoorEnabled = true

    }

    private fun setOnMarkerClick() {
        var currentPolyline: Polyline? = null
        map.setOnMarkerClickListener { marker ->
            val name = marker.title ?: "Unnamed Location"
            val latLng = marker.position

            CustomDialogFragment.customDialog(this, name, "${latLng.latitude}, ${latLng.longitude}",
                onDrawLine = {
                    currentPolyline?.remove()
                    currentPolyline = map.addPolyline(
                        PolylineOptions()
                            .add(currentLocation, latLng)
                            .width(5f)
                            .color(Color.RED))
//                    drawRouteToDestination(latLng)
                },
                delete = {
                    marker.remove()
                    currentPolyline?.remove()
                    viewModel.deleteMarker(latLng, name)
                })
            true
        }
    }

    private fun getAllMarkers() {
        viewModel.getAllMarkers()
        viewModel.mapsDbLiveData.observe(this) {
            it.forEach { maps ->
                map.addMarker(MarkerOptions().position(maps.latLng).title(maps.name))
            }
        }
    }

    private fun setOnMapLongClick() {
        map.setOnMapLongClickListener { la ->
            CustomDialogFragment.customDialogWindow(this) { name ->
                map.addMarker(MarkerOptions().position(la).title(name))
                viewModel.insertMarker(la, name)

                drawRouteToDestination(la)
            }
        }
    }

    // Используется для построения маршрута(не работает из-за того, что google не дает доступ к API)
    private fun drawRouteToDestination(destination: LatLng) {
        val url = getDirectionsUrl(currentLocation, destination)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val routeData = withContext(Dispatchers.IO) {
                    downloadUrl(url)
                }
                val routes = parseDirections(routeData)
                drawRoute(routes)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapsActivity, "Ошибка при построении маршрута", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Используется для построения маршрута(не работает из-за того, что google не дает доступ к API)
    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${dest.latitude},${dest.longitude}"
        val parameters = "$strOrigin&$strDest&sensor=false&mode=driving"
        return "https://maps.googleapis.com/maps/api/directions/json?$parameters&key=AIzaSyCntOqnyenzb-fuYllQ9UBReVPIACGeLnY"
    }


    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        currentLocation = currentLatLng

                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        Toast.makeText(
                            this,
                            "Не удалось определить местоположение",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT)
            .show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            enableMyLocation()
        } else {
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {

            showMissingPermissionError()
            permissionDenied = false
        }
    }

    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    // Используется для построения маршрута(не работает из-за того, что google не дает доступ к API)
    private suspend fun downloadUrl(strUrl: String): String = withContext(Dispatchers.IO) {

        val data = StringBuilder()
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()

            iStream = urlConnection.inputStream
            val br = BufferedReader(InputStreamReader(iStream))
            var line: String?

            while (br.readLine().also { line = it } != null) {
                data.append(line)
            }
            br.close()
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        } finally {
            iStream?.close()
            urlConnection?.disconnect()
        }
        data.toString()
    }

    // Используется для построения маршрута(не работает из-за того, что google не дает доступ к API)
    private fun parseDirections(jsonData: String): List<List<HashMap<String, String>>> {
        val jObject = JSONObject(jsonData)
        val status = jObject.getString("status")
        if (status != "OK") {
            Toast.makeText(this@MapsActivity, "Ошибка: $status", Toast.LENGTH_SHORT).show()
            return emptyList()
        }

        val parser = DirectionsParser()
        return parser.parse(jObject)
    }

// Используется для построения маршрута(не работает из-за того, что google не дает доступ к API)
    private fun drawRoute(routes: List<List<HashMap<String, String>>>) {
        if (routes.isEmpty()) {
            Toast.makeText(this, "Маршрут не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val lineOptions = PolylineOptions()
        for (path in routes) {
            val points = ArrayList<LatLng>()
            for (point in path) {
                val lat = point["lat"]!!.toDouble()
                val lng = point["lng"]!!.toDouble()
                val position = LatLng(lat, lng)
                points.add(position)
            }
            lineOptions.addAll(points)
            lineOptions.width(12f)
            lineOptions.color(Color.RED)
            lineOptions.geodesic(true)
        }
        map.addPolyline(lineOptions)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}