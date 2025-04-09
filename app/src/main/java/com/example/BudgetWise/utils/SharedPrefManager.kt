import android.content.Context
import android.content.SharedPreferences
import com.example.BudgetWise.models.Category
import com.example.BudgetWise.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class SharedPrefManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "FinanceTrackerPrefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_BUDGET = "budget"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_NOTIFICATION_ENABLED = "notifications_enabled"
        private const val KEY_BUDGET_THRESHOLD = "budget_threshold" // percentage of budget to trigger warning

        // Singleton instance
        @Volatile
        private var instance: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPrefManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Transaction Methods
    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        editor.putString(KEY_TRANSACTIONS, json)
        editor.apply()
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun updateTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(transactionId: String) {
        val transactions = getTransactions().toMutableList()
        transactions.removeIf { it.id == transactionId }
        saveTransactions(transactions)
    }

    // Budget Methods
    fun setBudget(amount: Double) {
        editor.putFloat(KEY_BUDGET, amount.toFloat())
        editor.apply()
    }

    fun getBudget(): Double {
        return sharedPreferences.getFloat(KEY_BUDGET, 0f).toDouble()
    }

    // Currency Methods
    fun setCurrency(currency: String) {
        editor.putString(KEY_CURRENCY, currency)
        editor.apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "$") ?: "$"
    }

    // Notification Methods
    fun setNotificationsEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
        editor.apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    // Budget Threshold Methods
    fun setBudgetThreshold(percentage: Int) {
        editor.putInt(KEY_BUDGET_THRESHOLD, percentage)
        editor.apply()
    }

    fun getBudgetThreshold(): Int {
        return sharedPreferences.getInt(KEY_BUDGET_THRESHOLD, 80) // Default 80%
    }

    // Helper Methods
    fun getTransactionsForMonth(year: Int, month: Int): List<Transaction> {
        val calendar = Calendar.getInstance()
        return getTransactions().filter { transaction ->
            calendar.time = transaction.date
            calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month
        }
    }

    fun getCurrentMonthExpenses(): Double {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        return getTransactionsForMonth(year, month)
            .filter { it.isExpense }
            .sumOf { it.amount }
    }

    fun getCategoryExpenses(year: Int, month: Int): Map<Category, Double> {
        val expenses = mutableMapOf<Category, Double>()

        getTransactionsForMonth(year, month)
            .filter { it.isExpense }
            .forEach {
                val currentAmount = expenses.getOrDefault(it.category, 0.0)
                expenses[it.category] = currentAmount + it.amount
            }

        return expenses
    }
}
