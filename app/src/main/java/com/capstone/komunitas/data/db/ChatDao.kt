package com.capstone.komunitas.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capstone.komunitas.data.db.entities.Chat

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllChat(quotes : List<Chat>)

    @Query("SELECT * FROM Chat")
    fun getChats() : LiveData<List<Chat>>
}