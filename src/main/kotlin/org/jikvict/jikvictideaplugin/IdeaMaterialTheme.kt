@file:Suppress("UnstableApiUsage")

package org.jikvict.jikvictideaplugin

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.intellij.ide.ui.LafManager
import com.intellij.ui.JBColor
import javax.swing.UIManager

private fun uiColor(key: String, fallback: Color): Color {
    val awt = UIManager.getColor(key)
    return if (awt != null) Color(awt.rgb)
    else fallback
}

@Suppress("SameParameterValue")
private fun jbColor(light: Int, dark: Int): Color = Color(JBColor(light, dark).rgb)

private fun buildIdeaColorScheme(dark: Boolean): ColorScheme {
    // Core surfaces try to reflect IDE panel backgrounds and text
    val background = uiColor("Panel.background", if (dark) Color(0xFF2B2B2B) else Color(0xFFFFFFFF))
    val surface = background
    val onSurface = uiColor("Label.foreground", if (dark) Color(0xFFE6E6E6) else Color(0xFF1F1F1F))
    val outline = uiColor("Separator.foreground", if (dark) Color(0xFF3C3F41) else Color(0xFFDDDDDD))

    // Primary/secondary use IDE accent if available; fall back to JBColor link colors
    val accent = run {
        // Many LaFs expose accent via named keys; try a few
        val keys = listOf("Link.activeForeground", "Component.accentColor", "Actions.Blue")
        val c = keys.firstNotNullOfOrNull { UIManager.getColor(it) }
        c?.let { Color(it.rgb) } ?: jbColor(0x0A84FF, 0x409CFF)
    }

    val secondaryContainer = uiColor("EditorPane.background", if (dark) Color(0xFF3C3F41) else Color(0xFFF5F5F5))
    val onSecondaryContainer = onSurface

    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = Color.White,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            outline = outline,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent.copy(alpha = 0.9f),
            onSecondary = Color.White,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            outline = outline,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
        )
    }
}

@Composable
fun IdeaMaterialTheme(content: @Composable () -> Unit) {
    // Detect dark mode from IDE
    val laf = LafManager.getInstance().currentUIThemeLookAndFeel
    val isDark = laf?.name?.contains("Dark", ignoreCase = true) == true || laf?.name?.contains(
        "Darcula",
        ignoreCase = true
    ) == true
    val colorScheme = remember(isDark) { buildIdeaColorScheme(isDark) }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
