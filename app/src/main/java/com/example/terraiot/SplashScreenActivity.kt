package com.example.terraiot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var accessCodeInput: TextInputEditText
    private lateinit var accessCodeLayout: TextInputLayout
    private lateinit var accessButton: MaterialButton
    private lateinit var progressBar: View

    // The correct access code
    private val correctCode = "unir"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize UI components
        accessCodeInput = findViewById(R.id.etAccessCode)
        accessCodeLayout = findViewById(R.id.tilAccessCode)
        accessButton = findViewById(R.id.btnAccess)
        progressBar = findViewById(R.id.progressBar)

        // Set up access button click listener
        accessButton.setOnClickListener {
            validateAccessCode()
        }
    }

    private fun validateAccessCode() {
        val enteredCode = accessCodeInput.text.toString().trim()

        if (enteredCode.isEmpty()) {
            accessCodeLayout.error = "Please enter the access code"
            return
        }

        // Clear any previous errors
        accessCodeLayout.error = null

        // Show progress indicator
        progressBar.visibility = View.VISIBLE
        accessButton.isEnabled = false

        // Simulate a brief verification process
        accessButton.postDelayed({
            if (enteredCode == correctCode) {
                // Code is correct, proceed to MainActivity
                proceedToMainActivity()
            } else {
                // Code is incorrect, show error
                accessCodeLayout.error = "Invalid access code"
                progressBar.visibility = View.GONE
                accessButton.isEnabled = true

                // Shake animation for error feedback
                accessCodeLayout.apply {
                    translationX = 0f
                    animate()
                        .translationX(20f)
                        .setDuration(50)
                        .withEndAction {
                            animate()
                                .translationX(-20f)
                                .setDuration(50)
                                .withEndAction {
                                    animate()
                                        .translationX(20f)
                                        .setDuration(50)
                                        .withEndAction {
                                            animate()
                                                .translationX(0f)
                                                .setDuration(50)
                                                .start()
                                        }.start()
                                }.start()
                        }.start()
                }
            }
        }, 800) // Delay for 800ms to simulate verification
    }

    private fun proceedToMainActivity() {
        Toast.makeText(this, "Access granted", Toast.LENGTH_SHORT).show()

        // Start MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Finish this activity so user can't go back to it
        finish()
    }
}

