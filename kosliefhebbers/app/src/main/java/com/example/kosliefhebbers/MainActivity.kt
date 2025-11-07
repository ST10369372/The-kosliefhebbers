package com.example.kosliefhebbers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.kosliefhebbers.R.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var recipePreviewContainer: LinearLayout
    private lateinit var showSavedRecipesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)

        // Initialize Firebase using your database URL
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference

        recipePreviewContainer = findViewById(id.recipePreviewContainer)
        showSavedRecipesButton = findViewById(id.showSavedRecipesButton)

        // Load saved recipes from Firebase when clicked
        showSavedRecipesButton.setOnClickListener {
            Log.d("MainActivity", "Show Saved Recipes button clicked")
            loadSavedRecipesFromFirebase()
        }

        // Other navigation buttons
        findViewById<Button>(id.homeButton).setOnClickListener { /* stay on home */ }
        findViewById<Button>(id.searchButton).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<Button>(id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────
    // Load Saved Recipes from Firebase Database (node = "recipes")
    // ─────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private fun loadSavedRecipesFromFirebase() {
        recipePreviewContainer.removeAllViews()
        val recipesRef = FirebaseDatabase.getInstance("https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("recipes")

        Log.d("MainActivity", "Loading saved recipes from Firebase...")

        recipesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipePreviewContainer.removeAllViews()

                if (!snapshot.exists()) {
                    Log.d("MainActivity", "No saved recipes found in Firebase.")
                    val noData = TextView(this@MainActivity)
                    noData.text = "No saved recipes found."
                    recipePreviewContainer.addView(noData)
                    return
                }

                for (recipeSnap in snapshot.children) {
                    val title = recipeSnap.child("title").getValue(String::class.java) ?: "Untitled"
                    val description = recipeSnap.child("summary").getValue(String::class.java) ?: "No description"
                    val category = recipeSnap.child("category").getValue(String::class.java) ?: "Uncategorized"

                    Log.d("MainActivity", "Loaded saved recipe: $title ($category)")
                    addRecipeCard(title, description, category)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error loading saved recipes: ${error.message}")
                val errorView = TextView(this@MainActivity)
                errorView.text = "Error loading saved recipes: ${error.message}"
                recipePreviewContainer.addView(errorView)
            }
        })
    }

    private fun addRecipeCard(title: String, description: String, category: String) {
        val inflater = LayoutInflater.from(this)
        val card = inflater.inflate(layout.recipe_card, recipePreviewContainer, false)
        card.findViewById<TextView>(id.recipeTitle).text = title
        card.findViewById<TextView>(id.recipeDescription).text = description
        card.findViewById<TextView>(id.recipeCategory).text = category
        recipePreviewContainer.addView(card)
    }
}
