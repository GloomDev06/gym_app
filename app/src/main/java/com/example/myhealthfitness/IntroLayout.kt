package com.fitness.myhealthfitness

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.Manifest
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fitness.myhealthfitness.databinding.ActivityIntroLayoutBinding
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class IntroLayout : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView

    private lateinit var introLayoutBinding: ActivityIntroLayoutBinding

    private var barcodeCallback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            result?.let {
                handleQRCodeResult(it.text)
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        introLayoutBinding = ActivityIntroLayoutBinding.inflate(layoutInflater)
        setContentView(introLayoutBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize barcodeView
        barcodeView = introLayoutBinding.qrScanner
        barcodeView.decodeContinuous(barcodeCallback)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    fun startScan(view: android.view.View) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.decodeContinuous(barcodeCallback)
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleQRCodeResult(qrCodeData: String) {
        // Call your backend API to validate and process the QR code data here
        // Example: Check if the user has sufficient credits to process the shake request
        // Implement your API call and logic here
        // For demonstration purpose, let's just show the QR code data
        Toast.makeText(this, "QR Code scanned: $qrCodeData", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}
