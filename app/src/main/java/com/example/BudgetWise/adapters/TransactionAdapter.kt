package com.example.BudgetWise.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.BudgetWise.R
import com.example.BudgetWise.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val currency: String,
    private val onItemClickListener: (Transaction) -> Unit,
    private val onItemLongClickListener: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTransactionTitle)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewTransactionAmount)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewTransactionCategory)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewTransactionDate)
        val typeIndicator: View = itemView.findViewById(R.id.viewTransactionTypeIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.titleTextView.text = transaction.title

        // Format amount with currency
        val amountText = if (transaction.isExpense) {
            "- $currency${String.format("%.2f", transaction.amount)}"
        } else {
            "+ $currency${String.format("%.2f", transaction.amount)}"
        }
        holder.amountTextView.text = amountText

        // Set color for amount
        if (transaction.isExpense) {
            holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.expense_color))
        } else {
            holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.income_color))
        }

        holder.categoryTextView.text = transaction.category.displayName

        // Format date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.dateTextView.text = dateFormat.format(transaction.date)

        // Set type indicator color
        val indicatorColor = if (transaction.isExpense) {
            ContextCompat.getColor(holder.itemView.context, R.color.expense_color)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.income_color)
        }
        holder.typeIndicator.setBackgroundColor(indicatorColor)

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClickListener(transaction)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}