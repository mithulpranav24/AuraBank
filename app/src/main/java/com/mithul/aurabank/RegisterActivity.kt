package com.mithul.aurabank // Make sure this matches your actual package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    // MODIFIED: Corrected and completed variable declarations
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var accountNumberEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var proceedButton: Button

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var confirmPasswordEditText: EditText // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // MODIFIED: Corrected and completed findViewById calls
        nameEditText = findViewById(R.id.editTextName)
        usernameEditText = findViewById(R.id.editTextUsername)
        emailEditText = findViewById(R.id.editTextEmail)
        phoneEditText = findViewById(R.id.editTextPhone)
        accountNumberEditText = findViewById(R.id.editTextAccountNumber)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword) // NEW
        proceedButton = findViewById(R.id.buttonProceed)

        proceedButton.setOnClickListener {
            validateInputAndProceed()
        }

        setupBiometrics()
    }

    private fun validateInputAndProceed() {
        // MODIFIED: Getting text from all correct fields
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val accountNumber = accountNumberEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString() // NEW

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty() || accountNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Check for password strength (e.g., at least 8 chars, 1 letter, 1 number)
        val passwordPattern = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8,16}$"
        )
        if (!passwordPattern.matcher(password).matches()) {
            Toast.makeText(this, "Password must be at least 8 characters and contain at least one letter and one number.", Toast.LENGTH_LONG).show()
            return
        }


        showBiometricPrompt()
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Auth error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                registerUserOnServer()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
        biometricPrompt = BiometricPrompt(this, executor, callback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Registration")
            .setSubtitle("Confirm your identity to register")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
            else -> Toast.makeText(this, "Biometric features not available or not enrolled.", Toast.LENGTH_LONG).show()
        }
    }

    private fun registerUserOnServer() {
        lifecycleScope.launch {
            try {
                // MODIFIED: Using all correct fields for the request
                val request = RegisterRequest(
                    name = nameEditText.text.toString().trim(),
                    username = usernameEditText.text.toString().trim(),
                    email = emailEditText.text.toString().trim(),
                    password = passwordEditText.text.toString(),
                    phoneNumber = phoneEditText.text.toString().trim(),
                    accountNumber = accountNumberEditText.text.toString().trim()
                )

                val response = RetrofitInstance.api.registerUser(request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = response.body()?.message ?: "Unknown registration error"
                    Toast.makeText(this@RegisterActivity, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}