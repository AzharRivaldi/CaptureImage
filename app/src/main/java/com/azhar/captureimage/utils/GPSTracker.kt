package com.azhar.captureimage.utils

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.*
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.azhar.captureimage.utils.GPSTracker
import java.io.IOException
import java.util.*

class GPSTracker(
        private val context: Context) : Service(), LocationListener {

    var isGPSEnabled = false
    var isNetworkEnabled = false
    var isGPSTrackingEnabled = false
    var location: Location? = null
    var latitude = 0.0
    var longitude = 0.0
    var geocoderMaxResults = 1

    protected var locationManager: LocationManager? = null
    private var provider_info: String? = null

    @SuppressLint("MissingPermission")
    fun getLocation() {
        try {
            locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGPSEnabled) {
                isGPSTrackingEnabled = true
                Log.d(TAG, "Application use GPS Service")
                provider_info = LocationManager.GPS_PROVIDER
            } else if (isNetworkEnabled) { // Try to get location if you Network Service is enabled
                isGPSTrackingEnabled = true
                Log.d(TAG, "Application use Network State to get GPS coordinates")
                provider_info = LocationManager.NETWORK_PROVIDER
            }

            if (!provider_info!!.isEmpty()) {
                locationManager!!.requestLocationUpdates(provider_info,
                        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)
                if (locationManager != null) {
                    location = locationManager!!.getLastKnownLocation(provider_info)
                    updateGPSCoordinates()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible to connect to LocationManager", e)
        }
    }

    fun updateGPSCoordinates() {
        if (location != null) {
            latitude = location!!.latitude
            longitude = location!!.longitude
        }
    }

    @JvmName("getLatitude1")
    fun getLatitude(): Double {
        if (location != null) {
            latitude = location!!.latitude
        }
        return latitude
    }

    @JvmName("getLongitude1")
    fun getLongitude(): Double {
        if (location != null) {
            longitude = location!!.longitude
        }
        return longitude
    }

    fun stopUsingGPS() {
        if (locationManager != null) {
            locationManager!!.removeUpdates(this@GPSTracker)
        }
    }

    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("GPS is settings")
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

        alertDialog.setPositiveButton("Settings") { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }

        alertDialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }

    fun getGeocoderAddress(context: Context?): List<Address>? {
        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                return geocoder.getFromLocation(latitude, longitude, geocoderMaxResults)
            } catch (e: IOException) {
                //e.printStackTrace();
                Log.e(TAG, "Impossible to connect to Geocoder", e)
            }
        }
        return null
    }

    fun getAddressLine(context: Context?): String? {
        val addresses = getGeocoderAddress(context)
        return if (addresses != null && addresses.size > 0) {
            val address = addresses[0]
            address.getAddressLine(0)
        } else {
            null
        }
    }

    fun getLocality(context: Context?): String? {
        val addresses = getGeocoderAddress(context)
        return if (addresses != null && addresses.size > 0) {
            val address = addresses[0]
            address.locality
        } else {
            null
        }
    }

    fun getPostalCode(context: Context?): String? {
        val addresses = getGeocoderAddress(context)
        return if (addresses != null && addresses.size > 0) {
            val address = addresses[0]
            address.postalCode
        } else {
            null
        }
    }

    fun getCountryName(context: Context?): String? {
        val addresses = getGeocoderAddress(context)
        return if (addresses != null && addresses.size > 0) {
            val address = addresses[0]
            address.countryName
        } else {
            null
        }
    }

    override fun onLocationChanged(location: Location) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val TAG = GPSTracker::class.java.name
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10
        private const val MIN_TIME_BW_UPDATES = (1000 * 60 * 1).toLong()
    }

    init {
        getLocation()
    }

}