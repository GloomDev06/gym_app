package com.fitness.myhealthfitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myhealthfitness.OTPActivity
import com.fitness.myhealthfitness.databinding.ActivityMobileNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MobileNumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMobileNumberBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMobileNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.sendOtpButton.setOnClickListener {
           val mobileNumber = binding.mobileEditText.text.toString().trim()
            if (mobileNumber.isNotEmpty()) {
                sendOTPToMobile(mobileNumber)
            } else {
                Toast.makeText(this, "Please enter a mobile number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOTPToMobile(mobile: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(mobile)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Toast.makeText(this@MobileNumberActivity, "Verification completed", Toast.LENGTH_SHORT).show()
                    // Automatically verifies and signs in the user.
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@MobileNumberActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verificationId, token)
                    Toast.makeText(this@MobileNumberActivity, "OTP sent to mobile number", Toast.LENGTH_SHORT).show()
                    this@MobileNumberActivity.verificationId = verificationId
                    this@MobileNumberActivity.resendToken = token
                    val intent = Intent(this@MobileNumberActivity, OTPActivity::class.java)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("resendToken", token)
                    startActivity(intent)
                    finish()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
                    navigateToHomePage()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToHomePage() {
        startActivity(Intent(this, Homepage::class.java))
        finish()
    }
}


