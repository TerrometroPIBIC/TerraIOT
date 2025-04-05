package com.example.terraiot

import android.app.Application
import com.google.firebase.FirebaseApp

class ElectricalMonitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}

