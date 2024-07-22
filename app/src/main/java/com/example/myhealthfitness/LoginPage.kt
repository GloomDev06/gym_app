package com.fitness.myhealthfitness

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fitness.myhealthfitness.databinding.ActivityLoginPageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class LoginPage : AppCompatActivity() {

    private lateinit var loginPageBinding: ActivityLoginPageBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loginPageBinding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(loginPageBinding.root)

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
        }

        val currentUser = auth.currentUser

        if (currentUser != null && sharedPreferences.getBoolean("rememberMe", false)) {
            startActivity(Intent(this, Homepage::class.java))
            finish()
        }

        loginPageBinding.switchRememberMe.isChecked = sharedPreferences.getBoolean("rememberMe", false)

        loginPageBinding.switchRememberMe.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("rememberMe", isChecked)
            if (!isChecked) {
                editor.remove("email")
                editor.remove("password")
            }
            editor.apply()
        }

        loginPageBinding.google.setOnClickListener {
            signIn()
        }

        loginPageBinding.loginButton.setOnClickListener {
            val email = loginPageBinding.emailEditText.text.toString().trim()
            val password = loginPageBinding.passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                val hashedPassword = hashPassword(password)
                checkCredentials(email, hashedPassword)
            } else {
                showToast("Please enter email and password")
            }
        }

        loginPageBinding.login.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }
        // Add click listener to mobile ImageView for OTP login
        loginPageBinding.mobile.setOnClickListener {
            startOTPLogin()
        }
    }

    private fun startOTPLogin() {
        // Prompt user to enter their mobile number
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_mobile_input, null)
        val mobileEditText = view.findViewById<EditText>(R.id.mobileEditText)
        builder.setView(view)
        builder.setPositiveButton("Send OTP") { _, _ ->
            val mobileNumber = mobileEditText.text.toString().trim()
            if (mobileNumber.isNotEmpty()) {
                sendOTPToMobileLogin(mobileNumber)
            } else {
                showToast("Please enter a mobile number")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun sendOTPToMobileLogin(mobile: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(mobile)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Automatically verifies and signs in the user.
                    showToast("Verification completed")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showToast("Verification failed: ${e.message}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verificationId, token)
                    showToast("OTP sent to mobile number")
                    // Store verificationId and token for use in verification step
                    this@LoginPage.verificationId = verificationId
                    this@LoginPage.resendToken = token
                    // Prompt user to enter the OTP
                    promptUserForOTP()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun promptUserForOTP() {
        // Prompt user to enter the OTP
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_otp_input, null)
        val otpEditText = view.findViewById<EditText>(R.id.otpEditText)
        builder.setView(view)
        builder.setPositiveButton("Verify") { _, _ ->
            val otp = otpEditText.text.toString().trim()
            if (otp.isNotEmpty()) {
                verifyOTP(otp)
            } else {
                showToast("Please enter the OTP")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun verifyOTP(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    showToast("Signed in as ${user?.phoneNumber}")
                    navigateToHomePage()
                } else {
                    showToast("Authentication failed")
                }
            }
    }

    private fun isUserLoggedIn(): Boolean {
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)
        return email != null && password != null
    }

    private fun saveUserCredentials(email: String, password: String) {
        if (loginPageBinding.switchRememberMe.isChecked) {
            val editor = sharedPreferences.edit()
            editor.putString("email", email)
            editor.putString("password", password)
            editor.apply()
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    private fun checkCredentials(email: String, hashedPassword: String) {
        database.child("MyFitness").child("userDetails")
            .orderByChild("email")
            .equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null && user.password == hashedPassword) {
                                showToast("Login successful!")
                                saveUserDataToSharedPreferences(user)
                                saveUserCredentials(email, hashedPassword)
                                navigateToHomePage()
                                return
                            }
                        }
                        showToast("Invalid email or password")
                    } else {
                        showToast("Invalid email or password")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error: ${error.message}")
                }
            })
    }

    private fun saveUserDataToSharedPreferences(user: User) {
        val editor = sharedPreferences.edit()
        editor.putString("userId", user.userId)
        editor.putString("email", user.email)
        editor.putString("mobile", user.mobile)
        editor.putString("username", user.username)
        editor.apply()
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, Homepage::class.java)
        startActivity(intent)
        finish()
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this, Homepage::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendForgotPasswordOTP(email: String) {
        database.child("MyFitness").child("userDetails")
            .orderByChild("email")
            .equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                sendOTPToEmail(email)
                                user.mobile?.let { sendOTPToMobile(it) }
                                showToast("OTP sent to both email and mobile number")
                                return
                            }
                        }
                        showToast("User not found")
                    } else {
                        showToast("User not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error: ${error.message}")
                }
            })
    }

    private fun sendOTPToEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("OTP sent to email")
                } else {
                    showToast("Failed to send OTP to email")
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
                    // Automatically verifies and signs in the user.
                    showToast("Verification completed")
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showToast("Verification failed: ${e.message}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verificationId, token)
                    showToast("OTP sent to mobile number")
                    // Store verificationId and token for use in verification step
                    this@LoginPage.verificationId = verificationId
                    this@LoginPage.resendToken = token
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}