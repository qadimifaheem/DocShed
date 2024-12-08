package com.example.test.Step21.Patient.CPatientHome.DMedicine
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PatientMedicineAlarmActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var alarmList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_medicine_alarm)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid ?: return

        setSupportActionBar(findViewById(R.id.toolbar))
        // Initialize ListView and adapter
        alarmList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            alarmList,
            R.layout.alarm_list_item,
            arrayOf("doctor_name", "medication_instructions", "number_of_days", "breakfast_time", "lunch_time", "dinner_time"),
            intArrayOf(R.id.doctorName1, R.id.medicationInstructions1, R.id.numberOfDays1, R.id.breakfastTime1, R.id.lunchTime1, R.id.dinnerTime1)
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                val view = super.getView(position, convertView, parent)
                val alarmOffButton: Button = view.findViewById(R.id.AlarmOff)
                alarmOffButton.setOnClickListener {
                    cancelAlarmsForPosition(position)
                    Toast.makeText(this@PatientMedicineAlarmActivity, "Alarms turned off completely $position.", Toast.LENGTH_SHORT).show()
                }
                return view
            }
        }
        findViewById<ListView>(R.id.listViewAlarms).adapter = adapter
        fetchAlarmDetails()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchAlarmDetails() {
        firestore.collection("patients")
            .document(userId)
            .collection("Alarms")
            .get()
            .addOnSuccessListener { querySnapshot ->
                alarmList.clear()
                for (document in querySnapshot) {
                    val alarmDetails = document.data
                    val numberOfDays = alarmDetails["number_of_days"].toString().toIntOrNull() ?: 1

                    // Convert times to 12-hour format
                    val breakfastTime12Hr = alarmDetails["breakfast_time"].toString()
                    val lunchTime12Hr = alarmDetails["lunch_time"].toString()
                    val dinnerTime12Hr = alarmDetails["dinner_time"].toString()

                    val item = HashMap<String, String>().apply {
                        put("doctor_name", "Doctor Name: ${alarmDetails["doctor_name"].toString()}")
                        put("medication_instructions", "Medication Instructions: ${alarmDetails["medication_instructions"].toString()}")
                        put("number_of_days", "Number of Days: $numberOfDays")
                        put("breakfast_time", "Breakfast Time: $breakfastTime12Hr")
                        put("lunch_time", "Lunch Time: $lunchTime12Hr")
                        put("dinner_time", "Dinner Time: $dinnerTime12Hr")
                    }
                    alarmList.add(item)

                    val breakfastTime = convertTo24HourFormat(alarmDetails["breakfast_time"].toString())
                    val lunchTime = convertTo24HourFormat(alarmDetails["lunch_time"].toString())
                    val dinnerTime = convertTo24HourFormat(alarmDetails["dinner_time"].toString())

                    scheduleAlarms(breakfastTime, numberOfDays, "breakfast")
                    scheduleAlarms(lunchTime, numberOfDays, "lunch")
                    scheduleAlarms(dinnerTime, numberOfDays, "dinner")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PatientMedicineAlarm", "Error fetching alarm details: ${e.message}")
                Toast.makeText(this, "Error fetching alarm details", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to cancel alarms for the specific list item (position)
    private fun cancelAlarmsForPosition(position: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val mealTypes = listOf("breakfast", "lunch", "dinner")

        for (i in mealTypes.indices) {
            val requestCode = mealTypes[i].hashCode() + userId.hashCode() + position
            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)  // Cancel the alarm
        }
    }

    private fun scheduleAlarms(time: List<Int>, numberOfDays: Int, mealType: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time[0])
            set(Calendar.MINUTE, time[1])
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        for (i in 0 until numberOfDays) {
            val triggerTime = calendar.timeInMillis

            // Use a unique requestCode by combining mealType hashCode and day index
            val requestCode = mealType.hashCode() + userId.hashCode() + i
            setMealAlarm(triggerTime, requestCode, mealType)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun setMealAlarm(timeInMillis: Long, requestCode: Int, mealType: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("meal_type", mealType) // Pass the mealType as an extra
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }

    private fun convertTo24HourFormat(time: String): List<Int> {
        try {
            val parts = time.split(":")
            var hour = parts[0].toInt()
            val minute = parts[1].takeWhile { it.isDigit() }.toInt()
            if (time.contains("AM", true) || time.contains("PM", true)) {
                val isPM = time.contains("PM", true)
                if (isPM && hour < 12) hour += 12
                if (!isPM && hour == 12) hour = 0
            }
            return listOf(hour, minute)
        } catch (e: Exception) {
            return listOf(12, 0)
        }
    }




    // Function to check Patient's account status
    private fun checkPatientStatus(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") ?: true
                    if (!isActive) {
                        // If the account is inactive, log out and redirect to login
                        FirebaseAuth.getInstance().signOut()
                        redirectToLogin()
                    }
                } else {
                    Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show()
                    redirectToLogin()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error checking account status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                redirectToLogin()
            }
    }

    // Function to redirect to Login page
    private fun redirectToLogin() {
        val sha = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        with(sha.edit()) {
            remove("username")
            apply()
            val editor: SharedPreferences.Editor = sha.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@PatientMedicineAlarmActivity, LoginActivity::class.java))
            Toast.makeText(
                this@PatientMedicineAlarmActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }

}