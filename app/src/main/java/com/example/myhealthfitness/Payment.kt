package com.fitness.myhealthfitness

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fitness.myhealthfitness.databinding.ActivityPaymentBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Payment : AppCompatActivity() {

    private lateinit var paymentBinding: ActivityPaymentBinding
    private var currentCreditsValue: Int = 0
    private var requiredCreditsValue: Int = 1
    private lateinit var currentCredits: TextView
    private lateinit var requiredCredits: TextView
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        paymentBinding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(paymentBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        paymentBinding.cancelBtn.setOnClickListener {
            onBackPressed()
        }

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference

        // Get user ID from SharedPreferences
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "-")

        // Fetch current credits from Firebase
        fetchCurrentCredits()

        // Set required credits
        currentCredits = findViewById(R.id.shakeCredits)
            ?: throw IllegalStateException("currentCredits TextView not found")
        requiredCredits = findViewById(R.id.requiredCredits)
            ?: throw IllegalStateException("requiredCredits TextView not found")
        requiredCredits.text = "Required Credits: $requiredCreditsValue"
    }

    private fun fetchCurrentCredits() {
        userId?.let { id ->
            database.child("MyFitness").child("userDetails").child(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                currentCreditsValue = it.available_credits?.toIntOrNull() ?: 0
                                currentCredits.text = "Your Current Credits: $currentCreditsValue"

                                val remainingCredits = currentCreditsValue - requiredCreditsValue
                                paymentBinding.balanceCredits.text = remainingCredits.toString()

                                paymentBinding.proceedPayment.setOnClickListener {
                                    paymentBinding.cancelBtn.visibility = View.GONE
                                    paymentBinding.progressBarHorizontal.visibility = View.VISIBLE

                                    if (remainingCredits == 0) {
                                        showToast("Your credit score is low. Please recharge credits to proceed.")
                                        paymentBinding.cancelBtn.visibility = View.VISIBLE
                                        paymentBinding.progressBarHorizontal.visibility = View.GONE
                                    } else {
                                        // Deduct 1 credit from available credits
                                        updateAvailableCredits(currentCreditsValue - 1)

                                        // Store ShakeRequest details
                                        storeShakeRequest()
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch current credits: ${error.message}")
                    }
                })
        }
    }

    private fun updateAvailableCredits(newCredits: Int) {
        userId?.let { id ->
            val userRef = database.child("MyFitness").child("userDetails").child(id)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentCreditsSpend = snapshot.child("credits_spend").value?.toString() ?: "0"
                        val creditsSpendInt = if (currentCreditsSpend.isNotEmpty()) {
                            currentCreditsSpend.toInt()
                        } else {
                            0
                        }
                        val newCreditsSpend = creditsSpendInt + 1

                        // Update available_credits and credits_spend
                        userRef.child("available_credits").setValue(newCredits.toString())
                        userRef.child("credits_spend").setValue(newCreditsSpend.toString())
                            .addOnSuccessListener {
                                // Update UI or perform any post-success operations
                            }
                            .addOnFailureListener { error ->
                                showToast("Failed to update credits_spend: ${error.message}")
                            }
                    } else {
                        showToast("User data not found.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Failed to fetch user data: ${error.message}")
                }
            })
        }
    }


    private fun storeShakeRequest() {
        // Fetch the current counter value and increment it atomically
        val shakeRequestsRef = database.child("MyFitness").child("ShakeRequests")
        shakeRequestsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var currentId = mutableData.child("counter").getValue(Int::class.java) ?: 0
                currentId++
                mutableData.child("counter").value = currentId
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (committed) {
                    val shakeRequestId = "Shake${dataSnapshot?.child("counter")?.value}"
                    userId?.let { id ->
                        val shakeRequestRef = database.child("MyFitness").child("ShakeRequests")
                            .child(shakeRequestId)

                        // Get current date and time
                        val dateFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val currentDate = dateFormat.format(Date())

                        // Store shake request details under the generated ID with timestamp
                        database.child("MyFitness").child("userDetails").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userDataSnapshot: DataSnapshot) {
                                    if (userDataSnapshot.exists()) {
                                        val user =
                                            userDataSnapshot.getValue(User::class.java)
                                        user?.let {
                                            val shakeRequest = ShakeRequestClass(
                                                it.userId,
                                                it.username,
                                                it.email,
                                                it.mobile,
                                                it.available_credits,
                                                requiredCreditsValue.toString(),
                                                currentDate
                                            )
                                            shakeRequestRef.setValue(shakeRequest)
                                                .addOnSuccessListener {
                                                    showSuccessDialog()
                                                }
                                                .addOnFailureListener { error ->
                                                    showToast("Failed to store ShakeRequest: ${error.message}")
                                                }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    showToast("Failed to fetch user details: ${error.message}")
                                }
                            })
                    }
                } else {
                    showToast("Failed to generate shake request ID.")
                }
            }
        })
    }

    private fun showSuccessDialog() {
        paymentBinding.progressBarHorizontal.visibility = View.GONE
        val dialogBuilder = AlertDialog.Builder(this, R.style.TransparentDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.success_credit_shake, null)

        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()

        alertDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        alertDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            alertDialog.dismiss()
            onBackPressed()
        }, 3000)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
