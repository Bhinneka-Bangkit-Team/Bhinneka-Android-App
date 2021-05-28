package com.capstone.komunitas.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capstone.komunitas.data.db.entities.CURRENT_USER_ID
import com.capstone.komunitas.data.db.entities.User

@Dao
interface UserDao {
    // Replace user in case of conflict
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User): Long

    @Query(value = "SELECT * FROM user WHERE uid = $CURRENT_USER_ID")
    fun getuser(): LiveData<User>
}