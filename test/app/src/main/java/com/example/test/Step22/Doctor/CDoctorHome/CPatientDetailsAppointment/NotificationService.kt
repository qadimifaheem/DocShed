package com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment




import android.app.NotificationChannel
import com.example.test.R
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore


// NotificationService.kt
class NotificationService : Service() {
    private lateinit var firestore: FirebaseFirestore
    private var doctorId: String? = null

    override fun onCreate() {
        super.onCreate()
        firestore = FirebaseFirestore.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "appointment_channel",
                "Appointment Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        doctorId = intent?.getStringExtra("doctorId")

        // Start Firestore listener in the background
        doctorId?.let { setupFirestoreListener(it) }
        val notification = NotificationCompat.Builder(this, "appointment_channel")
            .setContentTitle("Appointment Service")
            .setContentText("Monitoring for new appointments")
            .setSmallIcon(R.drawable.icon3)
            .build()

        startForeground(1, notification) // Show foreground notification

        return START_STICKY
    }

    private fun setupFirestoreListener(doctorId: String) {
        val selectedAppointmentsRef = firestore.collection("doctors")
            .document(doctorId)
            .collection("selected_appointments")

        selectedAppointmentsRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("NotificationService", "Listen failed.", e)
                return@addSnapshotListener
            }

            for (docChange in snapshots!!.documentChanges) {
                if (docChange.type == DocumentChange.Type.ADDED) {
                    val appointmentNumber = docChange.document.getString("appointment_number") ?: "N/A"
                    showNotification("New Appointment Booked", "Appointment $appointmentNumber has been booked.")
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationId = 1 // Use a unique ID for your notification

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingNotification = notificationManager.activeNotifications.find { it.id == notificationId }

        if (existingNotification != null) {
            // Update existing notification
            val notificationBuilder = NotificationCompat.Builder(this, "appointment_channel")
                .setSmallIcon(R.drawable.icon3)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, notificationBuilder.build())
        } else {
            // Create new notification
            val notificationBuilder = NotificationCompat.Builder(this, "appointment_channel")
                .setSmallIcon(R.drawable.icon3)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
