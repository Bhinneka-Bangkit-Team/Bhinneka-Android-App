package com.capstone.komunitas.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capstone.komunitas.data.db.entities.CURRENT_USER_ID
import com.capstone.komunitas.data.db.entities.Chat

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllChat(quotes : List<Chat>)

    @Query("SELECT * FROM Chat")
    fun getChats() : LiveData<List<Chat>>

    @Query(value = "DELETE FROM Chat")
    suspend fun deleteChats()
}