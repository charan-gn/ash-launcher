package app.ash.launcher.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
    }

    class PageViewHolder(val binding: ItemHomePageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = pageNames.size

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
        holder.binding.panel.setPanel(pageNames[position], listOf("", "empty", ""))
    }
}
