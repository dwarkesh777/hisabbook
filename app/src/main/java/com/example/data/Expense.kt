package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val note: String = "",
    val isIncome: Boolean = false,
    val paymentMethod: String = "Online" // "Online" or "Cash"
) : Serializable
