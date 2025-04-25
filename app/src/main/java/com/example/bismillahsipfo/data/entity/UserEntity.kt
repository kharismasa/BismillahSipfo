package com.example.bismillahsipfo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengguna")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val email: String,
    val nama: String
    // other fields...
)

