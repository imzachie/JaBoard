package com.jmrc.jaboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jmrc.jaboard.data.JaBoardViewModel
import com.jmrc.jaboard.ui.JaBoardApp
import com.jmrc.jaboard.ui.JaBoardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: JaBoardViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            JaBoardTheme(theme = settings.theme, dynamicColor = settings.dynamicColor) {
                JaBoardApp(vm)
            }
        }
    }
}
