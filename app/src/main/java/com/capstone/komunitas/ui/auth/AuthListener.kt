package com.capstone.komunitas.ui.auth

import androidx.lifecycle.LiveData
import com.capstone.komunitas.data.db.entities.User

interface AuthListener {
    fun onStarted()
    fun onSuccess(user: User)
    fun onFailure(message: String)
}