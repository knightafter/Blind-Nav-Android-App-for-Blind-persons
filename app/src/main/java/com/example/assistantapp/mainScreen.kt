package com.example.assistantapp

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainPage(navController: NavHostController) {
    val context = LocalContext.current
    var isSettingsVisible by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Background blur when settings are visible
        if (isSettingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(10.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }

        // Blind Mode Card
        val cardOffset by animateDpAsState(
            targetValue = if (isSettingsVisible) (-120).dp else 0.dp,
            animationSpec = tween(300)
        )

        ElevatedCard(
            onClick = { navController.navigate("blindMode") },
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .offset(y = cardOffset)
                .fillMaxWidth(0.8f)
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B1B1))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Blind Mode",
                    color = Color.White,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Instructions",
                color = Color.Gray,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable {
                        isSettingsVisible = !isSettingsVisible
                        if (isSettingsVisible) {
                            tts?.speak(
                                "To Master the navigation through this App, follow these instructions below. " +
                                        "Double Tap on the Navigation screen to stop the Navigation or vice versa. " +
                                        "Right after, speak any query about anything or the environment around you. " +
                                        "You also can use your earbuds or airpods by double tapping to stop or resume the navigation. " +
                                        "You can also visit the YouTube video to see detailed navigation on the App. " +
                                        "By following these, you can teach an impaired person how to use this app.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        } else {
                            tts?.stop()
                        }
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = isSettingsVisible,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) { isVisible ->
                Icon(
                    imageVector = if (isVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isVisible) "Close Settings" else "Open Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isSettingsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                ) {
                    val annotatedText = buildAnnotatedString {
                        append("To Master the navigation through this App, follow these instructions below 😎.\n\n")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Double Tap ")
                        }
                        append("on the Navigation screen to ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("stop ")
                        }
                        append("the Navigation or ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("vice versa.\n")
                        }
                        append("Right after, speak any query about ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("anything or the environment around you.\n")
                        }
                        append("You also can use your ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("earbuds / airpods ")
                        }
                        append("by ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("double-tapping ")
                        }
                        append("to stop or resume the navigation.\n\n")
                        append("You can also visit the ")
                        pushStringAnnotation(
                            tag = "URL",
                            annotation = "https://www.youtube.com/watch?v=_9prH7NFmLI"
                        )
                        withStyle(
                            style = SpanStyle(
                                color = Color.Blue,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("YouTube video ")
                        }
                        pop()
                        append("to see detailed navigation on the App. By following these, you can ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("teach an impaired person ")
                        }
                        append("how to use this app.")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            ClickableText(
                                text = annotatedText,
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations(
                                        tag = "URL",
                                        start = offset,
                                        end = offset
                                    )
                                        .firstOrNull()?.let { annotation ->
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            context.startActivity(intent)
                                        }
                                },
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }

        }
    }
}
