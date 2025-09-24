package com.mithul.aurabank // Make sure this matches your package name

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    // --- Declare all UI elements ---
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var biometricLoginButton: Button
    private lateinit var registerTextView: TextView

    // --- Declare Biometric variables ---
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Fix for the status bar collision
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // --- Initialize all UI elements ---
        usernameEditText = findViewById(R.id.editTextUsername)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        biometricLoginButton = findViewById(R.id.buttonBiometricLogin)
        registerTextView = findViewById(R.id.textViewRegister)

        // --- Setup Biometrics ---
        setupBiometrics()

        // --- Set Click Listeners ---
        loginButton.setOnClickListener {
            handlePasswordLogin()
        }

        biometricLoginButton.setOnClickListener {
            showBiometricPrompt()
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handlePasswordLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Read saved data from SharedPreferences
        val sharedPreferences = getSharedPreferences("AuraBankPrefs", Context.MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString("user_name", null)
        val savedPassword = sharedPreferences.getString("user_password", null)

        // Check if credentials match
        if (username == savedUsername && password == savedPassword) {
            Toast.makeText(this, "Password Login Successful!", Toast.LENGTH_SHORT).show()
            // In the next step, we will navigate to the dashboard here
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(applicationContext, "Biometric Login Successful!", Toast.LENGTH_SHORT).show()
                // In the next step, we will navigate to the dashboard here
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
        biometricPrompt = BiometricPrompt(this, callback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AuraBank Biometric Login")
            .setSubtitle("Log in using your fingerprint or face")
            .setNegativeButtonText("Use account password")
            .build()
    }

    private fun showBiometricPrompt() {
        // Check if a user is registered before showing the prompt
        val sharedPreferences = getSharedPreferences("AuraBankPrefs", Context.MODE_PRIVATE)
        val isUserRegistered = sharedPreferences.getBoolean("is_user_registered", false)

        if (!isUserRegistered) {
            Toast.makeText(this, "No user is registered. Please register first.", Toast.LENGTH_LONG).show()
            return
        }

        // Check if device supports biometrics
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometric features are not available or not enrolled.", Toast.LENGTH_LONG).show()
        }
    }
}