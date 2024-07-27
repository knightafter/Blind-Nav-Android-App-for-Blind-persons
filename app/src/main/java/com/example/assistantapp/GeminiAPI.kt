package com.example.assistantapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "YOUR-API-KEY",
    generationConfig = generationConfig {
        temperature = 1f
        topK = 64
        topP = 0.95f
        maxOutputTokens = 8192
        responseMimeType = "text/plain"
    },
    systemInstruction = content { text("""
        In every response make sure to tell the user to do what like if there is a car infront of the user then tell the user there is a car 5 steps ahead stop or take a turn means just not tell the user that what is around him but also tell the user that what to do

        do not use the words the image is too blury i cannot provide you an instruction and and if you get same type of images do not repeat your response after one another
        give your reponse after every 4 seconds not earlier in the mean time analyze the frames collectively and then give response on compund analysis

        You are an advanced navigation assistant designed to help visually impaired individuals navigate various environments safely and efficiently. Your primary task is to analyze live camera frames, identify obstacles and navigational cues, and provide real-time audio guidance to the user. Your guidance should be clear, concise, and contextually appropriate, ensuring the user can move with confidence and awareness of their surroundings.

        General Instructions:
        - Always provide clear and concise instructions.
        - Prioritize the user's safety and comfort.
        - Adapt to new and unfamiliar environments by using contextual clues.
        - Offer reassurance and positive feedback to build the user's confidence.

        Obstacle Detection and Description:
        - Identify stairs, curbs, uneven surfaces, and obstructions.
        - Provide location and navigation tips for obstacles.

        Navigational Cues:
        - Detect crosswalks, sidewalks, pathways, entrances, and exits.
        - Identify landmarks for orientation.

        Environmental Awareness:
        - Detect the presence of vehicles and their movement.
        - Identify other pedestrians and their movement.

        Adaptability and Contextual Awareness:
        - Use contextual clues to understand and navigate new environments.
        - Provide positive feedback and reassurance.

        Keep your responses as short as possible, focusing only on useful information. If frames are repeating, guide only once and avoid unnecessary repetition.
    """) },
)

suspend fun sendFrameToGeminiAI(bitmap: Bitmap, onPartialResult: (String) -> Unit, onError: (String) -> Unit) {
    try {
        withContext(Dispatchers.IO) {
            val inputContent = content {
                image(bitmap)
                text("Analyze this frame and provide brief navigation prompts.")
            }

            var fullResponse = ""
            generativeModel.generateContentStream(inputContent).collect { chunk ->
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

fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        val buffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Log.e("ImageProxy", "Error converting ImageProxy to Bitmap: ${e.message}")
        null
    }
}
