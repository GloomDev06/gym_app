package com.fitness.myhealthfitness

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class FragmentSettings : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var profileListener: OnUserProfileClickListener? = null
    private var notificationListener: OnNotificationSettingsClickListener? = null
    private var walletListener: OnWalletClickListener? = null
    private var transactionListener: OnTransactionClickListener? = null
    private var shakeRequestListener: OnShakeRequestClickListener? = null
    private var faqListener: OnFaqClickListener? = null
    private var termsListener: OnTermsClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNotificationSettingsClickListener) {
            notificationListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnNotificationSettingsClickListener")
        }
        if (context is OnUserProfileClickListener) {
            profileListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnUserProfileClickListener")
        }
        if (context is OnWalletClickListener) {
            walletListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnWalletClickListener")
        }
        if (context is OnTransactionClickListener) {
            transactionListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnTransactionClickListener")
        }
        if (context is OnShakeRequestClickListener) {
            shakeRequestListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnShakeRequestClickListener")
        }
        if (context is OnFaqClickListener) {
            faqListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnFaqClickListener")
        }
        if (context is OnTermsClickListener) {
            termsListener = context
        } else {
            throw RuntimeException("$context must implement OnUserProfileClickListener or OnTermsClickListener")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        mAuth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val logoutButton: AppCompatButton = root.findViewById(R.id.logout)
        logoutButton.setOnClickListener {
            signOutAndClearPreferences()
        }

        val userProfileDet = root.findViewById<TextView>(R.id.userProfileDet)
        userProfileDet.setOnClickListener {
            profileListener?.onUserProfileClick()
        }

        val notificationSettingsButton =
            root.findViewById<TextView>(R.id.notificationSettingsButton)
        notificationSettingsButton.setOnClickListener {
            notificationListener?.onNotificationSettingsClick()
        }

        val walletButton = root.findViewById<TextView>(R.id.wallets)
        walletButton.setOnClickListener {
            walletListener?.onWalletClick()
        }

        val transactionHisReq = root.findViewById<TextView>(R.id.transactionHistory)
        transactionHisReq.setOnClickListener {
            transactionListener?.onTransactionClick()
        }

        val shakeHisReq = root.findViewById<TextView>(R.id.shakeReq)
        shakeHisReq.setOnClickListener {
            shakeRequestListener?.onShakeClick()
        }

        val emailUs = root.findViewById<TextView>(R.id.emailUs)
        emailUs.setOnClickListener {
            composeEmail()
        }

        val rateUs = root.findViewById<TextView>(R.id.rateUs)
        rateUs.setOnClickListener {
            showRateUsDialog()
        }

        val shareAppButton = root.findViewById<TextView>(R.id.shareApp)
        shareAppButton.setOnClickListener {
            shareApp()
        }

        val faq = root.findViewById<TextView>(R.id.faq)
        faq.setOnClickListener {
            faqListener?.onFaqClick()
        }

        val terms = root.findViewById<TextView>(R.id.termsOfUse)
        terms.setOnClickListener {
            termsListener?.onTermsClick()
        }


        return root
    }

    private fun shareApp() {
        val appPackageName = requireContext().packageName
        val appPlayStoreUrl = "https://play.google.com/store/apps/details?id=$appPackageName"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out $appPlayStoreUrl")
        }

        val activities: List<ResolveInfo> =
            requireContext().packageManager.queryIntentActivities(intent, 0)
        if (activities.isNotEmpty()) {
            val chooser = Intent.createChooser(intent, "Share via")
            startActivity(chooser)
        } else {
            showToast("No app found to handle sharing")
        }
    }


    private fun showRateUsDialog() {
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.review, null)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()

        val btnRateNow = dialogView.findViewById<AppCompatButton>(R.id.btn_rate_now)
        btnRateNow.setOnClickListener {
            alertDialog.dismiss()
            openPlayStoreForRating()
        }

        val btnLater = dialogView.findViewById<AppCompatButton>(R.id.btn_later)
        btnLater.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }


    private fun openPlayStoreForRating() {
        val appPackageName = requireContext().packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }


    private fun composeEmail() {
        val recipient = " info@akshayfitness.in"
        val subject = "App review - reg"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("No email app found")
        }
    }

    override fun onDetach() {
        super.onDetach()
        profileListener = null
        notificationListener = null
        walletListener = null
        transactionListener = null
        shakeRequestListener = null
    }

    private fun signOutAndClearPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        mAuth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(requireContext(), LoginPage::class.java)
                intent.putExtra("logout", true)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Google sign out failed: ${it.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showToast(message: String) {
        // Example showToast function
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    interface OnUserProfileClickListener {
        fun onUserProfileClick()
    }

    interface OnNotificationSettingsClickListener {
        fun onNotificationSettingsClick()
    }

    interface OnWalletClickListener {
        fun onWalletClick()
    }

    interface OnTransactionClickListener {
        fun onTransactionClick()
    }

    interface OnShakeRequestClickListener {
        fun onShakeClick()
    }

    interface OnFaqClickListener {
        fun onFaqClick()
    }

    interface OnTermsClickListener {
        fun onTermsClick()
    }
}
