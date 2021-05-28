package com.capstone.komunitas.ui.chat

import android.view.View
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ListItemMessageSentBinding
import com.xwray.groupie.viewbinding.BindableItem

class ChatItem(
    private val chat: Chat
) : BindableItem<ListItemMessageSentBinding>(){
    override fun bind(viewBinding: ListItemMessageSentBinding, position: Int) {
        viewBinding.message = chat
    }

    override fun getLayout() = R.layout.list_item_message_sent

    override fun initializeViewBinding(view: View): ListItemMessageSentBinding {
        return ListItemMessageSentBinding.bind(view)
    }
}