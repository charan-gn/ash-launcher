package app.ash.launcher.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.floor

class AsciiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    private var title = ""
    private var bodyLines: List<String> = emptyList()

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        setLetterSpacing(0f)
        setLineSpacing(0f, 1f)
    }

    fun setPanel(title: String, lines: List<String> = emptyList()) {
        this.title = title
        this.bodyLines = lines
        rebuild()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    private fun rebuild() {
        val charWidth = paint.measureText("M")
        val metrics = paint.fontMetrics
        val lineHeight = metrics.descent - metrics.ascent + lineSpacingExtra
        if (width == 0 || height == 0 || charWidth <= 0f || lineHeight <= 0f) return

        val usableWidth = width - compoundPaddingLeft - compoundPaddingRight
        val usableHeight = height - compoundPaddingTop - compoundPaddingBottom
        val columns = floor(usableWidth / charWidth).toInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val rows = floor(usableHeight / lineHeight).toInt().coerceAtLeast(MIN_ROWS)
        val inner = columns - 2
        if (inner <= title.length + 2) return

        val topBar = "┌─ $title " + "─".repeat(inner - title.length - 3) + "┐"
        val bottomBar = "└" + "─".repeat(inner) + "┘"

        val body = MutableList(rows - 2) { "" }
        val start = (body.size - bodyLines.size) / 2
        bodyLines.forEachIndexed { index, line ->
            val row = start + index
            if (row in body.indices) body[row] = line
        }

        text = buildString {
            append(topBar)
            body.forEach { line ->
                append('\n')
                append('│')
                append(line.padCenter(inner))
                append('│')
            }
            append('\n')
            append(bottomBar)
        }
    }

    private fun String.padCenter(width: Int): String {
        if (length >= width) return take(width)
        val total = width - length
        val left = total / 2
        return " ".repeat(left) + this + " ".repeat(total - left)
    }

    private companion object {
        const val MIN_COLUMNS = 12
        const val MAX_COLUMNS = 80
        const val MIN_ROWS = 3
    }
}
