package com.mithul.aurabank // Make sure this matches your actual package name

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val sharedPreferences = getSharedPreferences("AuraBankPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            setupBiometrics()
            showBiometricPrompt()
        } else {
            setupWelcomeScreen()
        }
    }

    // MODIFIED: The onWindowFocusChanged method has been removed.

    private fun setupWelcomeScreen() {
        setContentView(R.layout.activity_main)

        val mainContent: LinearLayout = findViewById(R.id.main_content)
        ViewCompat.setOnApplyWindowInsetsListener(mainContent) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val getStartedButton: Button = findViewById(R.id.buttonGetStarted)

        // --- NEW, MORE RELIABLE ANIMATION LOGIC ---
        // We post a 'Runnable' to the button's message queue.
        // This code will execute only after the button is fully ready on the screen.
        getStartedButton.post {
            val animationDrawable = getStartedButton.background as AnimationDrawable
            animationDrawable.setEnterFadeDuration(2000)
            animationDrawable.setExitFadeDuration(4000)
            animationDrawable.start()
        }
        // --- END OF NEW LOGIC ---

        getStartedButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // ... (setupBiometrics and showBiometricPrompt functions remain the same)
    private fun setupBiometrics() {
        setContentView(R.layout.activity_main)
        executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            }
        }
        biometricPrompt = BiometricPrompt(this, callback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AuraBank Authentication")
            .setSubtitle("Confirm your identity to continue")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}