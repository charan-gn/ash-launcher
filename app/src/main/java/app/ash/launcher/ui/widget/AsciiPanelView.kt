package app.ash.launcher.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.floor

class AsciiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    private var title = ""
    private var bodyLines: List<String> = emptyList()
    private val baseTextSizePx = textSize

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
        applyFitScale()
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

    private fun applyFitScale() {
        if (width == 0 || baseTextSizePx <= 0f) return
        if (paint.textSize != baseTextSizePx) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSizePx)
        }
        val usableWidth = width - compoundPaddingLeft - compoundPaddingRight
        val charWidth = paint.measureText("M")
        if (usableWidth <= 0 || charWidth <= 0f) return

        val widestLine = bodyLines.maxOfOrNull { it.length } ?: 0
        val neededColumns = (maxOf(widestLine, title.length + 5) + 2).toFloat()
        val availableColumns = usableWidth / charWidth
        if (availableColumns >= neededColumns) return

        val scaled = baseTextSizePx * (availableColumns / neededColumns) * 0.97f
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            scaled.coerceAtLeast(baseTextSizePx * 0.6f),
        )
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
