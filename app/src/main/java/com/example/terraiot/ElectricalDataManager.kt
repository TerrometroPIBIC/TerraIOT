package com.example.terraiot

import java.util.Date
import kotlin.math.max

/**
 * Singleton manager for storing and retrieving electrical readings
 */
object ElectricalDataManager {

    // List to store historical readings
    private val readings = mutableListOf<ElectricalReading>()

    // Maximum number of readings to store (for memory management)
    private const val MAX_READINGS = 100

    /**
     * Add a new reading to the history
     */
    fun addReading(voltage: Float, current: Float, resistance: Float) {
        val reading = ElectricalReading(
            timestamp = Date(),
            voltage = voltage,
            current = current,
            resistance = resistance
        )

        readings.add(reading)

        // Trim the list if it exceeds the maximum size
        if (readings.size > MAX_READINGS) {
            val startIndex = readings.size - MAX_READINGS
            val newList = readings.subList(startIndex, readings.size).toMutableList()
            readings.clear()
            readings.addAll(newList)
        }
    }

    /**
     * Get all stored readings
     */
    fun getAllReadings(): List<ElectricalReading> {
        return readings.toList()
    }

    /**
     * Get the most recent readings (limited by count)
     */
    fun getRecentReadings(count: Int): List<ElectricalReading> {
        val startIndex = max(0, readings.size - count)
        return readings.subList(startIndex, readings.size)
    }

    /**
     * Clear all stored readings
     */
    fun clearReadings() {
        readings.clear()
    }
}

