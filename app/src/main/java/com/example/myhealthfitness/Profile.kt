package com.fitness.myhealthfitness

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.util.Calendar

class Profile : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        sharedPreferences =
            requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "-")
        database = FirebaseDatabase.getInstance().reference

        fetchUserData(root)

        root.findViewById<ImageView>(R.id.editUserName).setOnClickListener {
            showEditDialog("username")
        }

        root.findViewById<ImageView>(R.id.editPhone).setOnClickListener {
            showEditDialog("phone")
        }

        root.findViewById<ImageView>(R.id.editMail).setOnClickListener {
            showEditDialog("email")
        }

        root.findViewById<ImageView>(R.id.editDOB).setOnClickListener {
            showDatePickerDialog()
        }

        return root
    }

    private fun fetchUserData(root: View) {
        userId?.let { id ->
            database.child("MyFitness").child("userDetails").child(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                showToast("${it.userId}")
                                root.findViewById<TextView>(R.id.userId).text = it.userId
                                root.findViewById<TextView>(R.id.username).text = it.username
                                root.findViewById<TextView>(R.id.emailText).text = it.email
                                root.findViewById<TextView>(R.id.phoneNumber).text = it.mobile
                                root.findViewById<TextView>(R.id.dateOfBirthValue).text =
                                    it.date_of_birth

                                val availableCredits =
                                    it.available_credits?.takeIf { it.isNotEmpty() } ?: "0"
                                root.findViewById<TextView>(R.id.creditValue).text =
                                    "$availableCredits Credits available"

                                if (availableCredits == "0" || availableCredits == "0 Scores") {
                                    root.findViewById<TextView>(R.id.goldMember).visibility =
                                        View.GONE
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch user data: ${error.message}")
                    }
                })
        }
    }


    private fun showEditDialog(field: String) {
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog, null)
        val editText = dialogView.findViewById<EditText>(R.id.editText)

        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val alertDialog = dialogBuilder.setView(dialogView).create()

        submitButton.setOnClickListener {
            val newText = editText.text.toString().trim()
            updateField(field, newText)
            alertDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate =
                    String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear)
                updateField("dob", formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun updateField(field: String, newText: String) {
        when (field) {
            "username" -> {
                updateFirebaseField("username", newText)
            }
            "phone" -> {
                val phoneTextView: TextView? = view?.findViewById(R.id.phoneNumber)
                // Check if the new phone number is already registered with another email
                checkPhoneNumberExists(newText) { exists ->
                    if (!exists) {
                        updateFirebaseField("mobile", newText)
                    } else {
                        showToast("Phone number already registered with another email!")
                        // Revert UI change if validation fails
//                        phoneTextView?.text = sharedPreferences.getString("mobile", "")
                    }
                }
            }

            "email" -> {
                val emailTextView: TextView? = view?.findViewById(R.id.emailText)
                // Check if the new email is already registered with another phone number
                checkEmailExists(newText) { exists ->
                    if (!exists) {
                        updateFirebaseField("email", newText)
                    } else {
                        showToast("Email already registered with another phone number!")
//                        emailTextView?.text = sharedPreferences.getString("email", "")
                    }
                }
            }

            "dob" -> {
                updateFirebaseField("date_of_birth", newText)
            }
        }
    }


    private fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        database.child("MyFitness").child("userDetails")
            .orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error checking email: ${error.message}")
                    callback(false)
                }
            })
    }

    private fun checkPhoneNumberExists(phoneNumber: String, callback: (Boolean) -> Unit) {
        database.child("MyFitness").child("userDetails")
            .orderByChild("mobile").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error checking phone number: ${error.message}")
                    callback(false)
                }
            })
    }


    private fun updateFirebaseField(field: String, newText: String) {
        userId?.let {
            database.child("MyFitness").child("userDetails").child(it).child(field)
                .setValue(newText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        when (field) {
                            "username" -> view?.findViewById<TextView>(R.id.username)?.text = newText
                            "phone" -> view?.findViewById<TextView>(R.id.phoneNumber)?.text =
                                newText

                            "email" -> view?.findViewById<TextView>(R.id.emailText)?.text = newText
                            "dob" -> view?.findViewById<TextView>(R.id.dateOfBirthValue)?.text =
                                newText
                        }

                        fetchUserData(requireView())

                        sharedPreferences.edit().apply {
                            when (field) {
                                "username" -> putString("username", newText)
                                "phone" -> putString("mobile", newText)
                                "email" -> putString("email", newText)
                                "dob" -> putString("dob", newText)
                            }
                            apply()
                        }
                        showToast("Updated $field in Database successfully!")
                    } else {
                        showToast("Failed to update $field in Database")
                    }
                }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
