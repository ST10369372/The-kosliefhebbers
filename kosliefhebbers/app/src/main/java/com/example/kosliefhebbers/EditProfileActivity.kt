package com.example.kosliefhebbers

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.example.kosliefhebbers.models.User

class EditProfileActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var saveChangesButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        saveChangesButton = findViewById(R.id.saveChangesButton)
        backButton = findViewById(R.id.backButton)

        // 🔙 Handle Back Button
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ Use your database URL explicitly
        val dbRef = FirebaseDatabase.getInstance("https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("users")
            .child(user.uid)

        // 🔹 Load existing user info
        dbRef.get().addOnSuccessListener { snapshot ->
            usernameInput.setText(snapshot.child("username").value?.toString() ?: user.displayName ?: "")
            emailInput.setText(snapshot.child("email").value?.toString() ?: user.email ?: "")
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Handle Save Button
        saveChangesButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (username.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Username and Email are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isNotEmpty() && password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()

            user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                if (!profileTask.isSuccessful) {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                if (email != user.email) {
                    user.updateEmail(email).addOnFailureListener {
                        Toast.makeText(this, "Failed to update email", Toast.LENGTH_SHORT).show()
                    }
                }

                if (password.isNotEmpty()) {
                    user.updatePassword(password).addOnFailureListener {
                        Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                    }
                }

                val updatesMap = User(user.uid, username, email)
                dbRef.setValue(updatesMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "❌ Database update failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
