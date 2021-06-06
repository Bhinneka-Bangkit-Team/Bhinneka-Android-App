package com.capstone.komunitas.ui.chat

import android.util.Log
import android.view.View
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ListItemMessageSentBinding
import com.xwray.groupie.viewbinding.BindableItem

class ChatSentItem(
    private val chat: Chat,
    private val viewModel: ChatViewModel
) : BindableItem<ListItemMessageSentBinding>(){

    override fun getLayout() = R.layout.list_item_message_sent

    override fun initializeViewBinding(view: View): ListItemMessageSentBinding {
        return ListItemMessageSentBinding.bind(view)
    }

    override fun bind(viewBinding: ListItemMessageSentBinding, position: Int) {
        viewBinding.message = chat
        viewBinding.micChat.setOnClickListener{
            Log.d("RECYCLERVIEW","MIC CHAT BUTTON PRESSED")
            viewModel.downloadAudio(chat.text!!)
        }
    }
}