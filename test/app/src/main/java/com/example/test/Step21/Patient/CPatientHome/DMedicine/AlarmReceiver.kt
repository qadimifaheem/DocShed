package com.example.test.Step21.Patient.CPatientHome.DMedicine



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mealType = intent.getStringExtra("meal_type") ?: "medicine"

        // Start AlarmService as a Foreground Service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("meal_type", mealType)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}