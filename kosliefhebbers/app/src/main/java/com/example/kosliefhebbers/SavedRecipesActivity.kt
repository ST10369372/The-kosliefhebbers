package com.example.kosliefhebbers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kosliefhebbers.adapter.SavedRecipeAdapter
import com.example.kosliefhebbers.models.SavedRecipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SavedRecipesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedRecipeAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val recipeList = mutableListOf<SavedRecipe>() // list for adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_recipes)

        // Initialize views
        recyclerView = findViewById(R.id.savedRecipesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SavedRecipeAdapter(recipeList)
        recyclerView.adapter = adapter

        // Firebase setup
        auth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        // Load recipes
        loadSavedRecipes()
    }

    private fun loadSavedRecipes() {
        val recipesRef = database.child("recipes")
        recipesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipeList.clear()
                if (!snapshot.exists()) {
                    Toast.makeText(
                        this@SavedRecipesActivity,
                        "No saved recipes found",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter.notifyDataSetChanged()
                    return
                }

                for (recipeSnap in snapshot.children) {
                    val title = recipeSnap.child("title").getValue(String::class.java) ?: "Untitled"
                    val description =
                        recipeSnap.child("summary").getValue(String::class.java) ?: "No description"
                    val category =
                        recipeSnap.child("category").getValue(String::class.java) ?: "Uncategorized"

                    val recipe = SavedRecipe(title, description, category)
                    recipeList.add(recipe)
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SavedRecipesActivity,
                    "Error loading recipes: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}