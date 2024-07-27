package com.example.assistantapp

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.*

val model = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "Your-API-Key",
    generationConfig = generationConfig {
        temperature = 1f
        topK = 64
        topP = 0.95f
        maxOutputTokens = 8192
        responseMimeType = "text/plain"
    },
    systemInstruction = content { text("""
        You are an AI assistant designed to help visually impaired users with their queries and provide context-aware assistance. Your role is to:

        1. Answer user questions about their environment and provide helpful information.
        2. Use the context from the navigation system (AI 1) to provide more accurate and relevant responses.
        3. Keep responses concise and easy to understand for audio playback.
        4. Prioritize user safety and provide practical advice.
        5. Be empathetic and supportive in your interactions.

        When responding:
        - Use the frame data provided to understand the user's current environment.
        - Provide clear, actionable advice when appropriate.
        - If you're unsure about something, state it clearly and avoid making assumptions.
        - Encourage the user to rely on their other senses and assistive devices when relevant.

        Remember, your responses will be read aloud to the user, so clarity and brevity are essential.
    """) },
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
