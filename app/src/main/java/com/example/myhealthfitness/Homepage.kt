package com.fitness.myhealthfitness

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.fitness.myhealthfitness.databinding.ActivityHomepageBinding
import com.google.firebase.database.*

class Homepage : AppCompatActivity(), FragmentSettings.OnUserProfileClickListener,
    FragmentSettings.OnNotificationSettingsClickListener, FragmentSettings.OnWalletClickListener,
    FragmentSettings.OnTransactionClickListener, FragmentSettings.OnShakeRequestClickListener,
    FragmentSettings.OnFaqClickListener, FragmentSettings.OnTermsClickListener {

    private lateinit var homepageBinding: ActivityHomepageBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        homepageBinding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(homepageBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference

        // Check if user profile is complete before setting the initial fragment
        checkUserProfileComplete()

        homepageBinding.home.setOnClickListener {
            replaceFragment(FragmentHome())
        }
        homepageBinding.qr.setOnClickListener {
            replaceFragment(FragmentQR())
        }
        homepageBinding.packages.setOnClickListener {
            replaceFragment(Packages())
        }
        homepageBinding.settings.setOnClickListener {
            replaceFragment(FragmentSettings())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentLayout, fragment)
            .commit()
    }

    private fun checkUserProfileComplete() {
        val userId = sharedPreferences.getString("userId", null)
        if (userId != null) {
            database.child("MyFitness").child("userDetails").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null && !user.mobile.isNullOrEmpty() && !user.date_of_birth.isNullOrEmpty()) {
                                replaceFragment(FragmentHome())
                            } else {
                                replaceFragment(Profile())
                            }
                        } else {
                            replaceFragment(Profile())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                        Toast.makeText(
                            this@Homepage,
                            "Error fetching user data",
                            Toast.LENGTH_SHORT
                        ).show()
                        replaceFragment(Profile())
                    }
                })
        } else {
            // User ID not found in SharedPreferences, redirect to Profile fragment
            replaceFragment(Profile())
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentLayout)
        when (currentFragment) {
            is Profile -> replaceFragment(FragmentSettings())
            is FragmentWallet -> replaceFragment(FragmentSettings())
            is FragmentTransaction -> replaceFragment(FragmentSettings())
            is FragmentShakeRequest -> replaceFragment(FragmentSettings())
            is NotificationSettings -> replaceFragment(FragmentSettings())
            is Faq -> replaceFragment(FragmentSettings())
            is TermsOfUse -> replaceFragment(FragmentSettings())
            !is FragmentHome -> replaceFragment(FragmentHome())
            else -> super.onBackPressed()
        }
    }

    override fun onUserProfileClick() {
        replaceFragment(Profile())
    }

    override fun onNotificationSettingsClick() {
        replaceFragment(NotificationSettings())
    }

    override fun onWalletClick() {
        replaceFragment(FragmentWallet())
    }

    override fun onTransactionClick() {
        replaceFragment(FragmentTransaction())
    }

    override fun onShakeClick() {
        replaceFragment(FragmentShakeRequest())
    }

    override fun onFaqClick() {
        replaceFragment(Faq())
    }

    override fun onTermsClick() {
        replaceFragment(TermsOfUse())
    }
}
