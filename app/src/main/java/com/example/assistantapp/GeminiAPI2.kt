package com.example.assistantapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

val ReadModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
<<<<<<< HEAD
    apiKey = "Your API Key",
=======
    apiKey = "YOUR-API-KEY",
>>>>>>> 2eb069cbfd8030e7d6bd19e3db7f33c63d88ac8e
    generationConfig = generationConfig {
        temperature = 0.2f
        topK = 64
        topP = 0.95f
        maxOutputTokens = 8192
        responseMimeType = "text/plain"
    },
    safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
    ),
    systemInstruction = content { text("your user is a blind person.\nfrom the input the the user about the think and then read the whole text from the image like from the book sign boards and others.") },

    )

/// ... (keep existing imports and model configuration)

suspend fun sendFrameToGemini2AI(bitmap: Bitmap, onPartialResult: (String) -> Unit, onError: (String) -> Unit) {
    try {
        withContext(Dispatchers.IO) {
            val inputContent = content {
                image(bitmap)
                text("Read the text from this image and provide the content.")
            }

            var fullResponse = ""
            ReadModel.generateContentStream(inputContent).collect { chunk ->
                chunk.text?.let {
                    fullResponse += it
                    onPartialResult(it)
                }
            }
        }
    } catch (e: IOException) {
        Log.e("GeminiAI", "Network error: ${e.message}")
        onError("Network error: ${e.message}")
    } catch (e: Exception) {
        Log.e("GeminiAI", "Unexpected error: ${e.message}")
        onError("Unexpected error: ${e.message}")
    }
}
