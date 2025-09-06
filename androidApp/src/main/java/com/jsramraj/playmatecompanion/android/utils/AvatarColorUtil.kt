package com.jsramraj.playmatecompanion.android.utils

import androidx.compose.ui.graphics.Color

object AvatarColorUtil {
    fun getColorForLetter(letter: Char): Color {
        val index = (letter.uppercaseChar() - 'A')
        if (index !in 0..25) return Color.Gray // fallback for non-letters
        
        // Map A–Z → evenly spaced hues on the color wheel
        val hue = (index * (360f / 26)) % 360f
        val saturation = 0.5f   // Medium saturation for pleasant colors
        val lightness = 0.6f    // Slightly bright for good contrast with white text

        return hslToColor(hue, saturation, lightness)
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = l - c / 2

        val (r, g, b) = when {
            h < 60  -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else    -> Triple(c, 0f, x)
        }

        return Color(
            red = (r + m),
            green = (g + m),
            blue = (b + m),
            alpha = 1f
        )
    }
}
