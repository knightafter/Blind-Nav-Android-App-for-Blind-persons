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
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.font.FontStyle
import sendFrameToGeminiAI
import java.util.Locale
import kotlin.math.abs

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
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var sessionStarted by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf("") }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenIndex by remember { mutableStateOf(0) }
    var lastProcessedTimestamp by remember { mutableStateOf(0L) }
    val frameInterval = 6000 // Process a frame every 2 seconds

    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
        }
    }

    if (hasPermission) {
        if (sessionStarted) {
            CameraPreviewWithAnalysis { imageProxy ->
                val currentTimestamp = System.currentTimeMillis()
                if (currentTimestamp - lastProcessedTimestamp >= frameInterval) {
                    coroutineScope.launch {
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            sendFrameToGeminiAI(bitmap, { partialResult ->
                                analysisResult += " $partialResult"
                                val newText = analysisResult.substring(lastSpokenIndex)
                                tts.value?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
                                lastSpokenIndex = analysisResult.length
                            }, { error ->
                                // Handle error here
                            })
                            lastProcessedTimestamp = currentTimestamp
                        }
                        imageProxy.close()  // Ensure the imageProxy is closed here
                    }
                } else {
                    imageProxy.close()  // Ensure the imageProxy is closed here
                }
            }

        }
    } else {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CAMERA),
            1
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
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
                        containerColor = Color(0xFF2AB9B3),
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.99f)
                        .height(130.dp)
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
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = {
                                showPopup = false
                                sessionStarted = true
                                tts.value?.speak("Start of Session", TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            modifier = Modifier
                                .width(240.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                "New Session",
                                color = Color(0xFF2AB9B3),
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
                    AIResponseOverlay(response = analysisResult, tts = tts.value, lastSpokenIndex = lastSpokenIndex)
                }
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
fun AIResponseOverlay(response: String, tts: TextToSpeech?, lastSpokenIndex: Int) {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(isInternetAvailable(context)) }

    LaunchedEffect(response) {
        val newText = response.substring(lastSpokenIndex)
        tts?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
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
                    modifier = Modifier
                        .background(Color(0xAA000000))
                        .padding(8.dp)
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
                    Text(
                        text = response,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .background(Color(0xAA000000))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}