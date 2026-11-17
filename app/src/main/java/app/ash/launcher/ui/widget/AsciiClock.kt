package app.ash.launcher.ui.widget

object AsciiClock {

    private fun cells(vararg rows: String): List<String> = rows.map { row ->
        buildString {
            row.forEach { cell -> append(if (cell == '1') "██" else "  ") }
        }
    }

    private val DIGITS = mapOf(
        '0' to cells("111", "101", "101", "101", "111"),
        '1' to cells("001", "001", "001", "001", "001"),
        '2' to cells("111", "001", "111", "100", "111"),
        '3' to cells("111", "001", "111", "001", "111"),
        '4' to cells("101", "101", "111", "001", "001"),
        '5' to cells("111", "100", "111", "001", "111"),
        '6' to cells("111", "100", "111", "101", "111"),
        '7' to cells("111", "001", "001", "001", "001"),
        '8' to cells("111", "101", "111", "101", "111"),
        '9' to cells("111", "101", "111", "001", "111"),
        ':' to cells("0", "1", "0", "1", "0"),
        ' ' to cells("0", "0", "0", "0", "0"),
    )

    fun lines(time: String): List<String> {
        val glyphs = time.map { DIGITS[it] ?: DIGITS.getValue(' ') }
        return (0 until ROWS).map { row -> glyphs.joinToString(" ") { it[row] } }
    }

    const val ROWS = 5
}
