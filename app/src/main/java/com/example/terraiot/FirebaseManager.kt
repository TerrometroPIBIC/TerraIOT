package com.example.terraiot

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

/**
 * Manager class for Firebase Realtime Database operations
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Firebase database reference
    private val database = FirebaseDatabase.getInstance()
    private val voltageRef = database.getReference("medidas/tensao")
    private val currentRef = database.getReference("medidas/corrente")
    private val powerRef = database.getReference("medidas/ligar")

    // Listeners
    private var voltageListener: ValueEventListener? = null
    private var currentListener: ValueEventListener? = null
    private var powerListener: ValueEventListener? = null

    // Latest values
    private var latestVoltage: Double = 0.0
    private var latestCurrent: Double = 0.0
    private var isPowerOn: Boolean = false

    // Callback interface
    interface FirebaseDataListener {
        fun onDataUpdate(voltage: Double, current: Double)
        fun onPowerStateUpdate(isOn: Boolean)
        fun onError(message: String)
    }

    // List of listeners
    private val listeners = mutableListOf<FirebaseDataListener>()

    /**
     * Start listening for real-time updates from Firebase
     */
    fun startListening() {
        if (voltageListener != null || currentListener != null || powerListener != null) {
            // Already listening
            return
        }

        // Listen for voltage updates
        voltageListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val value = snapshot.getValue(Double::class.java)
                    if (value != null) {
                        latestVoltage = value
                        notifyDataListeners()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading voltage value", e)
                    notifyError("Error reading voltage: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Voltage listener cancelled", error.toException())
                notifyError("Voltage data error: ${error.message}")
            }
        }

        // Listen for current updates
        currentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val value = snapshot.getValue(Double::class.java)
                    if (value != null) {
                        latestCurrent = value
                        notifyDataListeners()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading current value", e)
                    notifyError("Error reading current: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Current listener cancelled", error.toException())
                notifyError("Current data error: ${error.message}")
            }
        }

        // Listen for power state updates
        powerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val value = snapshot.getValue(Boolean::class.java)
                    if (value != null) {
                        isPowerOn = value
                        notifyPowerStateListeners()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading power state", e)
                    notifyError("Error reading power state: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Power state listener cancelled", error.toException())
                notifyError("Power state error: ${error.message}")
            }
        }

        // Attach listeners
        voltageRef.addValueEventListener(voltageListener!!)
        currentRef.addValueEventListener(currentListener!!)
        powerRef.addValueEventListener(powerListener!!)

        Log.d(TAG, "Started listening to Firebase updates")
    }

    /**
     * Stop listening for updates
     */
    fun stopListening() {
        voltageListener?.let { voltageRef.removeEventListener(it) }
        currentListener?.let { currentRef.removeEventListener(it) }
        powerListener?.let { powerRef.removeEventListener(it) }

        voltageListener = null
        currentListener = null
        powerListener = null

        Log.d(TAG, "Stopped listening to Firebase updates")
    }

    /**
     * Set the power state in Firebase
     */
    fun setPowerState(isOn: Boolean, callback: (Boolean) -> Unit) {
        powerRef.setValue(isOn)
            .addOnSuccessListener {
                Log.d(TAG, "Power state updated to: $isOn")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update power state", e)
                notifyError("Failed to update power state: ${e.message}")
                callback(false)
            }
    }

    /**
     * Get the current power state
     */
    fun isPowerOn(): Boolean {
        return isPowerOn
    }

    /**
     * Add a listener for data updates
     */
    fun addListener(listener: FirebaseDataListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)

            // Immediately notify with current values if available
            if (latestVoltage != 0.0 || latestCurrent != 0.0) {
                listener.onDataUpdate(latestVoltage, latestCurrent)
            }

            listener.onPowerStateUpdate(isPowerOn)
        }
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: FirebaseDataListener) {
        listeners.remove(listener)
    }

    /**
     * Notify all listeners of data updates
     */
    private fun notifyDataListeners() {
        // Calculate resistance using Ohm's law
        val resistance = if (latestCurrent != 0.0) latestVoltage / latestCurrent else 0.0

        // Add reading to the data manager
        ElectricalDataManager.addReading(latestVoltage.toFloat(), latestCurrent.toFloat(), resistance.toFloat())

        // Notify listeners
        for (listener in listeners) {
            listener.onDataUpdate(latestVoltage, latestCurrent)
        }
    }

    /**
     * Notify all listeners of power state updates
     */
    private fun notifyPowerStateListeners() {
        for (listener in listeners) {
            listener.onPowerStateUpdate(isPowerOn)
        }
    }

    /**
     * Notify all listeners of errors
     */
    private fun notifyError(message: String) {
        for (listener in listeners) {
            listener.onError(message)
        }
    }
}

