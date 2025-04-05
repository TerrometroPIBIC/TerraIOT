package com.example.terraiot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TrendExplorerActivity : AppCompatActivity(), FirebaseManager.FirebaseDataListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var pagerAdapter: TrendPagerAdapter
    private lateinit var tvConnectionStatus: TextView

    // Current data range
    private var currentRange = 10

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend_explorer)

        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize connection status text view
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)

        // Set up ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        pagerAdapter = TrendPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Graph"
                1 -> "Table"
                else -> null
            }
        }.attach()

        // Set up chip group for time range selection
        chipGroup = findViewById(R.id.timeRangeSelector)
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip10 -> {
                    currentRange = 10
                    updateData(10)
                }
                R.id.chip25 -> {
                    currentRange = 25
                    updateData(25)
                }
                R.id.chipAll -> {
                    currentRange = 0 // 0 means all
                    updateData(0)
                }
            }
        }

        // Register as a Firebase listener
        FirebaseManager.addListener(this)

        // Delay the initial data load to ensure fragments are ready
        viewPager.post {
            // Initial data load
            updateData(10) // Default to last 10 readings
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.trend_explorer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                updateData(currentRange)
                true
            }
            R.id.action_export_csv -> {
                initiateCSVExport()
                true
            }
            R.id.action_clear -> {
                ElectricalDataManager.clearReadings()
                updateData(currentRange)
                showSnackbar("Historical data cleared")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Initiate the CSV export process
     */
    private fun initiateCSVExport() {
        val readings = if (currentRange <= 0) {
            ElectricalDataManager.getAllReadings()
        } else {
            ElectricalDataManager.getRecentReadings(currentRange)
        }

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

        val input = androidx.appcompat.widget.AppCompatEditText(this)
        input.setText(defaultFileName)

        AlertDialog.Builder(this)
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
        val readings = if (currentRange <= 0) {
            ElectricalDataManager.getAllReadings()
        } else {
            ElectricalDataManager.getRecentReadings(currentRange)
        }

        // Show a loading indicator
        val loadingSnackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "Exporting data...",
            Snackbar.LENGTH_INDEFINITE
        )
        loadingSnackbar.show()

        // Perform the export in a background thread
        Thread {
            val result = CsvExportUtil.exportReadingsToUri(this, uri, readings)

            runOnUiThread {
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
        AlertDialog.Builder(this)
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
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        // Make sure we're listening for Firebase updates
        FirebaseManager.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener
        FirebaseManager.removeListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun updateData(count: Int) {
        val readings = if (count <= 0) {
            ElectricalDataManager.getAllReadings()
        } else {
            ElectricalDataManager.getRecentReadings(count)
        }

        pagerAdapter.updateData(readings)

        // Update UI based on data availability
        if (readings.isEmpty()) {
            findViewById<View>(R.id.emptyDataView).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.emptyDataView).visibility = View.GONE
        }
    }

    // FirebaseDataListener implementation

    override fun onDataUpdate(voltage: Double, current: Double) {
        // Update connection status
        runOnUiThread {
            tvConnectionStatus.text = "Connected to Firebase"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))

            // Refresh data display
            updateData(currentRange)
        }
    }

    override fun onPowerStateUpdate(isOn: Boolean) {
        // Not needed in this activity, but required by the interface
    }

    override fun onError(message: String) {
        // Update connection status
        runOnUiThread {
            tvConnectionStatus.text = "Firebase Error: $message"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

            showSnackbar("Firebase error: $message")
        }
    }
}

