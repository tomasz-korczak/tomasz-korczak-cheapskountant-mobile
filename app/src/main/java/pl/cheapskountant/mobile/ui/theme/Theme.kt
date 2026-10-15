package pl.cheapskountant.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Papier = lightColorScheme(
    primary = Color(0xFF115E59),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7EDEB),
    onPrimaryContainer = Color(0xFF1C1917),
    background = Color(0xFFF3EEE4),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1C1917),
    onSurfaceVariant = Color(0xFF57534E),
)

@Composable
fun CheapskountantTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Papier, content = content)
}
