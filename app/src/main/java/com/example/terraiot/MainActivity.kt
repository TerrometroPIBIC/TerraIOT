package com.example.terraiot


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.cardview.widget.CardView
import java.text.DecimalFormat
import kotlin.random.Random
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity(), FirebaseManager.FirebaseDataListener {

    // UI elements
    private lateinit var tvVoltageValue: TextView
    private lateinit var tvCurrentValue: TextView
    private lateinit var tvResistanceValue: TextView
    private lateinit var btnViewTrends: MaterialButton
    private lateinit var switchPower: SwitchMaterial
    private lateinit var tvConnectionStatus: TextView

    // For formatting decimal values
    private val decimalFormat = DecimalFormat("#.##")

    // Connection status
    private var isConnectedToFirebase = false

    // For periodic updates when not connected to Firebase
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    // Update interval in milliseconds
    private val updateInterval = 1000L // 1 second

    // Value ranges for random generation (used when Firebase is not available)
    private val minVoltage = 1.0
    private val maxVoltage = 12.0
    private val minCurrent = 0.1
    private val maxCurrent = 2.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        tvVoltageValue = findViewById(R.id.tvVoltageValue)
        tvCurrentValue = findViewById(R.id.tvCurrentValue)
        tvResistanceValue = findViewById(R.id.tvResistanceValue)
        btnViewTrends = findViewById(R.id.btnViewTrends)
        switchPower = findViewById(R.id.switchPower)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)

        // Set up card click listeners for additional interaction
        setupCardClickListeners()

        // Set up trend button click listener
        btnViewTrends.setOnClickListener {
            val intent = Intent(this, TrendExplorerActivity::class.java)
            startActivity(intent)
        }

        // Set up power switch listener
        switchPower.setOnCheckedChangeListener { _, isChecked ->
            if (isConnectedToFirebase) {
                updatePowerState(isChecked)
            } else {
                // If not connected to Firebase, revert the switch
                switchPower.isChecked = !isChecked
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Cannot change power state: Not connected to Firebase",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // Create the update runnable using Kotlin's lambda
        updateRunnable = Runnable {
            if (!isConnectedToFirebase) {
                updateElectricalParameters()
            }
            handler.postDelayed(updateRunnable, updateInterval)
        }

        // Try to connect to Firebase
        connectToFirebase()
    }

    private fun connectToFirebase() {
        try {
            // Update connection status
            tvConnectionStatus.text = "Connecting to Firebase..."
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_blue_dark, theme))

            // Register as a listener
            FirebaseManager.addListener(this)

            // Start listening for Firebase updates
            FirebaseManager.startListening()

            // Show a message
            Snackbar.make(
                findViewById(android.R.id.content),
                "Connecting to Firebase...",
                Snackbar.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            // If Firebase connection fails, fall back to random data
            isConnectedToFirebase = false

            // Update connection status
            tvConnectionStatus.text = "Offline Mode"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

            // Disable power switch
            switchPower.isEnabled = false

            Snackbar.make(
                findViewById(android.R.id.content),
                "Failed to connect to Firebase: ${e.message}. Using simulated data.",
                Snackbar.LENGTH_LONG
            ).show()

            // Start generating random data
            handler.post(updateRunnable)
        }
    }

    private fun updatePowerState(isOn: Boolean) {
        // Disable the switch while updating
        switchPower.isEnabled = false

        // Show updating indicator
        val loadingSnackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "Updating power state...",
            Snackbar.LENGTH_INDEFINITE
        )
        loadingSnackbar.show()

        // Update Firebase
        FirebaseManager.setPowerState(isOn) { success ->
            runOnUiThread {
                // Re-enable the switch
                switchPower.isEnabled = true

                // Dismiss the loading snackbar
                loadingSnackbar.dismiss()

                if (success) {
                    // Show success message
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        if (isOn) "Power turned ON" else "Power turned OFF",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    // If failed, revert the switch
                    switchPower.isChecked = !isOn

                    // Show error message
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Failed to update power state",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isConnectedToFirebase) {
            // If not connected to Firebase, start periodic updates
            handler.post(updateRunnable)
        }
    }

    override fun onPause() {
        super.onPause()

        // Stop updates when the activity is not visible
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove listener and stop Firebase updates
        FirebaseManager.removeListener(this)
    }

    /**
     * Generate random values for voltage and current,
     * calculate resistance, and update the UI
     * (Used when Firebase is not available)
     */
    private fun updateElectricalParameters() {
        // Generate random voltage between minVoltage and maxVoltage
        val voltage = minVoltage + (maxVoltage - minVoltage) * Random.nextDouble()

        // Generate random current between minCurrent and maxCurrent
        val current = minCurrent + (maxCurrent - minCurrent) * Random.nextDouble()

        // Calculate resistance using Ohm's law: R = V / I
        val resistance = voltage / current

        // Update UI
        updateUI(voltage, current, resistance)

        // Store the reading in the data manager
        ElectricalDataManager.addReading(voltage.toFloat().toFloat(), current.toFloat(),
            resistance.toFloat()
        )
    }

    /**
     * Update the UI with the provided values
     */
    private fun updateUI(voltage: Double, current: Double, resistance: Double) {
        // Update UI with formatted values
        tvVoltageValue.text = "${decimalFormat.format(voltage)} V"
        tvCurrentValue.text = "${decimalFormat.format(current)} A"
        tvResistanceValue.text = "${decimalFormat.format(resistance)} Ω"

        // Add animation effect for value changes
        animateValueChange(tvVoltageValue)
        animateValueChange(tvCurrentValue)
        animateValueChange(tvResistanceValue)
    }

    /**
     * Simple animation to highlight value changes
     */
    private fun animateValueChange(textView: TextView) {
        textView.alpha = 0.4f
        textView.animate().alpha(1.0f).setDuration(300).start()
    }

    /**
     * Set up click listeners for cards to provide additional interaction
     */
    private fun setupCardClickListeners() {
        findViewById<CardView>(R.id.cardVoltage).setOnClickListener {
            // Force an immediate update when voltage card is clicked
            if (!isConnectedToFirebase) {
                updateElectricalParameters()
            } else {
                Toast.makeText(this, "Using real-time data from Firebase", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<CardView>(R.id.cardCurrent).setOnClickListener {
            // Force an immediate update when current card is clicked
            if (!isConnectedToFirebase) {
                updateElectricalParameters()
            } else {
                Toast.makeText(this, "Using real-time data from Firebase", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<CardView>(R.id.cardResistance).setOnClickListener {
            // Show a brief explanation of Ohm's law
            showOhmLawToast()
        }
    }

    /**
     * Show a brief toast explaining Ohm's law
     */
    private fun showOhmLawToast() {
        Toast.makeText(
            this,
            "Ohm's Law: Resistance (R) = Voltage (V) / Current (I)",
            Toast.LENGTH_SHORT
        ).show()
    }

    // FirebaseDataListener implementation

    override fun onDataUpdate(voltage: Double, current: Double) {
        // Mark as connected to Firebase
        if (!isConnectedToFirebase) {
            isConnectedToFirebase = true

            // Update connection status
            runOnUiThread {
                tvConnectionStatus.text = "Connected to Firebase"
                tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))

                // Enable power switch
                switchPower.isEnabled = true

                // Show a message
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Connected to Firebase. Using real-time data.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            // Stop random updates
            handler.removeCallbacks(updateRunnable)
        }

        // Calculate resistance
        val resistance = if (current != 0.0) voltage / current else 0.0

        // Update UI on the main thread
        runOnUiThread {
            updateUI(voltage, current, resistance)
        }
    }

    override fun onPowerStateUpdate(isOn: Boolean) {
        // Update the switch state without triggering the listener
        runOnUiThread {
            // Temporarily remove the listener to prevent callback
            switchPower.setOnCheckedChangeListener(null)

            // Update the switch state
            switchPower.isChecked = isOn

            // Restore the listener
            switchPower.setOnCheckedChangeListener { _, isChecked ->
                if (isConnectedToFirebase) {
                    updatePowerState(isChecked)
                } else {
                    // If not connected to Firebase, revert the switch
                    switchPower.isChecked = !isChecked
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Cannot change power state: Not connected to Firebase",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onError(message: String) {
        // Show error message
        runOnUiThread {
            // Update connection status
            tvConnectionStatus.text = "Firebase Error"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

            Snackbar.make(
                findViewById(android.R.id.content),
                "Firebase error: $message. Using simulated data.",
                Snackbar.LENGTH_LONG
            ).show()

            // Mark as disconnected
            isConnectedToFirebase = false

            // Disable power switch
            switchPower.isEnabled = false

            // Start random updates
            handler.post(updateRunnable)
        }
    }
}

