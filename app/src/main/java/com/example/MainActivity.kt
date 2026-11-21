package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.SobdosuddhiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge layout
        enableEdgeToEdge()

        // STEP 3: Render Sobdosuddhi.1.0 Compose UI
        setContent {
            SobdosuddhiApp()
        }
    }
}
