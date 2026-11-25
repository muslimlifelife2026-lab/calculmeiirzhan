package com.aerocalc.smartcalculator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.calculator.core.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassContrastTest {

    private fun composite(bg: Color, fg: Color): Color {
        val a = fg.alpha
        return Color(
            red = bg.red * (1f - a) + fg.red * a,
            green = bg.green * (1f - a) + fg.green * a,
            blue = bg.blue * (1f - a) + fg.blue * a,
            alpha = 1f
        )
    }

    private fun wcagContrast(fg: Color, bg: Color): Double {
        val l1 = fg.luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertWcagAA(label: String, fg: Color, bg: Color) {
        val ratio = wcagContrast(fg, bg)
        assertTrue("$label contrast was %.2f (< 4.50)".format(ratio), ratio >= 4.5)
    }

    @Test
    fun textAndErrorsClearWcagAAOverGlassSceneAndCardPeaks() {
        var scenePeak = GlassSceneTop
        scenePeak = composite(scenePeak, GlassSceneSilverPeak)
        scenePeak = composite(scenePeak, GlassSceneWarmLight)
        scenePeak = composite(scenePeak, GlassSceneRibbonPeak)
        scenePeak = composite(scenePeak, GlassSceneFoldPeak)

        val cardPeak = composite(composite(scenePeak, GlassTint), Color.White.copy(alpha = GlassCardTopSheenAlpha))
        val surfacePeak = composite(composite(scenePeak, GlassTint), Color.White.copy(alpha = GlassSurfaceTopSheenAlpha))
        val navActivePeak = composite(surfacePeak, Color.White.copy(alpha = GlassNavIndicatorAlpha))
        val keyClearPeak = composite(composite(scenePeak, KeyClear), Color.White.copy(alpha = 0.08f))
        val keyNumericPeak = composite(composite(scenePeak, KeyNumeric), Color.White.copy(alpha = 0.08f))
        val keyOperatorPeak = composite(composite(scenePeak, KeyOperator), Color.White.copy(alpha = 0.11f))
        val keySpecialPeak = composite(composite(scenePeak, KeySpecial), Color.White.copy(alpha = 0.08f))

        listOf(
            "TextPrimary on GlassScene peak" to (TextPrimary to scenePeak),
            "TextSecondary on GlassScene peak" to (TextSecondary to scenePeak),
            "TextMuted on GlassScene peak" to (TextMuted to scenePeak),
            "ErrorRed on GlassScene peak" to (ErrorRed to scenePeak),
            "AccentSecondary on GlassScene peak" to (AccentSecondary to scenePeak),
            "TextPrimary on GlassCard peak" to (TextPrimary to cardPeak),
            "TextSecondary on GlassCard peak" to (TextSecondary to cardPeak),
            "TextMuted on GlassCard peak" to (TextMuted to cardPeak),
            "ErrorRed on GlassCard peak" to (ErrorRed to cardPeak),
            "SuccessGreen on GlassCard peak" to (SuccessGreen to cardPeak),
            "TextPrimary on GlassNav active" to (TextPrimary to navActivePeak),
            "ErrorRed on KeyClear peak" to (ErrorRed to keyClearPeak),
            "TextPrimary on KeyNumeric peak" to (TextPrimary to keyNumericPeak),
            "TextPrimary on KeyOperator peak" to (TextPrimary to keyOperatorPeak),
            "TextSecondary on KeySpecial peak" to (TextSecondary to keySpecialPeak),
            "Background on ErrorRed (onError)" to (Background to ErrorRed)
        ).forEach { (label, pair) ->
            assertWcagAA(label, pair.first, pair.second)
        }
    }
}
