package com.example.todo.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.compose.ui.viewinterop.AndroidView
import com.example.todo.ui.theme.ToDoTheme

/**
 * Prosty Activity pokazujący MapView. Użytkownik long-pressem umieszcza marker i może potwierdzić wybór.
 * Zwraca wynik w extras: "lat" (Double), "lng" (Double)
 */
class MapPickActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private var pickedLatLng: LatLng? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                tryEnableMyLocation()
            } else {
                Toast.makeText(this, "Brak uprawnień lokalizacji — nie można wyświetlić pozycji", Toast.LENGTH_SHORT).show()
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)

        setContent {
            ToDoTheme {
                Scaffold(topBar = {
                    CenterAlignedTopAppBar(title = { Text("Wybierz lokalizację") })
                }) { padding ->
                    Column(modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()) {

                        Box(modifier = Modifier.weight(1f)) {
                            AndroidView(factory = { mapView }) { mv ->
                                mv.getMapAsync { gm ->
                                    map = gm
                                    gm.uiSettings.isZoomControlsEnabled = true
                                    // enable my-location if permission granted
                                    tryEnableMyLocation()
                                    // ustaw listener long-click
                                    gm.setOnMapLongClickListener { latLng ->
                                        gm.clear()
                                        gm.addMarker(MarkerOptions().position(latLng))
                                        gm.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                                        pickedLatLng = latLng
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                            horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { setResult(RESULT_CANCELED); finish() }) {
                                Text("Anuluj")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val lat = pickedLatLng?.latitude
                                val lng = pickedLatLng?.longitude
                                if (lat == null || lng == null) {
                                    Toast.makeText(this@MapPickActivity, "Długo przytrzymaj mapę aby wybrać punkt", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val data = Intent().apply {
                                    putExtra("lat", lat)
                                    putExtra("lng", lng)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            }) {
                                Text("Wybierz")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun tryEnableMyLocation() {
        val gm = map ?: return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                gm.isMyLocationEnabled = true
                // opcjonalnie: ustaw kamerę na aktualną pozycję, jeśli dostępna (pomijamy tu fetch location dla zwięzłości)
            } catch (_: Exception) { }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}