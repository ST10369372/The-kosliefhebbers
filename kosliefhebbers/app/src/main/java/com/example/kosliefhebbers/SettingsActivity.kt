package com.example.kosliefhebbers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var themeSwitch: Switch
    private lateinit var notificationsSwitch: Switch
    private lateinit var progressBar: ProgressBar

    private lateinit var inputText: EditText
    private lateinit var translateButton: Button
    private lateinit var translatedText: TextView
    private lateinit var copyTranslatedButton: Button

    private lateinit var prefs: SharedPreferences
    private val geminiApiKey = "AIzaSyBgZSio8th3Z18eKfNw_SpmgKRCnYfLxlE"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        backButton = findViewById(R.id.backButton)
        languageSpinner = findViewById(R.id.languageSpinner)
        themeSwitch = findViewById(R.id.themeSwitch)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        progressBar = findViewById(R.id.progressBar)

        inputText = findViewById(R.id.inputText)
        translateButton = findViewById(R.id.translateButton)
        translatedText = findViewById(R.id.translatedText)
        copyTranslatedButton = findViewById(R.id.copyTranslatedButton)

        backButton.setOnClickListener { finish() }

        setupLanguageSpinner()
        setupThemeSwitch()
        setupNotificationsSwitch()

        // Translate Button
        translateButton.setOnClickListener {
            val textToTranslate = inputText.text.toString()
            val language = languageSpinner.selectedItem.toString()
            if (textToTranslate.isNotEmpty()) {
                translateText(textToTranslate, language)
            } else {
                Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show()
            }
        }

        // Copy Translated Text
        copyTranslatedButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Translated Text", translatedText.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    /** ----------------- LANGUAGE SETUP ------------------ **/
    private fun setupLanguageSpinner() {
        val languages = listOf("English", "Afrikaans", "Zulu", "Xhosa")
        val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        val savedLang = prefs.getString("language", "English")
        val selectedIndex = languages.indexOf(savedLang)
        if (selectedIndex in languages.indices) languageSpinner.setSelection(selectedIndex, false)
    }

    /** ----------------- THEME ------------------ **/
    private fun setupThemeSwitch() {
        val darkModeEnabled = prefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = darkModeEnabled
        AppCompatDelegate.setDefaultNightMode(if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("dark_mode", isChecked) }
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /** ----------------- NOTIFICATIONS ------------------ **/
    private fun setupNotificationsSwitch() {
        val notificationsEnabled = prefs.getBoolean("notifications", true)
        notificationsSwitch.isChecked = notificationsEnabled
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("notifications", isChecked) }
            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }
    }

    /** ----------------- GEMINI API TRANSLATION ------------------ **/
    private fun translateText(text: String, targetLanguage: String) {
        progressBar.visibility = View.VISIBLE

        val safeTarget = targetLanguage.ifBlank { "English" }
        val requestPrompt = """
            Detect the language of the following text and translate it accurately into $safeTarget.
            Text: "$text"
            Only return the translated text. Do not include explanations or extra information.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", requestPrompt) })
                    })
                })
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$geminiApiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    translatedText.text = ""
                    Toast.makeText(this@SettingsActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { progressBar.visibility = View.GONE }

                val responseBody = response.body.string()
                if (responseBody.isEmpty()) {
                    runOnUiThread {
                        translatedText.text = ""
                        Toast.makeText(this@SettingsActivity, "Empty response from API", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")

                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.optJSONObject(0)
                        val contentArray = candidate?.optJSONArray("content")
                        val partsArray = contentArray?.optJSONObject(0)?.optJSONArray("parts")
                        val translatedTextResult = partsArray?.optJSONObject(0)?.optString("text", "")?.trim() ?: ""

                        runOnUiThread {
                            if (translatedTextResult.isNotEmpty()) {
                                translatedText.text = translatedTextResult
                            } else {
                                translatedText.text = ""
                                Toast.makeText(this@SettingsActivity, "No translation found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            translatedText.text = ""
                            Toast.makeText(this@SettingsActivity, "No translation candidates found", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        translatedText.text = ""
                        Toast.makeText(this@SettingsActivity, "Error reading translation", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
