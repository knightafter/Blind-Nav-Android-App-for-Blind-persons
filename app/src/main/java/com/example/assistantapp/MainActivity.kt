package com.example.assistantapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.assistantapp.ui.theme.AssistantAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssistantAppTheme {
                    Navigation(navController = rememberNavController())
                }
            }
        }
    }

