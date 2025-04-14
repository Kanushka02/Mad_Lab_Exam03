package com.example.BudgetWise.activities


import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.BudgetWise.R
import com.example.BudgetWise.adapters.TransactionAdapter
import com.example.BudgetWise.models.Transaction
import com.example.BudgetWise.utils.BackupHelper
import com.example.BudgetWise.utils.NotificationHelper
import com.example.BudgetWise.utils.SharedPrefManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var backupHelper: BackupHelper
    private lateinit var transactionAdapter: TransactionAdapter

    private lateinit var textViewBudget: TextView
    private lateinit var textViewSpent: TextView
    private lateinit var textViewRemaining: TextView
    private lateinit var progressBarBudget: LinearProgressIndicator
    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var textViewEmptyState: TextView

    companion object {
        private const val REQUEST_ADD_TRANSACTION = 1001
        private const val REQUEST_EDIT_TRANSACTION = 1002
        private const val REQUEST_BUDGET_SETTING = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize utilities
        sharedPrefManager = SharedPrefManager.getInstance(this)
        notificationHelper = NotificationHelper(this)
        backupHelper = BackupHelper(this)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize views
        textViewBudget = findViewById(R.id.textViewBudget)
        textViewSpent = findViewById(R.id.textViewSpent)
        textViewRemaining = findViewById(R.id.textViewRemaining)
        progressBarBudget = findViewById(R.id.progressBarBudget)
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions)
        textViewEmptyState = findViewById(R.id.textViewEmptyState)

        // Set up RecyclerView
        recyclerViewTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(
            emptyList(),
            sharedPrefManager.getCurrency(),
            { transaction -> onTransactionClick(transaction) },
            { transaction -> onTransactionLongClick(transaction) }
        )
        recyclerViewTransactions.adapter = transactionAdapter

        // Set up FAB
        val fabAddTransaction: FloatingActionButton = findViewById(R.id.fabAddTransaction)
        fabAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }

        // Set up budget button
        val buttonSetBudget: View = findViewById(R.id.buttonSetBudget)
        buttonSetBudget.setOnClickListener {
            val intent = Intent(this, BudgetSettingActivity::class.java)
            startActivityForResult(intent, REQUEST_BUDGET_SETTING)
        }

        // Set up analysis button
        val buttonCategoryAnalysis: View = findViewById(R.id.buttonCategoryAnalysis)
        buttonCategoryAnalysis.setOnClickListener {
            val intent = Intent(this, CategoryAnalysisActivity::class.java)
            startActivity(intent)
        }

        // Load data
        loadTransactions()
        updateSummary()

        // Check budget status for notifications
        checkBudgetStatus()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_backup -> {
                createBackup()
                true
            }
            R.id.menu_restore -> {
                restoreBackup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_TRANSACTION, REQUEST_EDIT_TRANSACTION -> {
                    loadTransactions()
                    updateSummary()
                    checkBudgetStatus()
                }
                REQUEST_BUDGET_SETTING -> {
                    updateSummary()
                    checkBudgetStatus()
                }
            }
        }
    }

    private fun loadTransactions() {
        val transactions = sharedPrefManager.getTransactions()

        // Sort transactions by date (newest first)
        val sortedTransactions = transactions.sortedByDescending { it.date }

        transactionAdapter.updateTransactions(sortedTransactions)

        // Show empty state if no transactions
        if (sortedTransactions.isEmpty()) {
            textViewEmptyState.visibility = View.VISIBLE
            recyclerViewTransactions.visibility = View.GONE
        } else {
            textViewEmptyState.visibility = View.GONE
            recyclerViewTransactions.visibility = View.VISIBLE
        }
    }

    private fun updateSummary() {
        val currency = sharedPrefManager.getCurrency()
        val budget = sharedPrefManager.getBudget()
        val spent = sharedPrefManager.getCurrentMonthExpenses()
        val remaining = budget - spent

        // Update text views
        textViewBudget.text = "$currency${String.format("%.2f", budget)}"
        textViewSpent.text = "$currency${String.format("%.2f", spent)}"
        textViewRemaining.text = "$currency${String.format("%.2f", remaining)}"

        // Update progress bar
        val percentSpent = if (budget > 0) ((spent / budget) * 100).toInt() else 0
        progressBarBudget.progress = minOf(percentSpent, 100)

        // Change progress bar color based on spending
        if (percentSpent > 100) {
            progressBarBudget.setIndicatorColor(getColor(R.color.expense_color))
        } else if (percentSpent > sharedPrefManager.getBudgetThreshold()) {
            progressBarBudget.setIndicatorColor(getColor(R.color.warning_color))
        } else {
            progressBarBudget.setIndicatorColor(getColor(R.color.colorPrimary))
        }
    }

    private fun checkBudgetStatus() {
        if (sharedPrefManager.areNotificationsEnabled()) {
            val budget = sharedPrefManager.getBudget()
            val spent = sharedPrefManager.getCurrentMonthExpenses()
            val currency = sharedPrefManager.getCurrency()

            notificationHelper.showBudgetWarningNotification(spent, budget, currency)
        }
    }

    private fun onTransactionClick(transaction: Transaction) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        intent.putExtra(AddTransactionActivity.EXTRA_TRANSACTION, transaction)
        startActivityForResult(intent, REQUEST_EDIT_TRANSACTION)
    }

    private fun onTransactionLongClick(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                sharedPrefManager.deleteTransaction(transaction.id)
                loadTransactions()
                updateSummary()
                checkBudgetStatus()
                Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createBackup() {
        val backupPath = backupHelper.createBackup()
        if (backupPath != null) {
            Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreBackup() {
        val backups = backupHelper.getAvailableBackups()
        if (backups.isEmpty()) {
            Toast.makeText(this, "No backups available", Toast.LENGTH_SHORT).show()
            return
        }

        val backupItems = backups.map { (path, date) ->
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val fileName = File(path).name
            "${dateFormat.format(date)} - $fileName"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setItems(backupItems) { _, which ->
                val (path, _) = backups[which]
                val result = backupHelper.restoreBackup(path)
                if (result) {
                    Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                    loadTransactions()
                    updateSummary()
                } else {
                    Toast.makeText(this, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}