package com.example.kosliefhebbers

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * RegisterActivity
 * ------------------------------------------------------------
 * Handles user account creation via Firebase Authentication
 * and stores basic user info (username, email) in Realtime DB.
 * ------------------------------------------------------------
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/")

        // Bind UI elements
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)

        // Register button listener
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase user creation
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: return@addOnSuccessListener
                    val userMap = mapOf("username" to username, "email" to email)

                    database.reference.child("Users").child(userId)
                        .setValue(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save data: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
