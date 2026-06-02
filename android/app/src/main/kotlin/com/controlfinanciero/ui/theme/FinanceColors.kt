package com.controlfinanciero.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores semánticos de finanzas, fijos en ambos temas (no salen del ColorScheme de Material
 * porque "ingreso/egreso" no mapea a primary/error). Usar en vez de hardcodear verdes/rojos.
 */
object FinanceColors {
    val Income = Color(0xFF22C55E)   // ingreso / positivo
    val Expense = Color(0xFFF43F5E)  // egreso / negativo
    val Warning = Color(0xFFF59E0B)  // alerta (ej: presupuesto cerca del límite)

    val Violet = Color(0xFF9B30F5)
    val Blue = Color(0xFF3B82F6)
    val Magenta = Color(0xFFC724E0)
}
