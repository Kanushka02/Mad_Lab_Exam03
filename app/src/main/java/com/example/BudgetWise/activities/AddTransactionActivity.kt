package com.example.BudgetWise.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.models.Category
import com.example.personalfinancetracker.models.Transaction
import com.example.personalfinancetracker.utils.SharedPrefManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var tabLayoutTransactionType: TabLayout
    private lateinit var editTextTitle: TextInputEditText
    private lateinit var editTextAmount: TextInputEditText
    private lateinit var autoCompleteCategory: AutoCompleteTextView
    private lateinit var editTextDate: TextInputEditText
    private lateinit var buttonSaveTransaction: Button

    private lateinit var sharedPrefManager: SharedPrefManager
    private var transactionDate = Calendar.getInstance()
    private var isExpense = true
    private var editingTransaction: Transaction? = null

    companion object {
        const val EXTRA_TRANSACTION = "extra_transaction"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // Initialize SharedPrefManager
        sharedPrefManager = SharedPrefManager.getInstance(this)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        tabLayoutTransactionType = findViewById(R.id.tabLayoutTransactionType)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextAmount = findViewById(R.id.editTextAmount)
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory)
        editTextDate = findViewById(R.id.editTextDate)
        buttonSaveTransaction = findViewById(R.id.buttonSaveTransaction)

        // Check if we're editing an existing transaction
        if (intent.hasExtra(EXTRA_TRANSACTION)) {
            editingTransaction = intent.getSerializableExtra(EXTRA_TRANSACTION) as Transaction
            setupForEditing()
        } else {
            supportActionBar?.title = "Add Transaction"
            setupForNewTransaction()
        }

        // Set up transaction type tabs
        tabLayoutTransactionType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isExpense = tab.position == 0
                updateCategoryDropdown()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Set up date picker
        setupDatePicker()

        // Set up category dropdown
        updateCategoryDropdown()

        // Set up save button
        buttonSaveTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupForEditing() {
        editingTransaction?.let { transaction ->
            supportActionBar?.title = "Edit Transaction"

            // Set transaction type
            isExpense = transaction.isExpense
            tabLayoutTransactionType.selectTab(
                tabLayoutTransactionType.getTabAt(if (isExpense) 0 else 1)
            )

            // Fill form with transaction data
            editTextTitle.setText(transaction.title)
            editTextAmount.setText(String.format("%.2f", transaction.amount))

            // Set date
            transactionDate.time = transaction.date
            updateDateDisplay()

            // Need to update categories before setting the selected one
            updateCategoryDropdown()
            autoCompleteCategory.setText(transaction.category.displayName, false)
        }
    }

    private fun setupForNewTransaction() {
        // Default to current date
        updateDateDisplay()
    }

    private fun setupDatePicker() {
        editTextDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    transactionDate.set(Calendar.YEAR, year)
                    transactionDate.set(Calendar.MONTH, month)
                    transactionDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplay()
                },
                transactionDate.get(Calendar.YEAR),
                transactionDate.get(Calendar.MONTH),
                transactionDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        editTextDate.setText(dateFormat.format(transactionDate.time))
    }

    private fun updateCategoryDropdown() {
        val categories = if (isExpense) {
            Category.getExpenseCategories()
        } else {
            Category.getIncomeCategories()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )

        autoCompleteCategory.setAdapter(adapter)
    }

    private fun saveTransaction() {
        // Validate inputs
        val title = editTextTitle.text.toString().trim()
        val amountStr = editTextAmount.text.toString().trim()
        val categoryName = autoCompleteCategory.text.toString().trim()

        if (title.isEmpty()) {
            editTextTitle.error = "Please enter a title"
            return
        }

        if (amountStr.isEmpty()) {
            editTextAmount.error = "Please enter an amount"
            return
        }

        if (categoryName.isEmpty()) {
            autoCompleteCategory.error = "Please select a category"
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            editTextAmount.error = "Invalid amount"
            return
        }

        if (amount <= 0) {
            editTextAmount.error = "Amount must be greater than zero"
            return
        }

        // Find selected category
        val category = if (isExpense) {
            Category.getExpenseCategories()
        } else {
            Category.getIncomeCategories()
        }.find { it.displayName == categoryName }

        if (category == null) {
            autoCompleteCategory.error = "Invalid category"
            return
        }

        // Create or update transaction
        if (editingTransaction != null) {
            val updatedTransaction = editingTransaction!!.copy(
                title = title,
                amount = amount,
                category = category,
                date = transactionDate.time,
                isExpense = isExpense
            )
            sharedPrefManager.updateTransaction(updatedTransaction)
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            val newTransaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = transactionDate.time,
                isExpense = isExpense
            )
            sharedPrefManager.addTransaction(newTransaction)
            Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show()
        }

        // Return to main activity
        setResult(RESULT_OK)
        finish()
    }
}