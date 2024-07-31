package com.example.assistantapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontStyle
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

@Composable
fun BlindModeScreen() {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    var showPopup by remember { mutableStateOf(true) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var sessionStarted by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf("") }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenIndex by remember { mutableStateOf(0) }
    var lastProcessedTimestamp by remember { mutableStateOf(0L) }
    val frameInterval = 1300 // Process a frame every 1.3 seconds
    var navigationPaused by remember { mutableStateOf(false) }
    var isMicActive by remember { mutableStateOf(false) }
    var isBookActive by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var chatResponse by remember { mutableStateOf("") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.5f)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    coroutineScope.launch {
                        chatResponse = sendMessageToGeminiAI(spokenText, analysisResult)
                        tts.value?.speak(chatResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Automatically restart listening when speech ends
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onError(error: Int) {
                // Restart listening on error, if navigation is paused
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Effect to handle microphone activation when navigation is paused
    LaunchedEffect(navigationPaused) {
        if (navigationPaused) {
            isMicActive = true
            speechRecognizer.startListening(speechIntent)
        } else {
            isMicActive = false
            speechRecognizer.stopListening()
            // Clear chatResponse to display the analysis result when resuming navigation
            chatResponse = ""
        }
    }

    // Effect to handle long press for book icon activation
    LaunchedEffect(isBookActive) {
        if (isBookActive) {
            navigationPaused = true
            tts.value?.speak("Reading mode activated.", TextToSpeech.QUEUE_FLUSH, null, null)
            // Initialize Gemini 2 model for reading purposes here
        } else {
            navigationPaused = false
            tts.value?.speak("Reading mode deactivated.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    if (hasPermission) {
        if (sessionStarted && !navigationPaused) {
            CameraPreviewWithAnalysis { imageProxy ->
                val currentTimestamp = System.currentTimeMillis()
                if (currentTimestamp - lastProcessedTimestamp >= frameInterval) {
                    coroutineScope.launch {
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            if (isBookActive) {
                                // Use Gemini 2 AI for reading
                                sendFrameToGemini2AI(bitmap, { partialResult ->
                                    chatResponse += " $partialResult"
                                    tts.value?.speak(partialResult, TextToSpeech.QUEUE_ADD, null, null)
                                }, { error ->
                                    // Handle error here
                                })
                            } else {
                                // Use Gemini 1 AI for navigation
                                sendFrameToGeminiAI(bitmap, { partialResult ->
                                    analysisResult += " $partialResult"
                                    val newText = analysisResult.substring(lastSpokenIndex)
                                    tts.value?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
                                    lastSpokenIndex = analysisResult.length
                                }, { error ->
                                    // Handle error here
                                })
                            }
                            lastProcessedTimestamp = currentTimestamp
                        }
                        imageProxy.close()
                    }
                } else {
                    imageProxy.close()
                }
            }
        }

    } else {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        navigationPaused = !navigationPaused
                        if (navigationPaused) {
                            tts.value?.speak("Navigation paused.", TextToSpeech.QUEUE_FLUSH, null, null)
                        } else {
                            tts.value?.speak("Navigation resumed", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    onLongPress = {
                        isBookActive = !isBookActive
                    }
                )
            }
    ) {
        if (showPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFFFF),
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.99f)
                        .height(150.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .width(300.dp)
                            .wrapContentHeight()
                    ) {
                        Text(
                            "Start of Session",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = {
                                showPopup = false
                                sessionStarted = true
                                tts.value?.speak("Start of Session", TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier
                                .width(240.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                "New Session",
                                color = Color(0xFFFFFFFF),
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (sessionStarted) {
                    AIResponseOverlay(response = analysisResult, chatResponse = chatResponse, tts = tts.value, lastSpokenIndex = lastSpokenIndex)
                }
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = "Book Icon",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(64.dp)
                        .clickable { /* Handle book icon click if needed */ },
                    tint = if (isBookActive) Color.Green else Color(0xFFB0B1B1)
                )
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Mic Icon",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(64.dp),
                    tint = if (isMicActive) Color.Green else Color(0xFFB0B1B1)
                )
            }
        }
    }
}







@Composable
fun CameraPreviewWithAnalysis(onImageCaptured: (ImageProxy) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val preview = Preview.Builder().build()
    val previewView = PreviewView(context)

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(1280, 720))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(Executors.newSingleThreadExecutor(), ImageAnalysis.Analyzer { imageProxy ->
                onImageCaptured(imageProxy)
            })
        }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        preview.setSurfaceProvider(previewView.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }
}

@Composable
fun AIResponseOverlay(response: String, chatResponse: String, tts: TextToSpeech?, lastSpokenIndex: Int) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(isInternetAvailable(context)) }
    var currentIndex by remember { mutableStateOf(lastSpokenIndex) } // Track the current sentence index
    val sentences = response.split(".") // Split the response into sentences
    var lastSpokenText by remember { mutableStateOf("") } // Track the last spoken text

    // Continuously check internet connectivity
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = isInternetAvailable(context)
            delay(5000) // Check every 5 seconds
        }
    }

    // Skip to the next sentence every 20 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(12000) // Wait for 12 seconds
            if (isConnected && sentences.isNotEmpty()) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0x88000000))
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (!isConnected) {
                Text(
                    text = "You are not connected to the internet",
                    color = Color.Red,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )
                LaunchedEffect(isConnected) {
                    tts?.speak("You are not connected to the internet", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (chatResponse.isNotEmpty()) {
                        Text(
                            text = "Chat: $chatResponse",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .background(Color(0xAA000000))
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            text = response,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}