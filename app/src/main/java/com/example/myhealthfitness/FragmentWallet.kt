package com.fitness.myhealthfitness

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.database.*

class FragmentWallet : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wallet, container, false)

        // Initialize SharedPreferences
        sharedPreferences =
            requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "") ?: ""

        // Initialize Database Reference
        databaseReference =
            FirebaseDatabase.getInstance().reference.child("MyFitness").child("userWishList")

        val containerLayout = root.findViewById<LinearLayout>(R.id.fragment_wishlist)
        containerLayout?.let {
            checkUserWishList()
        }

        return root
    }

    private fun checkUserWishList() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userFound = false

                for (userSnapshot in snapshot.children) {
                    for (wishListSnapshot in userSnapshot.children) {
                        val userId =
                            wishListSnapshot.key

                        val dbUserId = wishListSnapshot.child("userId").getValue(String::class.java)

                        if (dbUserId == userId) {
                            userFound = true

                            val packageId =
                                wishListSnapshot.child("packageId").getValue(String::class.java)
                            val productCredits =
                                wishListSnapshot.child("productCredits").getValue(Int::class.java)
                            println("User ID: $dbUserId, Package ID: $packageId, Credits: $productCredits")
                        }
                    }
                }

                if (userFound) {
                    Toast.makeText(requireContext(), "User ID matched", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No matching user found in database",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to retrieve data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

}
