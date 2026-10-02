package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TournamentDarkColorScheme = darkColorScheme(
  primary = Gold400,
  onPrimary = Slate950,
  primaryContainer = Gold500,
  onPrimaryContainer = Slate950,
  secondary = Cyan400,
  onSecondary = Slate950,
  secondaryContainer = MidnightNavyCard,
  onSecondaryContainer = Cyan400,
  tertiary = NeonEmerald,
  onTertiary = Slate950,
  background = MidnightBgTop,
  onBackground = Slate50,
  surface = MidnightNavyCard,
  onSurface = Slate50,
  surfaceVariant = Slate900,
  onSurfaceVariant = Slate400,
  outline = MidnightNavyBorder,
  outlineVariant = Slate700,
  error = Rose500,
  onError = Color.White,
)

private val TournamentLightColorScheme = lightColorScheme(
  primary = Amber600,
  onPrimary = Color.White,
  primaryContainer = Gold400,
  onPrimaryContainer = Slate950,
  secondary = Cyan500,
  onSecondary = Color.White,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate600,
  outline = Slate200,
  error = Rose600,
  onError = Color.White,
)

@Composable
fun AdTournamentTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) TournamentDarkColorScheme else TournamentLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = TournamentTypography,
    content = content,
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  AdTournamentTheme(darkTheme = darkTheme, content = content)
}
