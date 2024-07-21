package com.fitness.myhealthfitness

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fitness.myhealthfitness.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

import java.security.MessageDigest

class Register : AppCompatActivity() {

    private lateinit var registerBinding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerBinding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(registerBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize auth before using it
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance().reference

        val currentUser = auth.currentUser

        if (currentUser != null && sharedPreferences.getBoolean("rememberMe", true)) {
            // User is logged in and "Remember Me" is enabled
            startActivity(Intent(this, Homepage::class.java))
            finish()
        }

        if (isUserLoggedIn()) {
            navigateToHomePage()
            return
        }

        registerBinding.login.setOnClickListener {
            startActivity(Intent(this, LoginPage::class.java))
        }

        registerBinding.google.setOnClickListener {
            signIn()
        }

        registerBinding.loginButton.setOnClickListener {
            val username = registerBinding.userNameEditText.text.toString().trim()
            val email = registerBinding.emailEditText.text.toString().trim()
            val mobile = registerBinding.mobileEditText.text.toString().trim()
            val password = registerBinding.passwordEditText.text.toString().trim()
            val confirmPassword = registerBinding.conformPaswordEditText.text.toString().trim()

            if (password == confirmPassword) {
                val hashedPassword = hashPassword(password)
                checkIfUserExists(username, email, mobile, hashedPassword)
            } else {
                showToast("Passwords do not match")
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)
        return email != null && password != null
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val email = user?.email ?: ""
                    val username = user?.displayName ?: ""

                    if (user != null) {
                        generateUserRefNumberAndSaveDetails(user.uid, username, email, "")
                        saveUserCredentials(email, "", username, "")
                        Toast.makeText(this, "Signed in as $username", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Homepage::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    private fun checkIfUserExists(
        username: String,
        email: String,
        mobile: String,
        hashedPassword: String
    ) {
        val usersRef = database.child("MyFitness").child("userDetails")
        var isEmailRegistered = false
        var isMobileRegistered = false

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    showToast("Email already registered")
                    isEmailRegistered = true
                    return
                }

                usersRef.orderByChild("mobile").equalTo(mobile)
                    .addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                showToast("Mobile number already registered")
                                isMobileRegistered = true
                                return
                            }

                            if (!isEmailRegistered && !isMobileRegistered) {
                                generateUserRefNumberAndSaveDetails(
                                    "", username, email, mobile, hashedPassword
                                )
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            showToast("Database read failed: ${error.message}")
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database read failed: ${error.message}")
            }
        })
    }

    private fun generateUserRefNumberAndSaveDetails(
        userId: String,
        username: String,
        email: String,
        mobile: String,
        hashedPassword: String = ""
    ) {
        val usersRef = database.child("MyFitness").child("userDetails")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRefNumber = "USER${1001 + snapshot.childrenCount.toInt()}"
                saveUserDetailsToDatabase(
                    userRefNumber,
                    userId,
                    username,
                    email,
                    mobile,
                    hashedPassword
                )
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database read failed: ${error.message}")
            }
        })
    }

    private fun saveUserDetailsToDatabase(
        userRefNumber: String,
        userId: String,
        username: String,
        email: String,
        mobile: String,
        hashedPassword: String
    ) {
        val userDetailsRef = database.child("MyFitness").child("userDetails").child(userRefNumber)

        val user = hashMapOf(
            "userId" to userRefNumber,
            "username" to username,
            "email" to email,
            "mobile" to mobile,
            "password" to hashedPassword,
            "profile_image" to "-",
            "total_purchase" to "0",
            "available_credits" to "0",
            "credits_spend" to "0",
            "date_of_birth" to "0"
        )

        userDetailsRef.setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("User details saved successfully")
                navigateToHomePage()
                finish()
            } else {
                showToast("Failed to save user details: ${task.exception?.message}")
            }
        }
    }

    private fun saveUserCredentials(
        email: String,
        password: String,
        username: String,
        mobile: String
    ) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.putString("username", username)
        editor.putString("mobile", mobile)
        editor.apply()
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, LoginPage::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
