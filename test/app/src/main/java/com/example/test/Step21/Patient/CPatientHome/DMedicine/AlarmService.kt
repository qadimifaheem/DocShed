package com.example.test.Step21.Patient.CPatientHome.DMedicine



import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.test.R

class AlarmService : Service() {

    private lateinit var ringtone: Ringtone
    private lateinit var vibrator: Vibrator

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mealType = intent?.getStringExtra("meal_type") ?: "medicine"

        // Start the ringtone and vibration
        ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        ringtone.play()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = longArrayOf(0, 500, 1000)
        vibrator.vibrate(vibrationPattern, 0) // 0 means repeat

        // Show notification
        showNotification(mealType)

        // Stop ringtone and vibration after 5 minutes
        Handler().postDelayed({
            stopAlarm()
        }, 150000) // Stop after 2.5 minutes

        return START_NOT_STICKY
    }

    private fun showNotification(mealType: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medicine_reminder_channel"

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Medicine Reminder"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app when notification is clicked
        val notificationIntent = Intent(this, PatientMedicineAlarmActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon3)
            .setContentTitle("Time for your $mealType medicine")
            .setContentText("It's time to take your $mealType medicine.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)
    }

    private fun stopAlarm() {
        ringtone.stop()
        vibrator.cancel()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone.stop()
        vibrator.cancel()
    }
}