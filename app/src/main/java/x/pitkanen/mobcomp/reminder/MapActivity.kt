package x.pitkanen.mobcomp.reminder

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var googleMap: GoogleMap
    lateinit var fusedLocationClient:FusedLocationProviderClient
    lateinit var selectedLocation: LatLng
    lateinit var geofencingClient: GeofencingClient


    val GEOFENCE_ID = "REMINDER_GEO_ID"
    val GEOFENCE_RADIUS = 500 // 500 meters
    val GEOFENCE_EXPIRATION = 90*24*60*60*1000 // 90 days in milliseconds
    val GEOFENCE_DELAY = 60*1000 // 1 minute in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        (map_fragment as SupportMapFragment).getMapAsync(this)

        geofencingClient = LocationServices.getGeofencingClient(this)

        map_create.setOnClickListener {

            if(et_message.text.toString() == "") {
                toast("Fill in Reminder text")
                return@setOnClickListener
            }

            if(selectedLocation == null) {
                toast("Select location on the map")
                return@setOnClickListener
            }

            val reminder = Reminder(
                uid = null,
                time = null,
                location = String.format("%.3f,%.3f",selectedLocation.latitude,selectedLocation.longitude),
                message = et_message.text.toString()
            )

            doAsync {
                val db = Room.databaseBuilder(
                    applicationContext,
                    ReminderAppDb::class.java,
                    "reminders"
                ).build()

                val uid = db.reminderDao().insert(reminder).toInt()
                reminder.uid = uid

                db.close()
                createGeofence(selectedLocation, reminder, geofencingClient)

                finish()
            }
        }
    }

    private fun createGeofence(selectedLocation: LatLng, reminder: Reminder, geofencingClient: GeofencingClient) {
        val geofence = Geofence.Builder().setRequestId(GEOFENCE_ID)
                        .setCircularRegion(selectedLocation.latitude, selectedLocation.longitude, GEOFENCE_RADIUS.toFloat())
                        .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setLoiteringDelay(GEOFENCE_DELAY).build()

        val geofenceRequest = GeofencingRequest.Builder().setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                                .addGeofence(geofence).build()

        val intent = Intent(this, GeofenceReceiver::class.java)
                        .putExtra("uid", reminder.uid)
                        .putExtra("message", reminder.message)
                        .putExtra("location", reminder.location)

        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT)

        geofencingClient.addGeofences(geofenceRequest, pendingIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {

        if(requestCode != 42)
            return

        if(grantResults.isNotEmpty() && (grantResults[0]==PackageManager.PERMISSION_DENIED ||
                                      grantResults[1]==PackageManager.PERMISSION_DENIED) ) {
            toast("The Assisted Reminder needs all the permissions to function")
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (grantResults.isNotEmpty() && (grantResults[2] == PackageManager.PERMISSION_DENIED)) {
                toast("The Assisted Reminder needs all the permissions to function")
            }
        }

        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMapReady(pMap: GoogleMap?) {
        googleMap=pMap?:return

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val permissions = mutableListOf<String>()
                permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    permissions.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(),
                                                            42)
        }

        googleMap.isMyLocationEnabled=true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener {//location: Location? ->
            if (it != null) {
                val latLong = LatLng(it.latitude, it.longitude)
                with(googleMap) {
                    animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 13f))
                }
            }
        }

        googleMap.setOnMapClickListener {
            with(googleMap){
                clear()
                animateCamera(CameraUpdateFactory.newLatLngZoom(it, 13f))

                var title = ""
                var city = ""
                try {
                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    val addressList = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    val address = addressList.first()
                    city = address.locality
                    title = address.getAddressLine(0)
                }
                catch (e:Exception) {
                    toast(e.localizedMessage)
                }

                val marker = addMarker(MarkerOptions().position(it).snippet(city).title(title))
                marker.showInfoWindow()

                addCircle(CircleOptions().center(it).strokeColor(Color.RED)
                                                    .fillColor(Color.LTGRAY))

                selectedLocation = it
            }
        }
    }
}
