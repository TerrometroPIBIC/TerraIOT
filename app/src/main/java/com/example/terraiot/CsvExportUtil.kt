package com.example.terraiot

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for exporting data to CSV files
 */
object CsvExportUtil {
    private const val TAG = "CsvExportUtil"

    /**
     * Export readings to a CSV file at the specified URI
     * @param context The application context
     * @param uri The URI where the file should be saved
     * @param readings The list of readings to export
     * @return A pair containing success status and a message
     */
    fun exportReadingsToUri(
        context: Context,
        uri: Uri,
        readings: List<ElectricalReading>
    ): Pair<Boolean, String> {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri)

            if (outputStream == null) {
                return Pair(false, "Failed to open output stream")
            }

            OutputStreamWriter(outputStream).use { writer ->
                // Write CSV header
                writer.write("Timestamp,Voltage (V),Current (A),Resistance (Ω)\n")

                // Format for timestamps
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Write each reading
                for (reading in readings) {
                    val line = "${dateFormat.format(reading.timestamp)}," +
                            "${String.format(Locale.US, "%.2f", reading.voltage)}," +
                            "${String.format(Locale.US, "%.2f", reading.current)}," +
                            "${String.format(Locale.US, "%.2f", reading.resistance)}\n"
                    writer.write(line)
                }
            }

            Pair(true, "Data exported successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error exporting CSV", e)
            Pair(false, "Error exporting data: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during CSV export", e)
            Pair(false, "Unexpected error: ${e.message}")
        }
    }

    /**
     * Generate a default file name for the CSV export
     * @return A default file name with timestamp
     */
    fun generateDefaultFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "electrical_data_$timestamp.csv"
    }
}

