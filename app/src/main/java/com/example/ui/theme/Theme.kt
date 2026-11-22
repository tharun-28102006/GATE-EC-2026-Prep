package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GateSemanticColors(
    val examHeader: Color,
    val examHeaderContent: Color,
    val border: Color,
    val answered: Color,
    val answeredContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val review: Color,
    val reviewContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val unvisited: Color,
    val formulaBg: Color
)

val LocalGateColors = staticCompositionLocalOf {
    GateSemanticColors(
        examHeader = ExamNavy,
        examHeaderContent = Color.White,
        border = ExamBorderSlate,
        answered = GateAnsweredGreen,
        answeredContainer = GateAnsweredGreenSoft,
        wrong = GateWrongCrimson,
        wrongContainer = GateWrongCrimsonSoft,
        review = GateReviewPurple,
        reviewContainer = GateReviewPurpleSoft,
        streak = GateStreakAmber,
        streakContainer = GateStreakAmberSoft,
        unvisited = GateUnvisitedSlate,
        formulaBg = BlueprintBlueSoft
    )
}

private val GateLightColorScheme = lightColorScheme(
    primary = BlueprintBlue,
    onPrimary = Color.White,
    primaryContainer = BlueprintBlueSoft,
    onPrimaryContainer = ExamNavyDark,
    secondary = ExamNavy,
    onSecondary = Color.White,
    secondaryContainer = ExamNavyContainer,
    onSecondaryContainer = ExamNavyDark,
    tertiary = GateReviewPurple,
    onTertiary = Color.White,
    tertiaryContainer = GateReviewPurpleSoft,
    onTertiaryContainer = GateReviewPurple,
    background = ExamPaperBackground,
    onBackground = InkPrimary,
    surface = ExamPaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = ExamSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = ExamBorderSlate,
    error = GateWrongCrimson,
    onError = Color.White,
    errorContainer = GateWrongCrimsonSoft,
    onErrorContainer = GateWrongCrimson
)

private val GateDarkColorScheme = darkColorScheme(
    primary = NightBlueAccent,
    onPrimary = NightBackground,
    primaryContainer = NightHeaderNavy,
    onPrimaryContainer = NightInkPrimary,
    secondary = NightBlueAccent,
    onSecondary = NightBackground,
    secondaryContainer = NightSurfaceElevated,
    onSecondaryContainer = NightInkPrimary,
    tertiary = NightPurpleAccent,
    onTertiary = NightBackground,
    tertiaryContainer = NightPurpleSoft,
    onTertiaryContainer = NightPurpleAccent,
    background = NightBackground,
    onBackground = NightInkPrimary,
    surface = NightSurface,
    onSurface = NightInkPrimary,
    surfaceVariant = NightSurfaceElevated,
    onSurfaceVariant = NightInkSecondary,
    outline = NightBorder,
    error = NightCrimsonAccent,
    onError = NightBackground,
    errorContainer = NightCrimsonSoft,
    onErrorContainer = NightCrimsonAccent
)

@Composable
fun GateExamTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GateDarkColorScheme else GateLightColorScheme
    val semanticColors = if (darkTheme) {
        GateSemanticColors(
            examHeader = NightHeaderNavy,
            examHeaderContent = NightInkPrimary,
            border = NightBorder,
            answered = NightGreenAccent,
            answeredContainer = NightGreenSoft,
            wrong = NightCrimsonAccent,
            wrongContainer = NightCrimsonSoft,
            review = NightPurpleAccent,
            reviewContainer = NightPurpleSoft,
            streak = NightAmberAccent,
            streakContainer = NightAmberSoft,
            unvisited = NightSurfaceElevated,
            formulaBg = NightSurfaceElevated
        )
    } else {
        GateSemanticColors(
            examHeader = ExamNavy,
            examHeaderContent = Color.White,
            border = ExamBorderSlate,
            answered = GateAnsweredGreen,
            answeredContainer = GateAnsweredGreenSoft,
            wrong = GateWrongCrimson,
            wrongContainer = GateWrongCrimsonSoft,
            review = GateReviewPurple,
            reviewContainer = GateReviewPurpleSoft,
            streak = GateStreakAmber,
            streakContainer = GateStreakAmberSoft,
            unvisited = GateUnvisitedSlate,
            formulaBg = BlueprintBlueSoft
        )
    }

    CompositionLocalProvider(LocalGateColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GateExamTypography,
            content = content
        )
    }
}
