package com.example.terraiot

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GraphFragment : Fragment() {

    private lateinit var lineChart: LineChart
    private var readings: List<ElectricalReading> = listOf()
    private var pendingReadings: List<ElectricalReading>? = null
    private var chartInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lineChart = view.findViewById(R.id.lineChart)
        setupChart()
        chartInitialized = true

        // Se houver leituras pendentes, atualize-as agora
        pendingReadings?.let {
            updateData(it)
            pendingReadings = null
        }
    }

    private fun setupChart() {
        // Configure chart appearance
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setDrawGridBackground(false)

        // Configure X axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index >= 0 && index < readings.size) {
                    return dateFormat.format(readings[index].timestamp)
                }
                return ""
            }
        }

        // Configure left Y axis
        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)

        // Configure right Y axis
        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        // Configure legend
        val legend = lineChart.legend
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
    }

    fun updateData(newReadings: List<ElectricalReading>) {
        if (view == null || !isAdded || !chartInitialized) {
            // Fragmento não está pronto, armazene as leituras para atualização posterior
            pendingReadings = newReadings
            return
        }

        this.readings = newReadings

        if (readings.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        // Create voltage entries
        val voltageEntries = readings.mapIndexed { index, reading ->
            Entry(index.toFloat(), reading.voltage.toFloat())
        }

        // Create current entries
        val currentEntries = readings.mapIndexed { index, reading ->
            Entry(index.toFloat(), reading.current.toFloat())
        }

        // Create resistance entries
        val resistanceEntries = readings.mapIndexed { index, reading ->
            Entry(index.toFloat(), reading.resistance.toFloat())
        }

        // Create voltage dataset
        val voltageDataSet = LineDataSet(voltageEntries, "Voltage (V)")
        voltageDataSet.color = Color.parseColor("#3F51B5")
        voltageDataSet.setCircleColor(Color.parseColor("#3F51B5"))
        voltageDataSet.lineWidth = 2f
        voltageDataSet.circleRadius = 3f
        voltageDataSet.setDrawCircleHole(false)
        voltageDataSet.valueTextSize = 9f

        // Create current dataset
        val currentDataSet = LineDataSet(currentEntries, "Current (A)")
        currentDataSet.color = Color.parseColor("#FF9800")
        currentDataSet.setCircleColor(Color.parseColor("#FF9800"))
        currentDataSet.lineWidth = 2f
        currentDataSet.circleRadius = 3f
        currentDataSet.setDrawCircleHole(false)
        currentDataSet.valueTextSize = 9f

        // Create resistance dataset
        val resistanceDataSet = LineDataSet(resistanceEntries, "Resistance (Ω)")
        resistanceDataSet.color = Color.parseColor("#4CAF50")
        resistanceDataSet.setCircleColor(Color.parseColor("#4CAF50"))
        resistanceDataSet.lineWidth = 2f
        resistanceDataSet.circleRadius = 3f
        resistanceDataSet.setDrawCircleHole(false)
        resistanceDataSet.valueTextSize = 9f

        // Create line data
        val lineData = LineData(voltageDataSet, currentDataSet, resistanceDataSet)

        // Set data to chart
        lineChart.data = lineData
        lineChart.invalidate()
    }
}

