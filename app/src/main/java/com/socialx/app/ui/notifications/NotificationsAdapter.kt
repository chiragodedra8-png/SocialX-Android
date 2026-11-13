package com.socialx.app.ui.notifications

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.Utils
import com.socialx.app.core.loadAvatar
import com.socialx.app.data.AppNotification
import com.socialx.app.databinding.ItemNotificationBinding

class NotificationsAdapter(
    private val onOpen: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

    private val items = mutableListOf<AppNotification>()

    fun submit(list: List<AppNotification>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AppNotification) {
            b.notifAvatar.loadAvatar(item.actorAvatar, item.actorName)
            b.notifText.text = "${item.actorName} ${item.text}"
            b.notifText.setTypeface(null, if (item.isRead) Typeface.NORMAL else Typeface.BOLD)
            b.notifTime.text = Utils.timeAgo(item.createdAt)
            b.notifDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
            b.root.setOnClickListener { onOpen(item) }
        }
    }
}
