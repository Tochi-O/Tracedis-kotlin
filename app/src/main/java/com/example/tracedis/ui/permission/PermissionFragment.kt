package com.example.tracedis.ui.permission

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.tracedis.R
import com.example.tracedis.util.Permissions.hasLocationPermission
import com.example.tracedis.util.Permissions.requestLocationPermission
import com.example.tracedis.databinding.FragmentPermissionBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog

// TODO: Rename parameter arguments, choose names that match


class PermissionFragment : Fragment(), EasyPermissions.PermissionCallbacks {


    private var _binding: FragmentPermissionBinding? =null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =   FragmentPermissionBinding.inflate(inflater,container,false)

        binding.continueButton.setOnClickListener {
            if(hasLocationPermission(requireContext())){
                findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
            }else{
                requestLocationPermission(this)
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{

        }

    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
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
}