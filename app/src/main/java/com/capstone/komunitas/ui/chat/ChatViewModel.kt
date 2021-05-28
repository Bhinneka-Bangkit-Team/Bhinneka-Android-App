package com.capstone.komunitas.ui.chat

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.data.repositories.ChatRepository
import com.capstone.komunitas.ui.home.HomeListener
import com.capstone.komunitas.util.ApiException
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.NoInternetException
import com.capstone.komunitas.util.lazyDeferred

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    var chatListener: ChatListener? = null
    var newMessageText: String? = null

    val chats by lazyDeferred {
        repository.getChat()
    }

    fun sendMessagePressed() {
        chatListener?.onSendStarted()
        // Username or password is empty
        if (newMessageText.isNullOrEmpty()) {
            chatListener?.onSendFailure("Pesan tidak boleh kosong")
            return
        }

        // Call api via kotlin coroutines
        Coroutines.main {
            try {
                val chatResponse = repository.sendChat(newMessageText!!, 0)
                chatResponse?.let {
                    if (it.data!!.size > 0) {
                        repository.saveChat(it.data)
                        chatListener?.onSendSuccess("Berhasil mengambil pesan")
                        return@main
                    }
                }
                chatListener?.onSendFailure(chatResponse.message!!)
            } catch (e: ApiException) {
                chatListener?.onSendFailure(e.message!!)
            } catch (e: NoInternetException) {
                chatListener?.onSendFailure(e.message!!)
            }
        }
    }
}