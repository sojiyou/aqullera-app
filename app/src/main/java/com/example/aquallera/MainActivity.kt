package com.example.aquallera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.aquallera.components.NotificationListener
import com.example.aquallera.core.theme.AqualleraTheme
import com.example.aquallera.navigation.AqualleraNavGraph
import dagger.hilt.android.AndroidEntryPoint

/** Single-activity host that mounts the theme, order listener and navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AqualleraTheme {
                val navController = rememberNavController()
                NotificationListener()
                AqualleraNavGraph(navController = navController)
            }
        }
    }
}
