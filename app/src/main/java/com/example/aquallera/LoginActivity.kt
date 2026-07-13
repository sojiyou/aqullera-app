package com.example.aquallera

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView
    private lateinit var btnBack: View
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()

        // Footer globe link
        findViewById<ImageView>(R.id.GlobeIcon)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aquallera-website.vercel.app/"))
            startActivity(intent)
        }

        // Check if user is already logged in
        checkExistingSession()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.LoginEmailInput)
        etPassword = findViewById(R.id.LoginPasswordInput)
        btnLogin = findViewById(R.id.LoginButton)
        tvSignup = findViewById(R.id.GoToSignupBtn)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun checkExistingSession() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (isLoggedIn && auth.currentUser != null) {
            // User is already logged in, redirect to map
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Processing..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase Auth login successful, now get user data from Realtime Database
                    val user = auth.currentUser
                    if (user != null) {
                        fetchUserDataFromDatabase(user.uid)
                    }
                } else {
                    loginError("Login failed: wrong email or password")
                }
            }
    }

    private fun fetchUserDataFromDatabase(uid: String) {
        val database = FirebaseConfig.getDatabaseReference()
        val userRef = database.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        loginSuccess(user)
                    } else {
                        loginError("User data not found.")
                    }
                } else {
                    // Auto-provision user record if missing
                    val userId = uid
                    val database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")

                    database.reference.child("waterStations").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(stationSnapshot: DataSnapshot) {
                                val userMap = if (stationSnapshot.exists()) {
                                    val ownerName = stationSnapshot.child("ownerName").getValue(String::class.java) ?: ""
                                    val email = stationSnapshot.child("email").getValue(String::class.java) ?: ""
                                    val phone = stationSnapshot.child("phone").getValue(String::class.java) ?: ""
                                    mapOf(
                                        "uid" to userId,
                                        "fullName" to ownerName,
                                        "name" to ownerName,
                                        "email" to email,
                                        "number" to phone,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                } else {
                                    mapOf(
                                        "uid" to userId,
                                        "email" to (auth.currentUser?.email ?: ""),
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                }
                                database.reference.child("users").child(userId).setValue(userMap)
                                    .addOnSuccessListener {
                                        val provisionedUser = User(
                                            uid = userId,
                                            fullName = userMap["fullName"] as? String ?: "",
                                            email = userMap["email"] as? String ?: "",
                                            number = userMap["number"] as? String ?: ""
                                        )
                                        loginSuccess(provisionedUser)
                                    }
                                    .addOnFailureListener {
                                        loginError("Failed to create user profile.")
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                loginError("Database error. Please try again.")
                            }
                        })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                loginError("Database error. Please try again.")
            }
        })
    }

    private fun loginSuccess(user: User) {
        saveUserSession(user)
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun loginError(errorMessage: String) {
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun saveUserSession(user: User) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_uid", user.uid)
            putString("user_name", user.fullName)
            putString("user_email", user.email)
            putString("user_number", user.number)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }

        tvSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}