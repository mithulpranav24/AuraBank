package com.mithul.aurabank

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val detailsTextView: TextView = view.findViewById(R.id.textViewTransactionDetails)
        val amountTextView: TextView = view.findViewById(R.id.textViewTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // --- MODIFIED: Replaced date formatting with the compatible SimpleDateFormat ---
        var formattedTime = ""
        try {
            // This is the format the Python server sends (ISO 8601 with timezone)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            // This is the user-friendly format we want to display
            val outputFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

            val date = inputFormat.parse(transaction.timestamp)
            if (date != null) {
                formattedTime = outputFormat.format(date)
            }
        } catch (e: Exception) {
            // If parsing fails, just show the raw timestamp
            formattedTime = transaction.timestamp
            e.printStackTrace()
        }
        // --- END MODIFICATION ---

        // Set the details text to "Name • Time"
        holder.detailsTextView.text = "${transaction.otherPartyName} • $formattedTime"

        if (transaction.type == "sent") {
            holder.amountTextView.text = "- ₹${"%.2f".format(transaction.amount)}"
            holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_light))
        } else {
            holder.amountTextView.text = "+ ₹${"%.2f".format(transaction.amount)}"
            holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light))
        }
    }

    override fun getItemCount() = transactions.size
}