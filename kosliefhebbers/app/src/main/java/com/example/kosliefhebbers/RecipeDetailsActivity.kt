package com.example.kosliefhebbers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var titleView: TextView
    private lateinit var summaryView: TextView
    private lateinit var copyButton: Button
    private lateinit var saveButton: Button

    private val apiKey = "7ad9b47a8dcd40548802e94a4c913b3d"
    private val client = OkHttpClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // ✅ holds the entire formatted recipe text
    private var fullRecipeText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        imageView = findViewById(R.id.recipeImage)
        titleView = findViewById(R.id.recipeTitle)
        summaryView = findViewById(R.id.recipeSummary)
        copyButton = findViewById(R.id.copyButton)
        saveButton = findViewById(R.id.saveButton)

        val recipeId = intent.getStringExtra("recipeId")
        if (!recipeId.isNullOrEmpty()) {
            fetchRecipeDetails(recipeId)
        } else {
            Toast.makeText(this, "Recipe ID not found", Toast.LENGTH_SHORT).show()
        }

        copyButton.setOnClickListener {
            copyFullRecipeToClipboard()
        }

        saveButton.setOnClickListener {
            saveRecipeToFirebase()
        }
    }

    private fun fetchRecipeDetails(recipeId: String) {
        coroutineScope.launch {
            try {
                val url = "https://api.spoonacular.com/recipes/$recipeId/information?apiKey=$apiKey"
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                val json = response.body.string()
                if (json.isEmpty()) {
                    Toast.makeText(this@RecipeDetailsActivity, "Empty response from API", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val obj = JSONObject(json)
                val title = obj.optString("title", "Untitled Recipe")
                val image = obj.optString("image", "")
                val summary = obj.optString("summary", "No summary available.")

                val ingredientsArray = obj.optJSONArray("extendedIngredients") ?: JSONArray()
                val ingredientsList = mutableListOf<String>()
                for (i in 0 until ingredientsArray.length()) {
                    val ingObj = ingredientsArray.getJSONObject(i)
                    val name = ingObj.optString("original", "")
                    if (name.isNotEmpty()) ingredientsList.add(name)
                }

                var instructions = obj.optString("instructions", "")
                if (instructions.isEmpty()) {
                    val analyzedArray = obj.optJSONArray("analyzedInstructions")
                    if (analyzedArray != null && analyzedArray.length() > 0) {
                        val firstObj = analyzedArray.getJSONObject(0)
                        val stepsArray = firstObj.optJSONArray("steps")
                        val builder = StringBuilder()
                        if (stepsArray != null) {
                            for (i in 0 until stepsArray.length()) {
                                val stepObj = stepsArray.getJSONObject(i)
                                val number = stepObj.optInt("number", i + 1)
                                val step = stepObj.optString("step", "")
                                builder.append("$number. $step<br>")
                            }
                        }
                        instructions = builder.toString()
                    }
                }

                // ✅ Build full recipe text for display and copying
                val finalHtml = """
                    <b>Title:</b><br>$title<br><br>
                    <b>Summary:</b><br>$summary<br><br>
                    <b>Ingredients:</b><br>${ingredientsList.joinToString("<br>") { "• $it" }}<br><br>
                    <b>Instructions:</b><br>${if (instructions.isNotEmpty()) instructions else "No instructions available."}
                """.trimIndent()

                // ✅ Store this for copying
                fullRecipeText = Html.fromHtml(finalHtml, Html.FROM_HTML_MODE_LEGACY).toString()

                // ✅ Show formatted version in the UI
                titleView.text = title
                summaryView.text = formatHtml(finalHtml)

                Glide.with(this@RecipeDetailsActivity)
                    .load(image)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imageView)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RecipeDetailsActivity, "Failed to fetch recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Fixed: copies the full recipe text
    private fun copyFullRecipeToClipboard() {
        if (fullRecipeText.isEmpty()) {
            Toast.makeText(this, "Nothing to copy yet!", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Full Recipe", fullRecipeText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Full recipe copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun saveRecipeToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save recipes", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeTitle = titleView.text.toString()
        val recipeFullText = fullRecipeText // entire recipe: title, summary, ingredients, instructions
        val category = "Saved" // optional

        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)
            .child("saved_recipes")

        val recipeId = databaseRef.push().key ?: return
        val recipeMap = mapOf(
            "title" to recipeTitle,
            "fullRecipe" to recipeFullText,  // save entire recipe here
            "category" to category
        )

        databaseRef.child(recipeId).setValue(recipeMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun formatHtml(html: String): Spanned {
        @Suppress("DEPRECATION")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
