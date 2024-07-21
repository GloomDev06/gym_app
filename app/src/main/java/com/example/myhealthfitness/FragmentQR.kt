package com.fitness.myhealthfitness

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.fitness.myhealthfitness.databinding.FragmentQRBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class FragmentQR : Fragment() {

    private var _binding: FragmentQRBinding? = null
    private val binding get() = _binding!!

    private lateinit var barcodeView: DecoratedBarcodeView
    private var validQRCodeScanned = false
    private var qrCodeProcessed = false // Flag to track if QR code is processed

    private val database = FirebaseDatabase.getInstance().reference

    private val barcodeCallback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            result?.let {
                handleQRCodeResult(it.text)
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQRBinding.inflate(inflater, container, false)
        val root = binding.root

        barcodeView = binding.qrScanner
        barcodeView.decodeContinuous(barcodeCallback)

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.decodeContinuous(barcodeCallback)
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleQRCodeResult(qrCodeData: String) {
        if (!qrCodeProcessed) { // Check if QR code is already processed
            val validUrl =
                "https://www.freepik.com/free-vector/gradient-vpn-illustration_22111200.htm#fromView=search&page=1&position=22&uuid=98cc849e-a1c6-494a-8202-790b88cab788"
            validQRCodeScanned = qrCodeData == validUrl

            if (validQRCodeScanned) {
                qrCodeProcessed = true // Set flag to true
                checkCreditsAndNavigate()
            } else {
                Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_LONG).show()
                navigateToError()
            }
        }
    }

    private fun checkCreditsAndNavigate() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        userId?.let { id ->
            database.child("MyFitness").child("userDetails").child(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            val availableCredits =
                                user?.available_credits?.takeIf { it.isNotEmpty() } ?: "0"
                            if (availableCredits == "1") {
                                Toast.makeText(
                                    requireContext(),
                                    "Invalid Credit Score",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(requireContext(), "QR Verified", Toast.LENGTH_LONG)
                                    .show()
                                navigateToPayment()
                            }
                        } else {
                            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch user data: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        } ?: run {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToPayment() {
        val intent = Intent(requireContext(), Payment::class.java)
        startActivity(intent)
    }

    private fun navigateToError() {
        val intent = Intent(requireContext(), Error::class.java)
        startActivity(intent)
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
