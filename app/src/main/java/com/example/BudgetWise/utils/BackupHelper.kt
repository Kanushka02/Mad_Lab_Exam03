package com.example.BudgetWise.utils

import android.content.Context
import com.example.BudgetWise.models.Transaction
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BackupHelper(private val context: Context) {
    private val gson = Gson()
    private val sharedPrefManager = SharedPrefManager.getInstance(context)

    companion object {
        private const val BACKUP_FOLDER = "finance_tracker_backups"
        private const val BACKUP_FILE_PREFIX = "finance_tracker_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }

    fun createBackup(): String? {
        try {
            val transactions = sharedPrefManager.getTransactions()
            val settings = mapOf(
                "budget" to sharedPrefManager.getBudget(),
                "currency" to sharedPrefManager.getCurrency(),
                "threshold" to sharedPrefManager.getBudgetThreshold()
            )

            val backupData = mapOf(
                "transactions" to transactions,
                "settings" to settings,
                "timestamp" to Date().time
            )

            val backupJson = gson.toJson(backupData)

            // Create backup folder if it doesn't exist
            val backupFolder = File(context.filesDir, BACKUP_FOLDER)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            // Create backup file
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateString = dateFormat.format(Date())
            val backupFileName = "$BACKUP_FILE_PREFIX$dateString$BACKUP_FILE_EXTENSION"
            val backupFile = File(backupFolder, backupFileName)

            FileOutputStream(backupFile).use {
                it.write(backupJson.toByteArray())
            }

            return backupFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun restoreBackup(backupFilePath: String): Boolean {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) return false

            val jsonString = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val backupDataMap = gson.fromJson(jsonString, Map::class.java)

            // Restore transactions
            val transactionsMap = backupDataMap["transactions"] as? List<*> ?: return false
            val transactionListType = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(gson.toJson(transactionsMap), transactionListType)
            sharedPrefManager.saveTransactions(transactions)

            // Restore settings
            val settingsMap = backupDataMap["settings"] as? Map<*, *> ?: return false
            val budget = (settingsMap["budget"] as? Double) ?: 0.0
            val currency = (settingsMap["currency"] as? String) ?: "$"
            val threshold = (settingsMap["threshold"] as? Int) ?: 80

            sharedPrefManager.setBudget(budget)
            sharedPrefManager.setCurrency(currency)
            sharedPrefManager.setBudgetThreshold(threshold)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getAvailableBackups(): List<Pair<String, Date>> {
        val backupFolder = File(context.filesDir, BACKUP_FOLDER)
        if (!backupFolder.exists()) return emptyList()

        val backupFiles = backupFolder.listFiles()?.filter {
            it.isFile && it.name.startsWith(BACKUP_FILE_PREFIX) && it.name.endsWith(BACKUP_FILE_EXTENSION)
        } ?: return emptyList()

        return backupFiles.map { file ->
            val dateStr = file.name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(BACKUP_FILE_EXTENSION)
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val date = try {
                dateFormat.parse(dateStr) ?: Date(file.lastModified())
            } catch (e: Exception) {
                Date(file.lastModified())
            }

            Pair(file.absolutePath, date)
        }.sortedByDescending { it.second }
    }
}

// Remember to add TypeToken import for BackupHelper:
import com.google.gson.reflect.TypeToken