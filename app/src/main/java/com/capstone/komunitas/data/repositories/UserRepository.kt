package com.capstone.komunitas.data.repositories

import com.capstone.komunitas.data.db.AppDatabase
import com.capstone.komunitas.data.db.PreferenceProvider
import com.capstone.komunitas.data.db.entities.User
import com.capstone.komunitas.data.network.BackendApi
import com.capstone.komunitas.data.network.SafeApiRequest
import com.capstone.komunitas.data.network.responses.AuthResponse

class UserRepository(
    private val api: BackendApi,
    private val db: AppDatabase,
    private val prefs: PreferenceProvider
) : SafeApiRequest(){
    suspend fun userLogin(email:String, password:String): AuthResponse {
        return apiRequest {
            api.userLogin(email, password)
        }
    }
    suspend fun userRegister(email:String, password:String, firstName:String, lastName:String): AuthResponse {
        return apiRequest {
            api.userRegister(email, password, firstName, lastName)
        }
    }

    fun getAuthToken(): String? {
        return prefs.getAuthToken()
    }

    fun saveAuthToken(authToken: String) {
        prefs.saveAuthToken(authToken)
    }

    suspend fun saveUser(user: User) = db.getUserDao().upsert(user)

    fun getUser() = db.getUserDao().getuser()

    suspend fun deleteUser(){
        prefs.clearAuthToken()
        db.getUserDao().deleteuser()
    }
}