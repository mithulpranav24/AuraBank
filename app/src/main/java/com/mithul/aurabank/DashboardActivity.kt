package com.mithul.aurabank

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class DashboardActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var recipientAccountEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var welcomeTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var accountNumberTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var transactionRecyclerView: RecyclerView

    // This will store the logged-in user's ID
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_dashboard)

        val mainContent: LinearLayout = findViewById(R.id.main_content)
        ViewCompat.setOnApplyWindowInsetsListener(mainContent) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Get the user ID and store it in our class variable
        val userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }
        currentUserId = userId

        // Initialize all views
        welcomeTextView = findViewById(R.id.textViewWelcome)
        balanceTextView = findViewById(R.id.textViewBalance)
        accountNumberTextView = findViewById(R.id.textViewAccountNumber)
        emailTextView = findViewById(R.id.textViewEmail)
        phoneTextView = findViewById(R.id.textViewPhone)
        recipientAccountEditText = findViewById(R.id.editTextRecipientAccount)
        amountEditText = findViewById(R.id.editTextAmount)
        transactionRecyclerView = findViewById(R.id.recyclerViewTransactions)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)
        val sendMoneyButton: Button = findViewById(R.id.buttonSendMoney)
        val logoutButton: Button = findViewById(R.id.buttonLogout)

        // Fetch all data when the screen loads
        fetchAndDisplayUserData(currentUserId)
        fetchTransactionHistory(currentUserId)
        setupBiometrics()

        sendMoneyButton.setOnClickListener {
            showBiometricPrompt()
        }

        logoutButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AuraBankPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("is_logged_in", false)
            editor.apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchAndDisplayUserData(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getUserData(userId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()?.user
                    if (user != null) {
                        welcomeTextView.text = "Welcome back, ${user.name}!"
                        balanceTextView.text = "₹${"%.2f".format(user.balance)}"
                        accountNumberTextView.text = "Account: ${user.accountNumber}"
                        emailTextView.text = "Email: ${user.email}"
                        phoneTextView.text = "Phone: ${user.phoneNumber}"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchTransactionHistory(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTransactionHistory(userId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val transactions = response.body()?.transactions
                    if (!transactions.isNullOrEmpty()) {
                        transactionRecyclerView.adapter = TransactionAdapter(transactions)
                    }
                }
            } catch (e: Exception) {
                println("Failed to fetch transaction history: ${e.message}")
            }
        }
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                performTransfer()
            }
            // ... (onError and onFailed are the same)
        }
        biometricPrompt = BiometricPrompt(this, executor, callback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authorize Transaction")
            .setSubtitle("Confirm your identity to send money")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun showBiometricPrompt() {
        val recipient = recipientAccountEditText.text.toString().trim()
        val amount = amountEditText.text.toString().trim()
        if (recipient.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "Please fill in recipient and amount.", Toast.LENGTH_SHORT).show()
            return
        }
        biometricPrompt.authenticate(promptInfo)
    }

    private fun performTransfer() {
        val recipient = recipientAccountEditText.text.toString().trim()
        val amount = amountEditText.text.toString().toFloatOrNull()

        if (amount == null) {
            Toast.makeText(this, "Invalid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val request = TransferRequest(recipient, amount)
                val response = RetrofitInstance.api.transferMoney(request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    val newBalance = response.body()?.newBalance
                    Toast.makeText(this@DashboardActivity, "Transfer Successful!", Toast.LENGTH_LONG).show()
                    if (newBalance != null) {
                        balanceTextView.text = "₹${"%.2f".format(newBalance)}"
                    }
                    recipientAccountEditText.text.clear()
                    amountEditText.text.clear()

                    // Refresh the transaction history
                    fetchTransactionHistory(currentUserId)
                } else {
                    val error = response.body()?.message ?: "Transfer failed."
                    Toast.makeText(this@DashboardActivity, "Error: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}