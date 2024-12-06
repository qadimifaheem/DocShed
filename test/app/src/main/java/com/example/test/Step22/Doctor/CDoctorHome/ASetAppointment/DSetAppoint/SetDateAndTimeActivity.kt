package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.DSetAppoint


import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SetDateAndTimeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val calendar = Calendar.getInstance()
    private val selectedDays = mutableMapOf<String, Pair<Long, Long>>()  // Map each day to start and end timestamps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_date_and_time)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialize CheckBoxes and TextViews for each day
        val daysCheckboxes = mapOf(
            "Monday" to Pair(findViewById<CheckBox>(R.id.checkbox_monday), findViewById<TextView>(R.id.text_monday_time_range)),
            "Tuesday" to Pair(findViewById<CheckBox>(R.id.checkbox_tuesday), findViewById<TextView>(R.id.text_tuesday_time_range)),
            "Wednesday" to Pair(findViewById<CheckBox>(R.id.checkbox_wednesday), findViewById<TextView>(R.id.text_wednesday_time_range)),
            "Thursday" to Pair(findViewById<CheckBox>(R.id.checkbox_thursday), findViewById<TextView>(R.id.text_thursday_time_range)),
            "Friday" to Pair(findViewById<CheckBox>(R.id.checkbox_friday), findViewById<TextView>(R.id.text_friday_time_range)),
            "Saturday" to Pair(findViewById<CheckBox>(R.id.checkbox_saturday), findViewById<TextView>(R.id.text_saturday_time_range)),
            "Sunday" to Pair(findViewById<CheckBox>(R.id.checkbox_sunday), findViewById<TextView>(R.id.text_sunday_time_range))
        )

        daysCheckboxes.forEach { (day, views) ->
            val checkBox = views.first
            val timeTextView = views.second
            checkBox.setOnClickListener {
                if (checkBox.isChecked) {
                    showTimeRangePicker(day, timeTextView, checkBox)
                } else {
                    selectedDays.remove(day)
                    timeTextView.text = ""
                }
            }
        }

        // Save appointments button
        val saveButton = findViewById<Button>(R.id.btn_save_appointments)
        saveButton.setOnClickListener {
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "No appointments selected", Toast.LENGTH_SHORT).show()
            } else {
                selectedDays.forEach { (day, timeRange) ->
                    createAppointmentsForDay(day, timeRange.first, timeRange.second)
                }
                Toast.makeText(this, "Appointments saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun showTimeRangePicker(day: String, timeTextView: TextView, checkBox: CheckBox) {
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val targetDayOfWeek = when (day) {
            "Sunday" -> Calendar.SUNDAY
            "Monday" -> Calendar.MONDAY
            "Tuesday" -> Calendar.TUESDAY
            "Wednesday" -> Calendar.WEDNESDAY
            "Thursday" -> Calendar.THURSDAY
            "Friday" -> Calendar.FRIDAY
            "Saturday" -> Calendar.SATURDAY
            else -> currentDayOfWeek
        }

        // Find the next occurrence of the target day of the week
        val daysUntilTarget = (targetDayOfWeek + 7 - currentDayOfWeek) % 7
        val nearestDayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysUntilTarget)
        }

        val startTimePicker = TimePickerDialog(this, { _, startHour, startMinute ->
            val startCalendar = nearestDayCalendar.clone() as Calendar
            startCalendar.set(Calendar.HOUR_OF_DAY, startHour)
            startCalendar.set(Calendar.MINUTE, startMinute)

            // Check if the selected start time is in the future
            if (startCalendar.timeInMillis < System.currentTimeMillis()) {
                Toast.makeText(this, "Cannot set appointment in the past", Toast.LENGTH_SHORT).show()
                return@TimePickerDialog
            }

            val endTimePicker = TimePickerDialog(this, { _, endHour, endMinute ->
                val endCalendar = nearestDayCalendar.clone() as Calendar
                endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
                endCalendar.set(Calendar.MINUTE, endMinute)

                if (startCalendar.timeInMillis < endCalendar.timeInMillis) {
                    selectedDays[day] = startCalendar.timeInMillis to endCalendar.timeInMillis

                    // Format and display selected start and end times
                    val startTime = formatDate(startCalendar.timeInMillis)
                    val endTime = formatDate(endCalendar.timeInMillis)
                    timeTextView.text = "Selected Time: $startTime - $endTime"

                    // Uncheck the checkbox after times are set
                    checkBox.isChecked = false
                } else {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

            endTimePicker.setTitle("Select End Time")
            endTimePicker.show()

        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

        startTimePicker.setTitle("Select Start Time")
        startTimePicker.show()
    }


    private fun createAppointmentsForDay(day: String, startTime: Long, endTime: Long) {
        val doctorId = auth.currentUser?.uid
        doctorId?.let {
            var currentTime = startTime

            while (currentTime + 900_000 <= endTime) {
                checkAndSaveAppointment(it, currentTime)
                currentTime += 900_100 // Increment by 15 minutes
            }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndSaveAppointment(doctorId: String, timestamp: Long) {
        val appointmentsRef = firestore.collection("doctors").document(doctorId)
            .collection("appointments")

        val buffer = 900_000 // 15 minutes in milliseconds
        val startTime = timestamp - buffer
        val endTime = timestamp + buffer

        appointmentsRef
            .whereGreaterThanOrEqualTo("appointment_time", startTime)
            .whereLessThanOrEqualTo("appointment_time", endTime)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val conflictFound = querySnapshot.documents.any { document ->
                    val existingAppointmentTime = document.getLong("appointment_time") ?: 0L
                    val existingCalendar = Calendar.getInstance().apply { timeInMillis = existingAppointmentTime }
                    val currentCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

                    // Check for exact timestamp match or within buffer range
                    existingAppointmentTime in startTime..endTime
                }

                if (!conflictFound) {
                    saveAppointmentToFirestore(doctorId, timestamp)
                } else {
                    // Show a message indicating conflict
                    Toast.makeText(
                        this,
                        "Appointment within 15 minutes of ${formatDate(timestamp)} already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking appointment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAppointmentToFirestore(doctorId: String, timestamp: Long) {
        val appointmentData = hashMapOf(
            "appointment_time" to timestamp,
            "formatted_time" to formatDate(timestamp)
        )

        firestore.collection("doctors").document(doctorId)
            .collection("appointments")
            .add(appointmentData)
            .addOnSuccessListener {
                // Successfully saved appointment
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving appointment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("h:mm a dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(timestamp)
    }
}