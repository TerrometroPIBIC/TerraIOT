package com.example.terraiot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for electrical data - an alternative approach using MVVM architecture
 * This is not used in the current implementation but provided as an example
 * of how to implement this using modern Android architecture components
 */
class ElectricalDataViewModel : ViewModel() {

    // LiveData for UI updates
    private val _voltage = MutableLiveData<Double>()
    val voltage: LiveData<Double> = _voltage

    private val _current = MutableLiveData<Double>()
    val current: LiveData<Double> = _current

    private val _resistance = MutableLiveData<Double>()
    val resistance: LiveData<Double> = _resistance

    // Value ranges
    private val minVoltage = 1.0
    private val maxVoltage = 12.0
    private val minCurrent = 0.1
    private val maxCurrent = 2.0

    // Update interval
    private val updateInterval = 1000L

    // Flag to control updates
    private var isUpdating = false

    /**
     * Start generating electrical data
     */
    fun startUpdates() {
        if (isUpdating) return

        isUpdating = true
        viewModelScope.launch {
            while (isActive && isUpdating) {
                updateElectricalData()
                delay(updateInterval)
            }
        }
    }

    /**
     * Stop generating electrical data
     */
    fun stopUpdates() {
        isUpdating = false
    }

    /**
     * Generate random values and calculate resistance
     */
    private fun updateElectricalData() {
        // Generate random voltage
        val voltage = minVoltage + (maxVoltage - minVoltage) * Random.nextDouble()
        _voltage.value = voltage

        // Generate random current
        val current = minCurrent + (maxCurrent - minCurrent) * Random.nextDouble()
        _current.value = current

        // Calculate resistance using Ohm's law
        _resistance.value = voltage / current
    }

    /**
     * Force an immediate update
     */
    fun forceUpdate() {
        updateElectricalData()
    }
}

