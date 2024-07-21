package com.fitness.myhealthfitness

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.fitness.myhealthfitness.databinding.ActivityProductPackagesBinding
import com.google.firebase.database.*
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductPackages : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var productPackagesBinding: ActivityProductPackagesBinding
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var userId: String? = null
    private lateinit var productId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        productPackagesBinding = ActivityProductPackagesBinding.inflate(layoutInflater)
        setContentView(productPackagesBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val productTitle: TextView = findViewById(R.id.productName)
        val productCredits: TextView = findViewById(R.id.productCredit)
        val productPrice: TextView = findViewById(R.id.productPrice)
        val productDescription: TextView = findViewById(R.id.productDescription)
        val currentCredits: TextView = findViewById(R.id.currentCredits)
        val productImage: ImageView = findViewById(R.id.productImage)

        val bundle: Bundle? = intent.extras
        val title = bundle!!.getString("productTitle")
        val credits = bundle.getString("productCredits")
        val price = bundle.getString("productPrice")
        val description = bundle.getString("productDescription")
        val productImageURL = bundle.getString("productImage")


        productTitle.text = "Package Name: $title"
        productCredits.text = "Credits: $credits"
        productPrice.text = "Price: $price"
        productDescription.text = "$description"
        Glide.with(this)
            .load(productImageURL)
            .placeholder(R.drawable.energy_drink)
            .error(R.drawable.energy_drink)
            .into(productImage)


        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "-")


        database = FirebaseDatabase.getInstance().reference
        fetchCurrentCredits(currentCredits)

        // Calculate required credits after purchase
        val productCreditsValue = credits?.toInt() ?: 0
        val currentCreditsValue =
            currentCredits.text.toString().replace(Regex("[^\\d]"), "").toIntOrNull() ?: 0
        calculateRequiredCredits(productCreditsValue, currentCreditsValue)

        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)

        // Set OnClickListener for paymentButton
        productPackagesBinding.paymentButton.setOnClickListener {
            price?.let { it1 -> initPayment(it1.toInt()) }
        }

        productPackagesBinding.cancelBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun fetchCurrentCredits(currentCredits: TextView) {
        userId?.let { id ->
            database.child("MyFitness").child("userDetails").child(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                currentCredits.text =
                                    "Your Current Credits: ${it.available_credits}"

                                if (it.available_credits == "0 Scores") {
                                    findViewById<TextView>(R.id.goldMember).visibility = View.GONE
                                }
                            }
                        } else {
                            currentCredits.text = "Your Current Credits: 0"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch current credits: ${error.message}")
                    }
                })
        }
    }

    private fun calculateRequiredCredits(productCredits: Int, currentCredits: Int) {
        val totalCredits = productCredits + currentCredits
        productPackagesBinding.requiredCredits.text = "Total credits after buy: $totalCredits"
    }

    private fun initPayment(productCredits: Int) {
        val activity: Activity = this
        val co = Checkout()

        try {
            val amount =
                productCredits * 100
            val options = JSONObject()
            options.put("name", "Akshay")
            options.put("description", "Weight loss with Akshay Fitness")
            options.put("image", "http://example.com/image/rzp.jpg")
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")
            options.put("amount", amount.toString())

            val prefill = JSONObject()
            prefill.put("email", "info@akshayfitness.in")

            options.put("prefill", prefill)
            co.open(activity, options)
        } catch (e: Exception) {
            Toast.makeText(activity, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        savePaymentStatusToSharedPreferences("success", getProductCredits())
        Toast.makeText(this, "Payment success", Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        savePaymentStatusToSharedPreferences("failure", getProductCredits())
        val description =
            p1?.substringAfter("description: ")?.substringBefore(",") ?: "Unknown error"
        Toast.makeText(this, "Error: $description", Toast.LENGTH_SHORT).show()
    }

    private fun saveUserDataToSharedPreferences(
        user: User,
        productCredits: Int,
        currentDate: String,
        action: String
    ) {
        val editor = sharedPreferences.edit()
        editor.putString("userId", user.userId)
        editor.putString("email", user.email)
        editor.putString("mobile", user.mobile)
        editor.putString("username", user.username)
        editor.putInt("productCredits", productCredits)
        editor.putString("currentDate", currentDate)
        editor.putString("action", action)
        editor.apply()
    }

    private fun savePaymentStatusToSharedPreferences(action: String, productCredits: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        sharedPreferences.getString("userId", "-")?.let { userId ->
            database.child("MyFitness").child("userTransactionDetails").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                saveUserDataToSharedPreferences(
                                    it,
                                    productCredits,
                                    currentDate,
                                    action
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Failed to fetch user details: ${error.message}")
                    }
                })
        }
    }

    private fun getProductCredits(): Int {
        return productPackagesBinding.productCredit.text.toString().replace(Regex("[^\\d]"), "")
            .toIntOrNull() ?: 0
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
