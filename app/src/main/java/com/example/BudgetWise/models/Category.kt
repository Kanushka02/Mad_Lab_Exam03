package com.example.BudgetWise.models

enum class Category(val displayName: String, val colorCode: Int) {
    FOOD("Food", 0xFFE91E63.toInt()),
    TRANSPORT("Transport", 0xFF9C27B0.toInt()),
    BILLS("Bills", 0xFF3F51B5.toInt()),
    ENTERTAINMENT("Entertainment", 0xFF009688.toInt()),
    SHOPPING("Shopping", 0xFFFF9800.toInt()),
    HEALTH("Health", 0xFF8BC34A.toInt()),
    EDUCATION("Education", 0xFFFFEB3B.toInt()),
    SALARY("Salary", 0xFF4CAF50.toInt()),
    OTHER_INCOME("Other Income", 0xFF2196F3.toInt()),
    OTHER_EXPENSE("Other Expense", 0xFF607D8B.toInt());

    companion object {
        fun getExpenseCategories(): List<Category> {
            return values().filter { it != SALARY && it != OTHER_INCOME }
        }

        fun getIncomeCategories(): List<Category> {
            return listOf(SALARY, OTHER_INCOME)
        }
    }
}