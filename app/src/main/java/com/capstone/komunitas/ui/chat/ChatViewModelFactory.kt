package com.capstone.komunitas.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.engines.TextToSpeechEngine

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, textToSpeechEngine,context) as T
    }
}