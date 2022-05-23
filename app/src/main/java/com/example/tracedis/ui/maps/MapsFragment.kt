package com.example.tracedis.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tracedis.R
import com.example.tracedis.databinding.FragmentMapsBinding
import com.example.tracedis.model.Result
import com.example.tracedis.service.TrackerService
import com.example.tracedis.ui.maps.MapUtil.calculateElapsedTime
import com.example.tracedis.ui.maps.MapUtil.calculateTheDistance
import com.example.tracedis.ui.maps.MapUtil.setCameraPosition
import com.example.tracedis.util.Constants.ACTION_SERVICE_START
import com.example.tracedis.util.Constants.ACTION_SERVICE_STOP
import com.example.tracedis.util.ExtensionFunctions.disable
import com.example.tracedis.util.ExtensionFunctions.enabled
import com.example.tracedis.util.ExtensionFunctions.hide
import com.example.tracedis.util.ExtensionFunctions.show
import com.example.tracedis.util.Permissions.hasBackgroundLocationPermission
import com.example.tracedis.util.Permissions.requestBackgroundLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


//@AndroidEntryPoint
class MapsFragment : Fragment() , OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
     GoogleMap.OnMarkerClickListener, EasyPermissions.PermissionCallbacks{

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L


    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetButton.setOnClickListener {
            onResetButtonClicked()
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map  = googleMap!!
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled=false

        }
        observeTrackerService()
    }

    private fun observeTrackerService(){
        TrackerService.locationList.observe(viewLifecycleOwner, {
            if (it != null){
                locationList = it
                if(locationList.size>1){
                    // add current location address to view on map
                    //add last loc to firestore w uid email time and address
                    binding.stopButton.enabled()
                }
                drawPolyline()
                followPolyLine()
                //Log.d("LocationList", locationList.toString())
            }
        })
        TrackerService.started.observe(viewLifecycleOwner, {
            started.value = it
        })
        TrackerService.startTime.observe(viewLifecycleOwner,{
            startTime=it
        })
        TrackerService.stopTime.observe(viewLifecycleOwner,{
            stopTime=it
            if (stopTime !=0L){
                showBiggerPicture()
                displayResults()
            }
        })
    }

    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList){
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),100
            ), 2000,null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng){
        val marker = map.addMarker(MarkerOptions().position(position))
        if (marker != null) {
            markerList.add(marker)
        }
    }



    private fun displayResults(){
        val result = Result(
            calculateTheDistance(locationList),
                    calculateElapsedTime(startTime,stopTime)

        )
        lifecycleScope.launch {
            delay(2500)
            var directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.startButton.apply {
                hide()
                enabled()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }
    }

    private fun followPolyLine(){
        if(locationList.isNotEmpty()){
            if(locationList.size>1) {
                binding.addrTextView.text = getAddress(locationList.last())
            }
            map.animateCamera((
                    CameraUpdateFactory.newCameraPosition(
                        setCameraPosition(
                            locationList.last()
                        )
                    )
                    ), 1000,null)
        }
    }

    private fun drawPolyline(){
        //binding.addrTextView.text = getAddress(locationList.last())
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }


    private fun onStartButtonClicked() {
        if(hasBackgroundLocationPermission(requireContext())){
          //  Log.d("MapsActivity", "Already Enabled.")
            startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.stopButton.show()
        }else{
            requestBackgroundLocationPermission(this)
        }
    }

    private fun startCountDown() {
       binding.timerTextView.show()
        binding.addrTextView.show()
        binding.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000,1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished/1000
                if (currentSecond.toString()=="0"){
                    binding.timerTextView.text= "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                }else{
                    binding.timerTextView.text= currentSecond.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red
                    ))

                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }

        }

        timer.start()
    }

    private fun sendActionCommandToService(action: String){
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun onStopButtonClicked() {
        stopForegroundService()
        binding.startButton.show()
        binding.stopButton.hide()

    }



    private fun onResetButtonClicked() {
        mapReset()
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                val lastKnownlocation = LatLng(
                    it.result.latitude,
                    it.result.longitude
                )

                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        setCameraPosition(lastKnownlocation)
                    )
                )
                for(polyLine in polylineList){
                    polyLine.remove()
                }
                for (marker in markerList){
                    marker.remove()
                }
                locationList.clear()
                markerList.clear()
                binding.resetButton.hide()
                binding.startButton.show()
            }
    }

    private fun stopForegroundService() {
        binding.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        } else{
            requestBackgroundLocationPermission(this)
        }

   }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.startButton.show()
        }
        return false

    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }

    fun getAddress(loc:LatLng): String{
        var addr=""
        var yourAddresses: List<Address>
        val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
        yourAddresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)

        if (yourAddresses.isNotEmpty()) {
            val yourAddress: String = yourAddresses[0].getAddressLine(0)
//            val yourCity: String = yourAddresses[0].getAddressLine(1)
//            val yourCountry: String = yourAddresses[0].getAddressLine(2)
//            addr= "$yourAddress, $yourCity, $yourCountry. "
            addr="$yourAddress"
        }
        return addr
    }
}