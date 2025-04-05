package com.example.terraiot


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale

class TableFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabExport: FloatingActionButton
    private var adapter: ReadingsAdapter? = null
    private var pendingReadings: List<ElectricalReading>? = null

    // Create a launcher for the file creation intent
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            exportDataToCsv(uri)
        } else {
            showSnackbar("CSV export cancelled")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_table, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        fabExport = view.findViewById(R.id.fabExport)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ReadingsAdapter(emptyList())
        recyclerView.adapter = adapter

        // Set up export button
        fabExport.setOnClickListener {
            initiateCSVExport()
        }

        // If there are pending readings, update them now
        pendingReadings?.let {
            updateData(it)
            pendingReadings = null
        }
    }

    fun updateData(readings: List<ElectricalReading>) {
        if (view == null || !isAdded) {
            // Fragment isn't ready, store the readings for later update
            pendingReadings = readings
            return
        }

        adapter?.updateData(readings) ?: run {
            // If the adapter hasn't been initialized, store the readings for later update
            pendingReadings = readings
        }

        // Update the visibility of the export button
        fabExport.visibility = if (readings.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Initiate the CSV export process
     */
    private fun initiateCSVExport() {
        val readings = adapter?.getReadings() ?: emptyList()

        if (readings.isEmpty()) {
            showSnackbar("No data available to export")
            return
        }

        // Show file name dialog
        showFileNameDialog()
    }

    /**
     * Show a dialog to let the user specify the file name
     */
    private fun showFileNameDialog() {
        val defaultFileName = CsvExportUtil.generateDefaultFileName()

        val input = androidx.appcompat.widget.AppCompatEditText(requireContext())
        input.setText(defaultFileName)

        AlertDialog.Builder(requireContext())
            .setTitle("Export to CSV")
            .setMessage("Enter a file name for the CSV export:")
            .setView(input)
            .setPositiveButton("Export") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isEmpty()) {
                    showSnackbar("File name cannot be empty")
                    return@setPositiveButton
                }

                // Make sure the file name ends with .csv
                val finalFileName = if (fileName.endsWith(".csv", ignoreCase = true)) {
                    fileName
                } else {
                    "$fileName.csv"
                }

                // Launch the file creation intent
                createDocumentLauncher.launch(finalFileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Export data to CSV at the specified URI
     */
    private fun exportDataToCsv(uri: Uri) {
        val readings = adapter?.getReadings() ?: emptyList()

        // Show a loading indicator
        val loadingSnackbar = Snackbar.make(
            requireView(),
            "Exporting data...",
            Snackbar.LENGTH_INDEFINITE
        )
        loadingSnackbar.show()

        // Perform the export in a background thread
        Thread {
            val result = CsvExportUtil.exportReadingsToUri(requireContext(), uri, readings)

            activity?.runOnUiThread {
                loadingSnackbar.dismiss()

                if (result.first) {
                    // Success
                    showSuccessDialog(uri)
                } else {
                    // Error
                    showSnackbar("Export failed: ${result.second}")
                }
            }
        }.start()
    }

    /**
     * Show a success dialog with options to view the file
     */
    private fun showSuccessDialog(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Export Successful")
            .setMessage("Data has been exported successfully.")
            .setPositiveButton("OK", null)
            .setNeutralButton("View File") { _, _ ->
                // Open the file with an external app
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "text/csv")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    showSnackbar("No app found to view CSV files")
                }
            }
            .show()
    }

    /**
     * Show a snackbar message
     */
    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(
                it,
                message,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Adapter for displaying readings in a RecyclerView
     */
    private inner class ReadingsAdapter(
        private var readings: List<ElectricalReading>
    ) : RecyclerView.Adapter<ReadingsAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun updateData(newReadings: List<ElectricalReading>) {
            readings = newReadings
            notifyDataSetChanged()
        }

        fun getReadings(): List<ElectricalReading> {
            return readings
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reading, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val reading = readings[position]

            holder.tvTimestamp.text = dateFormat.format(reading.timestamp)
            holder.tvVoltage.text = String.format("%.2f V", reading.voltage)
            holder.tvCurrent.text = String.format("%.2f A", reading.current)
            holder.tvResistance.text = String.format("%.2f Ω", reading.resistance)
        }

        override fun getItemCount(): Int = readings.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTimestamp = view.findViewById<android.widget.TextView>(R.id.tvTimestamp)
            val tvVoltage = view.findViewById<android.widget.TextView>(R.id.tvVoltage)
            val tvCurrent = view.findViewById<android.widget.TextView>(R.id.tvCurrent)
            val tvResistance = view.findViewById<android.widget.TextView>(R.id.tvResistance)
        }
    }
}

