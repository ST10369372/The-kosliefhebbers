package com.example.kosliefhebbers.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kosliefhebbers.R
import com.example.kosliefhebbers.models.SavedRecipe

class SavedRecipeAdapter(private val recipeList: List<SavedRecipe>) :
    RecyclerView.Adapter<SavedRecipeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.recipeTitle)
        val descriptionText: TextView = view.findViewById(R.id.recipeDescription)
        val categoryText: TextView = view.findViewById(R.id.recipeCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.titleText.text = recipe.title
        holder.descriptionText.text = recipe.description
        holder.categoryText.text = recipe.category
    }

    override fun getItemCount(): Int = recipeList.size
}
