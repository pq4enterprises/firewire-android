package com.pioneer.nycfirewire.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the chip text contrast rule (no Android framework). */
class UnitColorsTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test fun blackOnLightChips() {
        assertEquals(black, UnitColors.contrastingText(0xFFF7C948.toInt())) // chief yellow
        assertEquals(black, UnitColors.contrastingText(0xFF17B26A.toInt())) // marine green
        assertEquals(black, UnitColors.contrastingText(0xFFFCF0E4.toInt())) // SOC light tint
        assertEquals(black, UnitColors.contrastingText(white))
        // brand red: black 4.85:1 beats white 4.33:1 — same answer as the server's autoTextColor
        assertEquals(black, UnitColors.contrastingText(0xFFE5342A.toInt()))
    }

    @Test fun whiteOnDarkChips() {
        assertEquals(white, UnitColors.contrastingText(0xFF1C1C1E.toInt())) // engine near-black
        assertEquals(white, UnitColors.contrastingText(black))
        assertEquals(white, UnitColors.contrastingText(0xFF2F6FE4.toInt())) // blue
        assertEquals(white, UnitColors.contrastingText(0xFF7A4DD8.toInt())) // purple
    }

    @Test fun translucentBackgroundIsCompositedOverBackdrop() {
        val socDark = 0x33E0842F // 20% orange
        assertEquals(white, UnitColors.contrastingText(socDark, 0xFF181A21.toInt())) // over dark surface → white
        assertEquals(black, UnitColors.contrastingText(socDark, white))               // over white → black
    }

    @Test fun luminanceAndContrast() {
        assertEquals(1.0, UnitColors.luminance(white), 1e-4)
        assertEquals(0.0, UnitColors.luminance(black), 1e-4)
        assertEquals(21.0, UnitColors.contrastRatio(white, black), 1e-4)
        assertTrue(UnitColors.contrastRatio(0xFFE5342A.toInt(), black) > 4.5)
    }
}
