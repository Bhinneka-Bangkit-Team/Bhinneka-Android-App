package com.capstone.komunitas.ui.auth

import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModel
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.util.ApiException
import com.capstone.komunitas.util.Coroutines
import com.capstone.komunitas.util.NoInternetException

class AuthViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // For data from the ui
    // In register only
    var firstName: String? = null
    var lastName: String? = null
    var passwordConfirm: String? = null

    // In login and register
    var email: String? = null
    var password: String? = null

    var authListener: AuthListener? = null

    fun getLoggetInUser() = repository.getUser()

    fun onRegisterButtonClick(view: View) {
        authListener?.onStarted()
        // Username or password is empty
        if (email.isNullOrEmpty() or password.isNullOrEmpty()
            or firstName.isNullOrEmpty() or lastName.isNullOrEmpty() or passwordConfirm.isNullOrEmpty()
        ) {
            authListener?.onFailure("Semua formulir harus diisi !")
            return
        }

        // Call api via kotlin coroutines
        Coroutines.main {
            try {
                val authResponse = repository.userRegister(email!!, password!!, firstName!!, lastName!!)
                authResponse.data?.let {
                    if (it.firstName != null) {
                        repository.saveAuthToken(authResponse.accessToken!!)
                        repository.saveUser(it)
                        authListener?.onSuccess(it)
                        return@main
                    }
                }
                authListener?.onFailure(authResponse.message!!)
            } catch (e: ApiException) {
                authListener?.onFailure(e.message!!)
            }
        }
    }

    fun onShowRegister(view: View){
        Intent(view.context, DaftarActivity::class.java).also{
            view.context.startActivity(it)
        }
    }

    fun onShowLogin(view: View){
        Intent(view.context, LoginActivity::class.java).also{
            view.context.startActivity(it)
        }
    }

    fun onLoginButtonClick(view: View) {
        authListener?.onStarted()
        // Username or password is empty
        if (email.isNullOrEmpty() or password.isNullOrEmpty()) {
            authListener?.onFailure("Email atau password tidak valid")
            return
        }

        // Call api via kotlin coroutines
        Coroutines.main {
            try {
                val authResponse = repository.userLogin(email!!, password!!)
                authResponse.data?.let {
                    if (it.firstName != null) {
                        repository.saveAuthToken(authResponse.accessToken!!)
                        repository.saveUser(it)
                        authListener?.onSuccess(it)
                        return@main
                    }
                }
                authListener?.onFailure(authResponse.message!!)
            } catch (e: ApiException) {
                authListener?.onFailure(e.message!!)
            } catch(e: NoInternetException){
                authListener?.onFailure(e.message!!)
            }
        }
    }
}