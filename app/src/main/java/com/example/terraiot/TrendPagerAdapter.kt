package com.example.terraiot


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TrendPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val graphFragment = GraphFragment()
    private val tableFragment = TableFragment()
    private var currentReadings: List<ElectricalReading> = emptyList()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> graphFragment
            1 -> tableFragment
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun updateData(readings: List<ElectricalReading>) {
        currentReadings = readings
        graphFragment.updateData(readings)
        tableFragment.updateData(readings)
    }
}

