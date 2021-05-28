package com.capstone.komunitas.ui.chat

import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.data.db.entities.User

interface ChatListener {
    fun onGetStarted()
    fun onGetSuccess(chats: List<Chat>)
    fun onGetFailure(message: String)
}