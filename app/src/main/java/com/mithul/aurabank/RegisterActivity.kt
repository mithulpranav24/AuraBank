package com.mithul.aurabank // Make sure this matches your package name

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executor

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var accountNumberEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var proceedButton: Button

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // --- THIS IS THE CORRECTED AND COMPLETE INITIALIZATION CODE ---

        // Fix for the status bar collision
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Connect the Kotlin code to the UI elements from the XML layout
        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        phoneEditText = findViewById(R.id.editTextPhone)
        accountNumberEditText = findViewById(R.id.editTextAccountNumber)
        passwordEditText = findViewById(R.id.editTextPassword)
        proceedButton = findViewById(R.id.buttonProceed)

        // Set the click listener for the button
        proceedButton.setOnClickListener {
            validateInputAndProceed()
        }

        // Initialize the Biometric components
        setupBiometrics()
        // --- END OF CORRECTED CODE ---
    }

    private fun validateInputAndProceed() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val accountNumber = accountNumberEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || accountNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        // If validation passes, show the biometric prompt
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
                saveUserOnDevice()
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
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No biometric features available on this device.", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No biometrics enrolled. Please set up a fingerprint or face unlock in your device's settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUserOnDevice() {
        val sharedPreferences = getSharedPreferences("AuraBankPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("user_name", nameEditText.text.toString().trim())
        editor.putString("user_email", emailEditText.text.toString().trim())
        editor.putString("user_phone", phoneEditText.text.toString().trim())
        editor.putString("user_account_number", accountNumberEditText.text.toString().trim())
        editor.putString("user_password", passwordEditText.text.toString())
        editor.putFloat("user_balance", 0.0f)
        editor.putBoolean("is_user_registered", true)
        editor.apply()

        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_LONG).show()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}