package com.capstone.komunitas.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.engines.TextToSpeechEngine
import com.capstone.komunitas.ui.auth.AuthViewModel
import com.capstone.komunitas.ui.chat.ChatViewModel

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, textToSpeechEngine,context) as T
    }
}