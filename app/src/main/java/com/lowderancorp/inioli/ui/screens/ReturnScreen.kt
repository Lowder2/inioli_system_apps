package com.lowderancorp.inioli.ui.screens

import androidx.compose.runtime.Composable
import com.lowderancorp.inioli.ui.components.FeaturePlaceholderScreen

@Composable
fun ReturnScreen(
    onBackClick: () -> Unit
) {
    FeaturePlaceholderScreen(
        title = "Return",
        onBackClick = onBackClick
    )
}
