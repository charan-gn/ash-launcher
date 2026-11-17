package app.ash.launcher.ui.widget

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import app.ash.launcher.R
import app.ash.launcher.helper.getColorFromAttr
import kotlin.math.floor

class AsciiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    private var title = ""
    private var bodyLines: List<String> = emptyList()
    private var footer = ""
    private var dimLastLine = false
    private val baseTextSizePx = textSize

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        setLetterSpacing(0f)
        setLineSpacing(0f, 1f)
    }

    fun setPanel(
        title: String,
        lines: List<String> = emptyList(),
        footer: String? = null,
        dimLastLine: Boolean = false,
    ) {
        this.title = title
        this.bodyLines = lines
        this.footer = footer.orEmpty()
        this.dimLastLine = dimLastLine
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

        val accent = context.getColorFromAttr(R.attr.accentColor)
        val dim = context.getColorFromAttr(R.attr.dimColor)
        val content = context.getColorFromAttr(R.attr.primaryColor)

        val body = MutableList(rows - 2) { "" }
        val start = (body.size - bodyLines.size) / 2
        var dimRow = -1
        bodyLines.forEachIndexed { index, line ->
            val row = start + index
            if (row in body.indices) {
                body[row] = line
                if (dimLastLine && index == bodyLines.lastIndex) dimRow = row
            }
        }

        val builder = SpannableStringBuilder()
        builder.appendColored("┌─ ", dim)
        builder.appendColored(title, accent)
        builder.appendColored(" " + "─".repeat(inner - title.length - 3) + "┐", dim)
        body.forEachIndexed { index, line ->
            builder.append('\n')
            builder.appendColored("│", dim)
            builder.appendColored(line.padCenter(inner), if (index == dimRow) dim else content)
            builder.appendColored("│", dim)
        }
        builder.append('\n')
        if (footer.isNotBlank() && footer.length <= inner - 3) {
            builder.appendColored("└─ ", dim)
            builder.appendColored(footer, accent)
            builder.appendColored(" " + "─".repeat(inner - footer.length - 3) + "┘", dim)
        } else {
            builder.appendColored("└" + "─".repeat(inner) + "┘", dim)
        }
        text = builder
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

    private fun SpannableStringBuilder.appendColored(value: String, color: Int) {
        if (value.isEmpty()) return
        val start = length
        append(value)
        setSpan(ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
