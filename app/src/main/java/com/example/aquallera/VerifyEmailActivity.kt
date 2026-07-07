package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserEmail: TextView
    private lateinit var tvStatusMsg: TextView
    private lateinit var btnResend: Button
    private lateinit var tvBackToLogin: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = 5000L
    private var isResending = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkEmailVerification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        if (user.isEmailVerified) {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
            return
        }

        tvUserEmail.text = user.email ?: ""
        startPolling()
    }

    private fun initializeViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvStatusMsg = findViewById(R.id.tvStatusMsg)
        btnResend = findViewById(R.id.btnResend)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupClickListeners() {
        btnResend.setOnClickListener { resendVerificationEmail() }

        tvBackToLogin.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun startPolling() {
        handler.postDelayed(pollRunnable, pollInterval)
    }

    private fun stopPolling() {
        handler.removeCallbacks(pollRunnable)
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser ?: run {
            stopPolling()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        user.reload().addOnCompleteListener {
            if (auth.currentUser?.isEmailVerified == true) {
                stopPolling()
                startActivity(Intent(this, MapActivity::class.java))
                finish()
            } else {
                startPolling()
            }
        }
    }

    private fun resendVerificationEmail() {
        if (isResending) return
        isResending = true
        btnResend.isEnabled = false
        btnResend.text = "Sending..."

        val user = auth.currentUser
        if (user == null) {
            showStatus("Error: No user found")
            resetResendButton()
            return
        }

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showStatus("Verification email sent! Check your inbox.")
                } else {
                    val errorMsg = task.exception?.message ?: "Failed to send verification email"
                    showStatus(errorMsg)
                }
                resetResendButton()
            }
    }

    private fun resetResendButton() {
        isResending = false
        btnResend.isEnabled = true
        btnResend.text = "Resend verification email"
    }

    private fun showStatus(message: String) {
        tvStatusMsg.text = message
        tvStatusMsg.visibility = TextView.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }
}
