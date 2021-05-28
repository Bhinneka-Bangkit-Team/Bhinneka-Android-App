package com.capstone.komunitas.data.repositories

import com.capstone.komunitas.data.db.AppDatabase
import com.capstone.komunitas.data.db.entities.User
import com.capstone.komunitas.data.network.BackendApi
import com.capstone.komunitas.data.network.SafeApiRequest
import com.capstone.komunitas.data.network.responses.AuthResponse
import retrofit2.Response

class UserRepository(
    private val api: BackendApi,
    private val db: AppDatabase
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

    suspend fun saveUser(user: User) = db.getUserDao().upsert(user)

    fun getUser() = db.getUserDao().getuser()
}