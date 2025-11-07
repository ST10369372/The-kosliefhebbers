package com.example.kosliefhebbers

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kosliefhebbers.adapter.RecipeAdapter
import com.example.kosliefhebbers.models.Recipe
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val recipes = mutableListOf<Recipe>()
    private lateinit var adapter: RecipeAdapter
    private val client = OkHttpClient()
    private val apiKey = "7ad9b47a8dcd40548802e94a4c913b3d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        adapter = RecipeAdapter(this, recipes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // go back to Home
        }

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter a recipe name", Toast.LENGTH_SHORT).show()
            } else {
                searchRecipes(query)
            }
        }
    }

    private fun searchRecipes(query: String) {
        progressBar.visibility = View.VISIBLE
        recipes.clear()
        adapter.notifyDataSetChanged()

        val url =
            "https://api.spoonacular.com/recipes/complexSearch?query=$query&number=15&addRecipeInformation=true&apiKey=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@SearchActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@SearchActivity, "Empty API response", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: JSONArray()
                    val fetched = mutableListOf<Recipe>()

                    for (i in 0 until results.length()) {
                        val r = results.getJSONObject(i)
                        val id = r.optInt("id", 0).toString()
                        val title = r.optString("title", "Untitled")
                        val image = r.optString("image", "")
                        val summary = r.optString("summary", "")
                        val instructions = r.optString("instructions", "")

                        // Ingredients safely
                        val ingredients = mutableListOf<String>()
                        val ingArray = r.optJSONArray("extendedIngredients") ?: JSONArray()
                        for (j in 0 until ingArray.length()) {
                            val name = ingArray.getJSONObject(j).optString("original", "")
                            if (name.isNotEmpty()) ingredients.add(name)
                        }

                        fetched.add(Recipe(id, title, image, summary, instructions, ingredients))
                    }

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (fetched.isEmpty()) {
                            Toast.makeText(this@SearchActivity, "No recipes found for \"$query\"", Toast.LENGTH_SHORT).show()
                        } else {
                            recipes.addAll(fetched)
                            adapter.notifyDataSetChanged()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@SearchActivity, "Error parsing API data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}

