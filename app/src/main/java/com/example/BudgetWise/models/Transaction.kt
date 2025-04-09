package com.example.BudgetWise.models

import java.io.Serializable
import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var amount: Double,
    var category: Category,
    var date: Date,
    var isExpense: Boolean
) : Serializable