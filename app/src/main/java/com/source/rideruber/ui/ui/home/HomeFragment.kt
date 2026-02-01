package com.source.rideruber.ui.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.source.rideruber.databinding.FragmentHomeBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val LOCATION_PERMISSION_REQUEST = 101

    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var riderLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private var riderId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // OSMDroid config (VERY IMPORTANT)
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        checkUserAndInit()

        return binding.root
    }

    private fun checkUserAndInit() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Snackbar.make(binding.root, "User not logged in", Snackbar.LENGTH_LONG).show()
            return
        }

        riderId = user.uid

        setupFirebase()
        setupMap()
        setupLocation()
    }

    private fun setupFirebase() {
        onlineRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        currentUserRef = FirebaseDatabase.getInstance()
            .getReference("driversOnline")
            .child(riderId!!)

        riderLocationRef = FirebaseDatabase.getInstance()
            .getReference("driversLocation")

        geoFire = GeoFire(riderLocationRef)

        currentUserRef.setValue(true)

        currentUserRef.onDisconnect().removeValue()

        onlineRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUserRef.onDisconnect().removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, error.message, Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun setupMap() {
        val map = binding.map

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val asansol = GeoPoint(23.6739, 86.9524)
        map.controller.setZoom(15.0)
        map.controller.setCenter(asansol)

        val marker = Marker(map)
        marker.position = asansol
        marker.title = "Asansol, West Bengal"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
    }

    private fun setupLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun enableMyLocation() {
        val map = binding.map

        val locationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            map
        )
        locationOverlay.enableMyLocation()

        locationOverlay.runOnFirstFix {
            val loc = locationOverlay.myLocation
            if (loc != null && riderId != null) {
                geoFire.setLocation(
                    riderId!!,
                    GeoLocation(loc.latitude, loc.longitude)
                )
            }
        }

        map.overlays.add(locationOverlay)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        riderId?.let {
            geoFire.removeLocation(it)
            currentUserRef.removeValue()
        }

        _binding = null
    }
}
