package app.ash.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.ash.launcher.databinding.ItemHomePageBinding
import app.ash.launcher.listener.OnSwipeTouchListener

class HomePagerAdapter(
    private val context: Context,
    private val callbacks: GestureCallbacks,
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    interface GestureCallbacks {
        fun onSwipeUp()
        fun onSwipeDown()
        fun onLongPress()
    }

    class PageViewHolder(val binding: ItemHomePageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = PAGE_COUNT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemHomePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        })
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.binding.pageLabel.text = pageArt(position)
    }

    private fun pageArt(position: Int): String {
        val title = "PAGE ${position + 1}"
        val inner = title.length + 2
        val border = "─".repeat(inner)
        val content = " " + title.padEnd(inner - 1)
        return "┌$border┐\n│$content│\n└$border┘"
    }

    companion object {
        const val PAGE_COUNT = 3
    }
}
