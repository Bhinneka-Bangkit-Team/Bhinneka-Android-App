package com.capstone.komunitas.ui.chat

import android.view.View
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ListItemMessageReceivedBinding
import com.capstone.komunitas.databinding.ListItemMessageSentBinding
import com.xwray.groupie.viewbinding.BindableItem

class ChatReceivedItem(
    private val chat: Chat
) : BindableItem<ListItemMessageReceivedBinding>(){

    override fun getLayout() = R.layout.list_item_message_received

    override fun initializeViewBinding(view: View): ListItemMessageReceivedBinding {
        return ListItemMessageReceivedBinding.bind(view)
    }

    override fun bind(viewBinding: ListItemMessageReceivedBinding, position: Int) {
        viewBinding.message = chat

    }
}