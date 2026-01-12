package com.chummusbenshira.soundmachine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chummusbenshira.soundmachine.ui.SoundMachineScreen
import com.chummusbenshira.soundmachine.ui.theme.SoundMachineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundMachineTheme {
                SoundMachineScreen()
            }
        }
    }
}
