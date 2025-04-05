package com.example.terraiot

import java.util.Date

/**
 * Data class representing a single electrical reading
 */
data class ElectricalReading(
    val voltage: Float,
    val current: Float,
    val resistance: Float,
    val timestamp: Date = Date()
)

