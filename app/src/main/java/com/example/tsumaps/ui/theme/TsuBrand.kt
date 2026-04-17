package com.example.tsumaps.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TsuBrand {
    val Blue = Color(0xFF005A9E)
    val BlueDark = Color(0xFF003D6B)
    val Header = BlueDark
    val PanelSurface = Color(0xFF0B2744).copy(alpha = 0.92f)
    val AccentBlue = Color(0xFF0077CC)
}

@Composable
fun tsuHeaderButtonColors() = ButtonDefaults.buttonColors(
    containerColor = TsuBrand.Header,
    contentColor = Color.White,
    disabledContainerColor = TsuBrand.Header.copy(alpha = 0.38f),
    disabledContentColor = Color.White.copy(alpha = 0.38f)
)
