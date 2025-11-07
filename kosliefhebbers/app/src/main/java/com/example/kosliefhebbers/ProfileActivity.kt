package com.example.kosliefhebbers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var addRecipeButton: Button
    private lateinit var logoutButton: Button
    private lateinit var settingsButton: Button
    private lateinit var savedRecipesButton: Button
    private lateinit var auth: FirebaseAuth

    // image picker
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                profileImageView.setImageURI(it)
                Toast.makeText(this, "Profile image updated (local only)", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupListeners()
        displayUserInfo()
    }

    private fun initViews() {
        profileImageView = findViewById(R.id.profileImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        editProfileButton = findViewById(R.id.editProfileButton)
        addRecipeButton = findViewById(R.id.addRecipeButton)
        logoutButton = findViewById(R.id.logoutButton)
        settingsButton = findViewById(R.id.settingsButton)
        savedRecipesButton = findViewById(R.id.savedRecipesButton)

        // Ensure the back button exists before setting listener
        findViewById<ImageButton?>(R.id.backButton)?.setOnClickListener { finish() }
    }

    private fun setupListeners() {
        profileImageView.setOnClickListener { pickImage.launch("image/*") }

        editProfileButton.setOnClickListener {

                startActivity(Intent(this, EditProfileActivity::class.java))
        }


        addRecipeButton.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        savedRecipesButton.setOnClickListener {
            startActivity(Intent(this, SavedRecipesActivity::class.java))
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun displayUserInfo() {
        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            usernameTextView.text = "Guest"
            emailTextView.text = "No email available"
            Glide.with(this).load(R.drawable.ic_launcher_foreground).into(profileImageView)
        } else {
            usernameTextView.text = user.displayName ?: "Anonymous User"
            emailTextView.text = user.email ?: "No email available"
            user.photoUrl?.let {
                Glide.with(this).load(it).into(profileImageView)
            } ?: Glide.with(this).load(R.drawable.ic_launcher_foreground).into(profileImageView)
        }
    }
}
