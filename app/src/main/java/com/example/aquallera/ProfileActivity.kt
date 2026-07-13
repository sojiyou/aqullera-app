package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Header
    private lateinit var tvAvatarInitial: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    // Account Details
    private lateinit var tvUserFullName: TextView
    private lateinit var tvUserEmailDetail: TextView
    private lateinit var tvUserNumber: TextView

    // Email Verification
    private lateinit var verifiedBanner: LinearLayout
    private lateinit var unverifiedBanner: LinearLayout
    private lateinit var btnResendVerification: Button
    private lateinit var btnRefreshStatus: Button

    // Buttons
    private lateinit var btnEditProfile: Button
    private lateinit var btnMaps: Button
    private lateinit var btnSeeOrders: Button
    private lateinit var btnAbout: Button
    private lateinit var btnReportBug: Button
    private lateinit var btnLogOut: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
    }

    private fun initializeViews() {
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserFullName = findViewById(R.id.tvUserFullName)
        tvUserEmailDetail = findViewById(R.id.tvUserEmailDetail)
        tvUserNumber = findViewById(R.id.tvUserNumber)

        verifiedBanner = findViewById(R.id.verifiedBanner)
        unverifiedBanner = findViewById(R.id.unverifiedBanner)
        btnResendVerification = findViewById(R.id.btnResendVerification)
        btnRefreshStatus = findViewById(R.id.btnRefreshStatus)

        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnMaps = findViewById(R.id.btnMaps)
        btnSeeOrders = findViewById(R.id.btnSeeOrders)
        btnAbout = findViewById(R.id.btnAbout)
        btnReportBug = findViewById(R.id.btnReportBug)
        btnLogOut = findViewById(R.id.btnLogOut)
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnMaps.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        btnSeeOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnReportBug.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:aquallera.main@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Aquallera")
                putExtra(Intent.EXTRA_TEXT, "Please describe the issue you encountered in detail:\n\n")
            }
            startActivity(Intent.createChooser(intent, "Report Bug"))
        }

        btnLogOut.setOnClickListener {
            showLogoutConfirmation()
        }

        btnResendVerification.setOnClickListener {
            resendVerificationEmail()
        }

        btnRefreshStatus.setOnClickListener {
            refreshEmailStatus()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        val displayName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: currentUser.email?.substringBefore("@") ?: "User"
        val email = currentUser.email ?: ""

        tvAvatarInitial.text = displayName.first().uppercase()
        tvUserName.text = displayName
        tvUserEmail.text = email

        val database = FirebaseConfig.getDatabaseReference()
        database.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        tvUserFullName.text = user.fullName.ifBlank { displayName }
                        tvUserEmailDetail.text = user.email.ifBlank { email }
                        tvUserNumber.text = user.number.ifBlank { "N/A" }
                        tvUserName.text = user.fullName.ifBlank { displayName }
                        tvAvatarInitial.text = (user.fullName.firstOrNull()
                            ?: displayName.first()).uppercase()
                    } else {
                        tvUserFullName.text = displayName
                        tvUserEmailDetail.text = email
                        tvUserNumber.text = "N/A"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvUserFullName.text = displayName
                    tvUserEmailDetail.text = email
                    tvUserNumber.text = "N/A"
                }
            })

        updateEmailVerificationUI()
    }

    private fun updateEmailVerificationUI() {
        val isVerified = auth.currentUser?.emailVerified ?: false

        if (isVerified) {
            verifiedBanner.visibility = View.VISIBLE
            unverifiedBanner.visibility = View.GONE
        } else {
            verifiedBanner.visibility = View.GONE
            unverifiedBanner.visibility = View.VISIBLE
        }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser ?: return
        btnResendVerification.isEnabled = false
        btnResendVerification.text = "Sending..."

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                btnResendVerification.isEnabled = true
                btnResendVerification.text = "Resend verification email"
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send. Try again later.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun refreshEmailStatus() {
        val user = auth.currentUser ?: return
        btnRefreshStatus.isEnabled = false
        btnRefreshStatus.text = "Refreshing..."

        user.reload().addOnCompleteListener { task ->
            btnRefreshStatus.isEnabled = true
            btnRefreshStatus.text = "Refresh status"
            if (task.isSuccessful) {
                updateEmailVerificationUI()
                if (auth.currentUser?.emailVerified == true) {
                    Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Email not yet verified.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to refresh status.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setActiveTab(navProfile)

        navMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        navOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        navProfile.setOnClickListener {
            setActiveTab(navProfile)
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val allTabs = listOf(
            findViewById<LinearLayout>(R.id.navMap),
            findViewById<LinearLayout>(R.id.navOrder),
            findViewById<LinearLayout>(R.id.navProfile)
        )

        allTabs.forEach { tab ->
            val textView = when (tab.id) {
                R.id.navMap -> findViewById<TextView>(R.id.tvMap)
                R.id.navOrder -> findViewById<TextView>(R.id.tvOrder)
                R.id.navProfile -> findViewById<TextView>(R.id.tvProfile)
                else -> null
            }

            if (tab == activeTab) {
                textView?.setTextColor(Color.WHITE)
                textView?.alpha = 1.0f
            } else {
                textView?.setTextColor(ContextCompat.getColor(this, R.color.light_yellow))
                textView?.alpha = 0.6f
            }
            tab.background = null
        }
    }
}
