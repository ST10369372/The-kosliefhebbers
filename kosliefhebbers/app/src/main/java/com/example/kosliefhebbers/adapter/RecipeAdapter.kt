package com.example.kosliefhebbers.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kosliefhebbers.R
import com.example.kosliefhebbers.RecipeDetailsActivity
import com.example.kosliefhebbers.models.Recipe

class RecipeAdapter(
    private val context: Context,
    private val recipes: List<Recipe>
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.recipeImage)
        val title: TextView = view.findViewById(R.id.recipeTitle)
        val summary: TextView = view.findViewById(R.id.recipeSummary)
        val saveButton: Button = view.findViewById(R.id.saveButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_safe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.summary.text = recipe.summary ?: "No summary available."

        // Safe image loading
        if (!recipe.image.isNullOrEmpty()) {
            Glide.with(context)
                .load(recipe.image)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background)
        }

        // Open RecipeDetailsActivity safely
        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeDetailsActivity::class.java)
            intent.putExtra("recipeId", recipe.id)
            context.startActivity(intent)
        }

        // Optional: Save button
        holder.saveButton.setOnClickListener {
            Toast.makeText(context, "Saved recipe: ${recipe.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = recipes.size
}
