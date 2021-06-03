package com.capstone.komunitas.ui.home

import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.ui.auth.LoginActivity
import com.capstone.komunitas.ui.chat.ChatNoVideoActivity
import com.capstone.komunitas.ui.chat.ChatWithVideoActivity
import com.capstone.komunitas.util.Coroutines

class HomeViewModel(
    private val repository: UserRepository,
    private val chatRepository: ChatRepository
): ViewModel()  {
    var homeListener: HomeListener? = null

    fun onShowChatNoVideo(view: View){
        Intent(view.context, ChatNoVideoActivity::class.java).also{
            view.context.startActivity(it)
        }
    }

    fun onShowChatWithVideo(view: View){
        Intent(view.context, ChatWithVideoActivity::class.java).also{
            view.context.startActivity(it)
        }
    }

    fun onLogoutButtonClick(view: View){
        Coroutines.main {
            chatRepository.deleteChats()
            repository.deleteUser()
            Intent(view.context, LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                view.context.startActivity(it)
            }
        }
    }
}