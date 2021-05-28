package com.capstone.komunitas.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Chat(
    @PrimaryKey(autoGenerate = false)
    var id: Int? = null,
    var userId: String? = null,
    var text: String? = null,
    var lang: String? = null
)