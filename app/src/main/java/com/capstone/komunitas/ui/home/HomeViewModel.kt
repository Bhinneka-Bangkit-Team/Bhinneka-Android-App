package com.capstone.komunitas.ui.home

import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.ui.chat.ChatNoVideoActivity

class HomeViewModel(
    private val repository: UserRepository
): ViewModel()  {
    var homeListener: HomeListener? = null
    fun getLoggetInUser() = repository.getUser()

    fun onShowChatNoVideo(view: View){
        Intent(view.context, ChatNoVideoActivity::class.java).also{
            view.context.startActivity(it)
        }
    }
}