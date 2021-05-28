package com.capstone.komunitas.ui.home

import com.capstone.komunitas.data.db.entities.User

interface HomeListener {
    fun onStarted()
    fun onSuccess(message: String)
    fun onFailure(message: String)
}