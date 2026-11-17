package app.ash.launcher.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class AsciiClockTest {

    @Test
    fun `clock renders five rows of equal width`() {
        val lines = AsciiClock.lines("09:41")
        assertEquals(AsciiClock.ROWS, lines.size)
        lines.forEach { line -> assertEquals(30, line.length) }
    }

    @Test
    fun `blinking colon keeps the same width`() {
        val on = AsciiClock.lines("09:41")
        val off = AsciiClock.lines("09 41")
        assertEquals(on[0].length, off[0].length)
        assertEquals(on, AsciiClock.lines("09:41"))
    }
}
