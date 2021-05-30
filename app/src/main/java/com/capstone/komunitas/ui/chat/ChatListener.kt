package com.capstone.komunitas.ui.chat

import com.capstone.komunitas.data.db.entities.Chat

interface ChatListener {
    fun onGetStarted()
    fun onGetSuccess(chats: List<Chat>)
    fun onGetFailure(message: String)
    fun onSendStarted()
    fun onSendSuccess(message: String)
    fun onSendFailure(message: String)
    fun onBack()
    fun onChangeLens(lensFacing: Int)
    fun onRecordPressed(isRecording: Boolean)
}