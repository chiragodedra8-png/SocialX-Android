package com.socialx.app.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socialx.app.core.loadAvatar
import com.socialx.app.data.PersonCard
import com.socialx.app.databinding.ItemUserBinding

class UserAdapter(
    private val onAction: (PersonCard) -> Unit,
    private val onOpen: (PersonCard) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    private val items = mutableListOf<PersonCard>()

    fun submit(list: List<PersonCard>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    inner class VH(private val b: ItemUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(card: PersonCard, position: Int) {
            b.userName.text = card.user.name
            b.userAvatar.loadAvatar(card.user.avatar, card.user.name)
            val mutualText = if (card.mutual > 0) " · ${card.mutual} mutual friends" else ""
            b.userSub.text = "@${card.user.username}$mutualText"
            b.userAction.text = when (card.relation) {
                PersonCard.FRIENDS -> "Friends"
                PersonCard.SENT -> "Requested"
                PersonCard.RECEIVED -> "Respond"
                PersonCard.FOLLOWING -> "Following"
                PersonCard.SELF -> "You"
                else -> "Add friend"
            }
            b.userAction.isEnabled = card.relation != PersonCard.FRIENDS &&
                card.relation != PersonCard.SELF && card.relation != PersonCard.SENT
            b.userAction.setOnClickListener { onAction(card) }
            b.root.setOnClickListener { onOpen(card) }
        }
    }
}
