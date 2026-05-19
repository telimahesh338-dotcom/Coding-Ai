package com.codebot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codebot.ui.ChatScreen
import com.codebot.ui.EditorScreen
import com.codebot.ui.PreviewScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeBotApp()
        }
    }
}

@Composable
fun CodeBotApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") { 
            ChatScreen(onNavigateToFiles = { navController.navigate("files") }) 
        }
        composable("files") { 
            EditorScreen(
                onNavigateToPreview = { navController.navigate("preview") },
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("preview") {
            PreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
