package com.fitness.myhealthfitness

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fitness.myhealthfitness.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var loginBinding: ActivityLoginBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance().reference

        if (isUserLoggedIn()) {
            navigateToHomePage()
            return
        }

        val currentUser = auth.currentUser

        if (currentUser != null && sharedPreferences.getBoolean("rememberMe", false)) {
            // User is logged in and "Remember Me" is enabled
            startActivity(Intent(this, Homepage::class.java))
            finish()
        }

        loginBinding.loginButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }

        // Check if there's a logout request
        if (intent.hasExtra("logout")) {
            clearSharedPreferences()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)
        return email != null && password != null
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, Homepage::class.java)
        startActivity(intent)
        finish()
    }

    private fun clearSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }


}
