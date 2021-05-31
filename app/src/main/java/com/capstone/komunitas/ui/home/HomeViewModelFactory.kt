package com.capstone.komunitas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.data.repositories.UserRepository

class HomeViewModelFactory(
    private val repository: UserRepository,
    private val chatRepository: ChatRepository
): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, chatRepository) as T
    }
}