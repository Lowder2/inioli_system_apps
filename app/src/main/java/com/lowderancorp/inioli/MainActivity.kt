package com.lowderancorp.inioli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lowderancorp.inioli.ui.InioliApp
import com.lowderancorp.inioli.ui.theme.InioliStockManagementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InioliStockManagementTheme {
                InioliApp()
            }
        }
    }
}
