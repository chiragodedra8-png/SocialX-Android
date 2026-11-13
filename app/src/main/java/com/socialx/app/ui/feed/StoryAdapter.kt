package com.socialx.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.loadAvatar
import com.socialx.app.databinding.ItemStoryBinding

data class StoryGroup(
    val userId: String,
    val name: String,
    val avatar: String,
    val unseen: Boolean,
    val isMine: Boolean
)

class StoryAdapter(
    private val onOpen: (StoryGroup) -> Unit
) : RecyclerView.Adapter<StoryAdapter.VH>() {

    private val items = mutableListOf<StoryGroup>()

    fun submit(list: List<StoryGroup>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemStoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StoryGroup) {
            b.tvStoryName.text = if (item.isMine) "Your story" else item.name.split(" ").first()
            b.storyAvatar.loadAvatar(item.avatar, item.name)
            b.storyRing.setBackgroundResource(
                if (item.unseen) com.socialx.app.R.drawable.bg_story_ring
                else com.socialx.app.R.drawable.bg_story_ring_seen
            )
            b.addBadge.visibility = if (item.isMine) View.VISIBLE else View.GONE
            b.root.setOnClickListener { onOpen(item) }
        }
    }
}
