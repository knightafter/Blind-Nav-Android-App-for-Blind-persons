package com.example.assistantapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun MainPage(navController: NavHostController) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        ElevatedCard(
            onClick = { navController.navigate("blindMode") },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2AB9B3),
            )

        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "Blind Mode", color = Color.White, fontSize = 30.sp)            }
        }
    }
}
