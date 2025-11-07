package com.example.kosliefhebbers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var summaryEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var languageSpinner: Spinner

    private val database = FirebaseDatabase.getInstance(
        "https://kosliefhebbers-52c13-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )
    private val client = OkHttpClient()
    private val geminiApiKey = "AIzaSyBgZSio8th3Z18eKfNw_SpmgKRCnYfLxlE"
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        titleEditText = findViewById(R.id.titleEditText)
        summaryEditText = findViewById(R.id.summaryEditText)
        categoryEditText = findViewById(R.id.categoryEditText)
        saveButton = findViewById(R.id.saveButton)
        languageSpinner = findViewById(R.id.languageSpinner) // Add Spinner in layout

        createNotificationChannel()
        checkNotificationPermission()
        setupLanguageSpinner()

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val summary = summaryEditText.text.toString().trim()
            val category = categoryEditText.text.toString().trim()
            val language = languageSpinner.selectedItem.toString()

            if (title.isEmpty() || summary.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val recipeId = database.reference.child("recipes").push().key ?: return@setOnClickListener
            val recipeData = mapOf(
                "title" to title,
                "summary" to summary,
                "category" to category
            )

            database.reference.child("recipes").child(recipeId).setValue(recipeData)
                .addOnSuccessListener {
                    notifyNewRecipe(title)
                    translateRecipe(summary, language)
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save recipe: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupLanguageSpinner() {
        val languages = listOf("English", "Afrikaans", "Zulu", "Xhosa")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
    }

    private fun translateRecipe(text: String, language: String) {
        val prompt = "Translate this recipe to $language:\n$text"
        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                })
            })
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val translated = JSONObject(body)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    runOnUiThread {
                        Toast.makeText(this@AddRecipeActivity, "Translated: $translated", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "recipe_channel",
                "New Recipes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun notifyNewRecipe(title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val builder = NotificationCompat.Builder(this, "recipe_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Recipe Added")
            .setContentText("Recipe \"$title\" has been added!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
