package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNumber: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnBack: TextView

    private var currentUserKey: String = ""
    private var originalEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadCurrentUserData()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etNumber = findViewById(R.id.etNumber)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        btnSaveProfile.setOnClickListener {
            if (validateInputs()) {
                updateUserProfile()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserKey = currentUser.uid

        val database = FirebaseConfig.getDatabaseReference()
        database.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        populateEditFields(user)
                    } else {
                        etFullName.setText(currentUser.displayName ?: "")
                        etEmail.setText(currentUser.email ?: "")
                        originalEmail = currentUser.email ?: ""
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    etFullName.setText(currentUser.displayName ?: "")
                    etEmail.setText(currentUser.email ?: "")
                    originalEmail = currentUser.email ?: ""
                }
            })
    }

    private fun populateEditFields(user: User) {
        etFullName.setText(user.fullName)
        etEmail.setText(user.email)
        etNumber.setText(user.number)
        originalEmail = user.email
    }

    private fun validateInputs(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            return false
        }

        if (number.isEmpty()) {
            etNumber.error = "Phone number is required"
            return false
        }

        return true
    }

    private fun updateUserProfile() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()

        if (currentUserKey.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseConfig.getDatabaseReference()
        val userRef = database.child("users").child(currentUserKey)

        val updatedUser = mapOf(
            "fullName" to fullName,
            "email" to email,
            "number" to number
        )

        userRef.updateChildren(updatedUser)
            .addOnSuccessListener {
                updateLocalUserData(fullName, email, number)
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLocalUserData(fullName: String, email: String, number: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", fullName)
            putString("user_email", email)
            putString("user_number", number)
            apply()
        }
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setInactiveTabs()

        navMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        navOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setInactiveTabs() {
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

            textView?.setTextColor(Color.parseColor("#757575"))
            tab.background = ContextCompat.getDrawable(this, R.drawable.tab_border_highlight)
        }
    }
}
