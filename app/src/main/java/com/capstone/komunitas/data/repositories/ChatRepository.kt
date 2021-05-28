package com.capstone.komunitas.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.capstone.komunitas.data.db.AppDatabase
import com.capstone.komunitas.data.db.PreferenceProvider
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.data.network.BackendApi
import com.capstone.komunitas.data.network.SafeApiRequest
import com.capstone.komunitas.data.network.responses.ChatResponse
import com.capstone.komunitas.util.Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val MINIMUM_INTERVAL = 6

class ChatRepository(
    private val api: BackendApi,
    private val db: AppDatabase,
    private val prefs: PreferenceProvider
) : SafeApiRequest() {
    private val chats = MutableLiveData<List<Chat>>()

    init {
        chats.observeForever {
            saveChat(it)
        }
    }

    suspend fun getChat(): LiveData<List<Chat>>{
        return withContext(Dispatchers.IO){
            fetchChats()
            db.getChatDao().getChats()
        }
    }

    private suspend fun fetchChats() {
        val lastSavedAt = prefs.getLastSavedAt()
        val token = "Bearer "+prefs.getAuthToken() ?: return
        if (lastSavedAt==null) {
            val response = apiRequest {
                api.getChat(token!!)
            }
            // Check if chat data is not empty
            if(response.data != null){
                chats.postValue(response.data!!)
            }
        }else if(isFetchNeeded(LocalDateTime.parse(lastSavedAt))){
            val response = apiRequest {
                api.getChat(token!!)
            }
            // Check if chat data is not empty
            if(response.data != null){
                chats.postValue(response.data!!)
            }
        }
    }

    private fun isFetchNeeded(savedAt: LocalDateTime): Boolean {
        return ChronoUnit.HOURS.between(savedAt, LocalDateTime.now()) > MINIMUM_INTERVAL
    }

    suspend fun sendChat(text: String, isSpeaker: Int): ChatResponse? {
        val token = "Bearer "+prefs.getAuthToken() ?: return null
        return apiRequest {
            api.sendChat(token!!, text, isSpeaker, "id-ID")
        }
    }

    fun saveChat(chats: List<Chat>) {
        Coroutines.io {
            prefs.savelastSavedAt(LocalDateTime.now().toString())
            db.getChatDao().saveAllChat(chats)
        }
    }
}