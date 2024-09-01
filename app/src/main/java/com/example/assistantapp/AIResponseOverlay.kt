package com.example.assistantapp

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


@Composable
fun AIResponseOverlay(
    currentMode: String,
    navigationResponse: String,
    chatResponse: String,
    readingModeResult: String,
    tts: TextToSpeech?,
    lastSpokenIndex: Int,
    response: String
) {
    val context = LocalContext.current
    var isConnected = remember { mutableStateOf(isInternetAvailable(context)) }
    var currentIndex by remember { mutableStateOf(lastSpokenIndex) } // Track the current sentence index
    val sentences = response.split(".") // Split the response into sentences
    var lastSpokenText by remember { mutableStateOf("") } // Track the last spoken text

    // Continuously check internet connectivity
    LaunchedEffect(Unit) {
        while (true) {
            isConnected.value = isInternetAvailable(context)
            delay(5000) // Check every 5 seconds
        }
    }

    // Skip to the next sentence every 8 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000) // Wait for 8 seconds
            if (isConnected.value && sentences.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % sentences.size
                val newText = sentences[currentIndex].trim()
                if (newText.isNotEmpty() && newText != lastSpokenText) {
                    tts?.speak(newText, TextToSpeech.QUEUE_FLUSH, null, null)
                    lastSpokenText = newText
                }
            }
        }
    }

    LaunchedEffect(response) {
        val newText = sentences[currentIndex].trim()
        if (newText.isNotEmpty() && newText != lastSpokenText) {
            tts?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
            lastSpokenText = newText
        }
    }

    LaunchedEffect(currentMode, navigationResponse, chatResponse, readingModeResult) {
        when (currentMode) {
            "navigation" -> {
                val newText = navigationResponse.substring(lastSpokenIndex)
                tts?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
            }
            "assistant" -> {
                // Don't automatically speak in assistant mode
            }
            "reading" -> {
                // Don't automatically speak in reading mode
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isConnected.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0x88000000))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You are not connected to the internet",
                    color = Color.Red,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                tts?.speak("You are not connected to the internet", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0x88000000))
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    when (currentMode) {
                        "reading" -> {
                            Text(
                                text = "Reading: $readingModeResult",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier
                                    .background(Color(0xAA000000))
                                    .padding(8.dp)
                            )
                        }
                        "assistant" -> {
                            Text(
                                text = "Chat: $chatResponse",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier
                                    .background(Color(0xAA000000))
                                    .padding(8.dp)
                            )
                        }
                        "navigation" -> {
                            Text(
                                text = navigationResponse,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}