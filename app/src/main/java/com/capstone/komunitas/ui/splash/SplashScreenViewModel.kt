package com.capstone.komunitas.ui.splash

import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.ui.auth.AuthListener

class SplashScreenViewModel (
    private val repository: UserRepository
) : ViewModel() {
    // Check if user already logged in
    fun getLoggetInUser() = repository.getUser()
}