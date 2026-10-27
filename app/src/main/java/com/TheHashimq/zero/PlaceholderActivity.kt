/*
 * MODULE: app (temporary launcher screen)
 * PURPOSE: Keeps the app launchable while the foundation is built.
 * TAKES: The launch intent.
 * GIVES: A static screen showing the app name.
 * OWNS: No state.
 * DOES NOT OWN: Protection, features, or health display. Replaced by the navigation shell.
 * DEPENDENCIES: Compose, Material3.
 * CONTRACT: Shows no feature state and does not block or enforce anything.
 * FAILURE BEHAVIOR: None expected; the content is static.
 */
package com.TheHashimq.zero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class PlaceholderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Zero — foundation build")
                    }
                }
            }
        }
    }
}
