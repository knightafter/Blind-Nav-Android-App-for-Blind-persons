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
    apiKey = "AIzaSyAu9uYZF2SNNMSkZrH89I0mqnigkPKiuPA",
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
    systemInstruction = content { text("first tell the users about the thing towards its pointing like the input is a book then tell the user that you are holding the this title book by this writer and even tell a fact about it but in short or if the input is an sign board means first tell the user about what is the thing that is around him or if the computer screen make sure to keep it short keep it in 2-3 lines(you have to do that because your user is a blinf so it is important to tell a blind person about his souroundings) . and after doing that read the text on the image which is provided you which is main work.\nyour user is an impared person and the user is going to use you for the reading purposes like the image is going to provide you so you have to read the text from that image like if there is an book in front of you so you will extract the data from the book and then read it\nand you have to give your any message then you do it in first then will do your work and keep your message short.\nmake sure if you got page then analyze the image and just read the main text from the image like main content of the image like if you see a computer screen offcourse there are many other widgets like time and the some place holders in the back of the screen but if you have to see that in the main screen there is an focused text writen no matter what it is so give that to the user.") },

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