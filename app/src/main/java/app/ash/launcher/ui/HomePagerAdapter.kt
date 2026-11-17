package app.ash.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.ash.launcher.R
import app.ash.launcher.databinding.ItemHomePageBinding
import app.ash.launcher.listener.OnSwipeTouchListener

class HomePagerAdapter(
    private val context: Context,
    private val pageNames: List<String>,
    private val callbacks: GestureCallbacks,
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    interface GestureCallbacks {
        fun onSwipeUp()
        fun onSwipeDown()
        fun onLongPress()
        fun onTap(position: Int, xFraction: Float)
    }

    class PageViewHolder(val binding: ItemHomePageBinding) : RecyclerView.ViewHolder(binding.root)

    private val pageContent = mutableMapOf<Int, List<String>>()
    private val placeholder by lazy {
        listOf("", context.getString(R.string.page_empty), "")
    }

    fun setPageContent(position: Int, lines: List<String>) {
        if (position !in 0 until itemCount) return
        if (pageContent[position] == lines) return
        pageContent[position] = lines
        notifyItemChanged(position)
    }

    fun setHomeContent(lines: List<String>) = setPageContent(0, lines)

    override fun getItemCount() = pageNames.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemHomePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = PageViewHolder(binding)
        binding.root.setOnTouchListener(object : OnSwipeTouchListener(context) {
            override fun onSwipeUp() {
                callbacks.onSwipeUp()
            }

            override fun onSwipeDown() {
                callbacks.onSwipeDown()
            }

            override fun onLongClick() {
                callbacks.onLongPress()
            }

            override fun onTap(xFraction: Float) {
                val position = holder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) callbacks.onTap(position, xFraction)
            }
        })
        return holder
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val lines = pageContent[position] ?: placeholder
        val footer = "${position + 1}/${pageNames.size}"
        holder.binding.panel.setPanel(pageNames[position], lines, footer, position == 0)
    }
}
