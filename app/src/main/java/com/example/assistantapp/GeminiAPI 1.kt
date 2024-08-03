package com.example.assistantapp

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.*

val model = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyAu9uYZF2SNNMSkZrH89I0mqnigkPKiuPA",
    generationConfig = generationConfig {
        temperature = 1.5f
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
    systemInstruction = content { text("the data about the users sourounding will be shared with you so if user ask you about anything about his sourounding so you have tell him about like if the user ask how many trees are there so you have to tell him or if the user asks about if the road is paved or unpaved or if the user asks about the specfic things color.\nthe above is your one work you have to adapt your self according to the users need like the user ask you about any other things in the world other then the souroundings then you have to tell him about that\nuser user's are impaired persons when they ask you about there souroundings then you have to tell them and if they ask you about anything or any general question you also have to answer that and help the user as much possible as you can. \nkeep your answer short really short jsut give the user main idea that this is the answer do not explain it much.") },


    )

val chatHistory = listOf<Content>()

val chat = model.startChat(chatHistory)

suspend fun sendMessageToGeminiAI(message: String, frameData: String? = null): String {
    val fullMessage = if (frameData != null) {
        "Frame data: $frameData\n\nUser message: $message"
    } else {
        message
    }
    val response = chat.sendMessage(fullMessage)
    return response.text ?: "" // Provide a default value if response.text is null
}

fun main() = runBlocking {
    val response = sendMessageToGeminiAI("Hello, how can you help me?")
    println(response)
}
