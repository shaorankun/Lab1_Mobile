package com.example.lab1_mobile

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val API_KEY = BuildConfig.GEMINI_API_KEY
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText = findViewById<EditText>(R.id.inputText)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val emojiText = findViewById<TextView>(R.id.emojiText)
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)

        submitBtn.setOnClickListener {
            val sentence = inputText.text.toString().trim()
            if (sentence.isEmpty()) {
                Toast.makeText(this, "Please enter a sentence", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            analyzeSentiment(sentence, emojiText, rootLayout)
        }
    }

    private fun analyzeSentiment(
        sentence: String,
        emojiText: TextView,
        rootLayout: LinearLayout
    ) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY"

        val prompt = """
            Analyze the sentiment of this sentence: "$sentence"
            Reply with ONLY one word: POSITIVE or NEGATIVE
        """.trimIndent()

        val json = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return

                android.util.Log.d("SENTIMENT", "Response: $responseBody")

                try {
                    val result = JSONObject(responseBody)
                    val text = result
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                        .uppercase()

                    runOnUiThread {
                        if (text.contains("POSITIVE")) {
                            rootLayout.setBackgroundColor(Color.GREEN)
                            emojiText.text = "😊"
                        } else {
                            rootLayout.setBackgroundColor(Color.RED)
                            emojiText.text = "☹️"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Parse error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

// For manual check
//    private fun analyzeSentiment(
//        sentence: String,
//        emojiText: TextView,
//        rootLayout: LinearLayout
//    ) {
//        val positiveWords = setOf(
//            "good", "great", "love", "like", "awesome", "happy", "excellent",
//            "wonderful", "fantastic", "nice", "best", "fun", "enjoy", "glad",
//            "superb", "amazing", "brilliant", "let's", "yes", "sure", "please",
//            "yay", "excited", "delicious", "perfect", "beautiful", "cool"
//        )
//
//        val negativeWords = setOf(
//            "bad", "hate", "dislike", "awful", "terrible", "sad", "worst",
//            "horrible", "no", "not", "never", "boring", "ugly", "poor",
//            "disgusting", "gross", "annoying", "wrong", "unfortunately",
//            "don't", "won't", "can't", "ugh", "nope", "thanks no", "no thanks"
//        )
//
//        val words = sentence.lowercase()
//            .replace("'", "'")  // normalize apostrophes
//            .split(" ", ",", ".", "!", "?")
//            .filter { it.isNotEmpty() }
//
//        // Also check two-word combos like "no thanks", "don't like"
//        val bigrams = (0 until words.size - 1).map { "${words[it]} ${words[it+1]}" }
//
//        var score = 0
//        for (word in words) {
//            if (positiveWords.contains(word)) score++
//            if (negativeWords.contains(word)) score--
//        }
//        for (bigram in bigrams) {
//            if (negativeWords.contains(bigram)) score -= 2  // stronger penalty
//        }
//
//        runOnUiThread {
//            if (score >= 0) {
//                rootLayout.setBackgroundColor(Color.parseColor("#4CAF50"))
//                emojiText.text = "😊"
//            } else {
//                rootLayout.setBackgroundColor(Color.parseColor("#8B0000"))
//                emojiText.text = "☹️"
//            }
//        }
//    }
}