package com.fitness.myhealthfitness

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FragmentShakeRequest : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userId: String // Assuming userId is stored in SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_shake_request2, container, false)

        // Initialize SharedPreferences
        sharedPreferences =
            requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "") ?: ""

        databaseReference =
            FirebaseDatabase.getInstance().reference.child("MyFitness").child("ShakeRequests")

        val containerLayout = root.findViewById<LinearLayout>(R.id.shakeRequestsContainer)
        containerLayout?.let {
            retrieveShakeRequests(it)
        }

        return root
    }

    private fun retrieveShakeRequests(container: LinearLayout) {
        container.removeAllViews()

        databaseReference.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var hasValidShakeRequests = false

                        for (shakeSnapshot in snapshot.children) {
                            if (shakeSnapshot.hasChild("deletedAt")) {
                                continue // Skip deleted shake requests
                            } else {
                                hasValidShakeRequests = true

                                val shakeNumber =
                                    shakeSnapshot.child("shakeCredits").getValue(String::class.java)
                                        ?: ""
                                val timestamp =
                                    shakeSnapshot.child("timestamp").getValue(String::class.java)
                                        ?: ""

                                val dateTimeParts = timestamp.split(" ")
                                val date = formatDate(dateTimeParts[0])
                                val time = if (dateTimeParts.size > 1) dateTimeParts[1] else ""

                                val shakeRequestLayout = layoutInflater.inflate(
                                    R.layout.item_shake_request,
                                    container,
                                    false
                                )

                                val countsTextView =
                                    shakeRequestLayout.findViewById<TextView>(R.id.counts)
                                val shakeReqNumberTextView =
                                    shakeRequestLayout.findViewById<TextView>(R.id.shakeReqNumber)
                                val requestTimeTextView =
                                    shakeRequestLayout.findViewById<TextView>(R.id.requestTime)
                                val cancelImageView =
                                    shakeRequestLayout.findViewById<ImageView>(R.id.cancel)

                                countsTextView.text =
                                    (container.childCount + 1).toString() // Index starts from 1
                                shakeReqNumberTextView.text = "Shake Credits: $shakeNumber"
                                requestTimeTextView.text = "Date: $date\nTime: $time"

                                cancelImageView.setOnClickListener {
                                    showDeleteConfirmationDialog(shakeSnapshot.key ?: "")
                                }

                                container.addView(shakeRequestLayout)
                            }
                        }

                        if (!hasValidShakeRequests) {
                            val noShakesLayout = layoutInflater.inflate(
                                R.layout.no_shakes_left,
                                container,
                                false
                            )
                            container.addView(noShakesLayout)
                        }
                    } else {
                        val noShakesLayout = layoutInflater.inflate(
                            R.layout.no_shakes_left,
                            container,
                            false
                        )
                        container.addView(noShakesLayout)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(
                        requireContext(),
                        "Failed to retrieve shake requests: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun formatDate(date: String): String {
        // Input format: yyyy-MM-dd
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // Output format: dd-MM-yyyy
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        return outputFormat.format(parsedDate!!)
    }

    private fun showDeleteConfirmationDialog(shakeRequestId: String) {
        val dialogView = layoutInflater.inflate(R.layout.cancel_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        val cancelBtn = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val confirmBtn = dialogView.findViewById<TextView>(R.id.btn_confirm)

        cancelBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        confirmBtn.setOnClickListener {
            deleteShakeRequest(shakeRequestId)
            alertDialog.dismiss()
        }
    }

    private fun deleteShakeRequest(shakeRequestId: String) {
        val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val deletionTime = dateFormat.format(Date())

        databaseReference.child(shakeRequestId).child("deletedAt").setValue(deletionTime)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Shake request deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                retrieveShakeRequests(requireView().findViewById(R.id.shakeRequestsContainer))
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to delete shake request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
