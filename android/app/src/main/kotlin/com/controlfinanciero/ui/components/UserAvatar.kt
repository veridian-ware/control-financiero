package com.controlfinanciero.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Paleta estable para los avatares de iniciales.
private val avatarColors = listOf(
    Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), Color(0xFF7B1FA2),
    Color(0xFFF57C00), Color(0xFF00796B), Color(0xFFC2185B), Color(0xFF512DA8)
)

/** Hasta 2 iniciales a partir del nombre; si no hay nombre, usa el email. */
fun initialsFor(name: String?, email: String): String {
    val base = name?.trim()?.takeIf { it.isNotBlank() }
    if (base != null) {
        val parts = base.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase()
        else parts[0].take(2).uppercase()
    }
    return email.trim().take(2).uppercase().ifBlank { "?" }
}

/** Avatar circular con las iniciales del usuario sobre un color derivado de su identidad. */
@Composable
fun InitialsAvatar(
    name: String?,
    email: String,
    size: Dp = 48.dp,
    fontSize: TextUnit = 18.sp
) {
    val initials = initialsFor(name, email)
    val key = name?.takeIf { it.isNotBlank() } ?: email
    val color = avatarColors[abs(key.hashCode()) % avatarColors.size]
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = fontSize)
    }
}
