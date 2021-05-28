package com.capstone.komunitas.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.ui.auth.AuthViewModel
import com.capstone.komunitas.ui.chat.ChatViewModel

class ChatViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatViewModel(repository) as T
    }
}