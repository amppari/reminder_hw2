package x.pitkanen.mobcomp.reminder

import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var googleMap: GoogleMap
    lateinit var fusedLocationClient:FusedLocationProviderClient
    var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        (map_fragment as SupportMapFragment).getMapAsync(this)

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
                location = String.format("%.3f,%.3f",selectedLocation!!.latitude,selectedLocation!!.longitude),
                message = et_message.text.toString()
            )

            doAsync {
                val db = Room.databaseBuilder(
                    applicationContext,
                    ReminderAppDb::class.java,
                    "reminders"
                ).build()
                db.reminderDao().insert(reminder)
                db.close()

                finish()
            }
        }
    }

    override fun onMapReady(pMap: GoogleMap?) {
        googleMap=pMap?:return

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION),
                                                            1)
        }

        googleMap.isMyLocationEnabled=true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener {//location: Location? ->
            if (it != null) {
                var latLong = LatLng(it.latitude, it.longitude)
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

                selectedLocation = it
            }
        }
    }
}
