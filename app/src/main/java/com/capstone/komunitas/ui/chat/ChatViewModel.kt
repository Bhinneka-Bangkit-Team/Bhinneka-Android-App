package com.capstone.komunitas.ui.chat

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.ui.home.HomeListener
import com.capstone.komunitas.util.lazyDeferred

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    var chatListener: ChatListener? = null
    var newMessageText: String? = null

    val chats by lazyDeferred {
        repository.getChat()
    }

    fun sendMessagePressed(){
        chatListener!!.onGetStarted()
    }
}