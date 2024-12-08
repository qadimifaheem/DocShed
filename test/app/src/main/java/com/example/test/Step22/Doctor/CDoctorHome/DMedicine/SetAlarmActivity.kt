package com.example.test.Step22.Doctor.CDoctorHome.DMedicine


import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SetAlarmActivity : AppCompatActivity() {
    private lateinit var userId: String
    private lateinit var doctorId: String
    private lateinit var doctorName: String
    private lateinit var appointmentId: String // Add this to track appointment ID
    private lateinit var firestore: FirebaseFirestore
    private lateinit var medicationInstructions: EditText
    private lateinit var numberOfDays: EditText
    private lateinit var breakfastCheckBox: CheckBox
    private lateinit var lunchCheckBox: CheckBox
    private lateinit var dinnerCheckBox: CheckBox
    private lateinit var showBreakfastTime: TextView
    private lateinit var showLunchTime: TextView
    private lateinit var showDinnerTime: TextView
    private var breakfastTime: Calendar? = null
    private var lunchTime: Calendar? = null
    private var dinnerTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_alarm)

        // Get user ID, doctor ID, doctor name, and appointment ID from the intent
        userId = intent.getStringExtra("user_id") ?: "Unknown"
        doctorId = intent.getStringExtra("doctor_id") ?: "Unknown"
        doctorName = intent.getStringExtra("doctor_name") ?: "Unknown"
        appointmentId = intent.getStringExtra("appointment_id") ?: "Unknown" // Retrieve the appointment ID

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        medicationInstructions = findViewById(R.id.medicationInstructions)
        numberOfDays = findViewById(R.id.numberofdays)
        breakfastCheckBox = findViewById(R.id.BreakfastCheckBox)
        lunchCheckBox = findViewById(R.id.LunchCheckBox)
        dinnerCheckBox = findViewById(R.id.DinnerCheckBox)
        showBreakfastTime = findViewById(R.id.showbreakfasttime)
        showLunchTime = findViewById(R.id.showlunchtime)
        showDinnerTime = findViewById(R.id.showdinnertime)
        setSupportActionBar(findViewById(R.id.toolbar))
        // Set listeners for checkboxes
        breakfastCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showTimePicker("Breakfast") else clearTime("Breakfast")
        }
        lunchCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showTimePicker("Lunch") else clearTime("Lunch")
        }
        dinnerCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showTimePicker("Dinner") else clearTime("Dinner")
        }

        // Set alarm button listener
        findViewById<Button>(R.id.setAlarmButton).setOnClickListener {
            saveAlarmDetails()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun showTimePicker(mealType: String) {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                updateTextView(mealType, time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun updateTextView(mealType: String, time: Calendar) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(time.time)

        when (mealType) {
            "Breakfast" -> {
                breakfastTime = time
                showBreakfastTime.text = "Breakfast: $formattedTime"
            }
            "Lunch" -> {
                lunchTime = time
                showLunchTime.text = "Lunch: $formattedTime"
            }
            "Dinner" -> {
                dinnerTime = time
                showDinnerTime.text = "Dinner: $formattedTime"
            }
        }
    }

    private fun clearTime(mealType: String) {
        when (mealType) {
            "Breakfast" -> {
                breakfastTime = null
                showBreakfastTime.text = ""
            }
            "Lunch" -> {
                lunchTime = null
                showLunchTime.text = ""
            }
            "Dinner" -> {
                dinnerTime = null
                showDinnerTime.text = ""
            }
        }
    }

    private fun saveAlarmDetails() {
        val instructions = medicationInstructions.text.toString()
        val days = numberOfDays.text.toString().toIntOrNull() ?: 0

        // Fetch patient details before saving the alarm
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val patientName = document.getString("username") ?: "Unknown"
                    val patientAge = document.getString("age") ?: "Unknown"
                    val patientEmail = document.getString("email") ?: "Unknown"
                    val patientPhoneNumber = document.getString("phoneNumber") ?: "Unknown"
                    val currentTimeAndDate = SimpleDateFormat("hh:mm:ss a dd-MM-yyyy", Locale.getDefault()).format(Date())
                    // Define the format for 12-hour clock with AM/PM
                    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

                    val alarmDetails = hashMapOf(
                        "medication_instructions" to instructions,
                        "number_of_days" to days,
                        "breakfast_time" to breakfastTime?.time?.let { timeFormat.format(it) },
                        "lunch_time" to lunchTime?.time?.let { timeFormat.format(it) },
                        "dinner_time" to dinnerTime?.time?.let { timeFormat.format(it) },
                        "doctor_id" to doctorId,
                        "doctor_name" to doctorName,
                        "appointment_id" to appointmentId // Add appointment_id to the details
                    ).filterValues { it != null } // Remove null values

                    // Use a transaction to handle saving alarm details, updating appointment status, and removing the appointment
                    firestore.runTransaction { transaction ->
                        // Save the alarm details
                        val alarmRef = firestore.collection("patients")
                            .document(userId)
                            .collection("Alarms")
                            .document() // Generate a new document ID for the alarm
                        transaction.set(alarmRef, alarmDetails)

                        // Update the appointment status to "appointment over" and add doctor_id and patient details to history
                        val historyRef = firestore.collection("patients")
                            .document(userId)
                            .collection("history")
                            .document(appointmentId)
                        val historyData: Map<String, Any> = mapOf(
                            "status" to "appointment over successfully",
                            "doctor_id" to doctorId, // Add doctor_id to the history document
                            "patient_name" to patientName, // Add patient details
                            "patient_age" to patientAge,
                            "patient_email" to patientEmail,
                            "patient_phone_number" to patientPhoneNumber
                        )
                        transaction.update(historyRef, historyData)
                        // Reference to the appointment in the doctor's history collection
                        val doctorHistoryRef = firestore.collection("doctors")
                            .document(doctorId)
                            .collection("history")
                            .document(appointmentId)

                        // Update the status in the doctor's history
                        val doctorHistoryData: Map<String, Any> = mapOf(
                            "status" to "appointment over successfully",
                            // ... (Add other fields if needed, like patient details) ...
                        )
                        transaction.update(doctorHistoryRef, doctorHistoryData)

                        val doctorAlarmRef = firestore.collection("doctors")
                            .document(doctorId)
                            .collection("patients")
                            .document(userId)
                            .collection("Alarms")
                            .document(appointmentId) // Use appointmentId as document ID
                        val doctorAlarmDetails = alarmDetails.toMutableMap() // Create a mutable copy
                        doctorAlarmDetails["appointment_time"] = currentTimeAndDate // Store current time and date as string
                        transaction.set(doctorAlarmRef, doctorAlarmDetails)

                        // Remove the appointment from selected_appointments
                        val selectedAppointmentRef = firestore.collection("doctors")
                            .document(doctorId)
                            .collection("selected_appointments")
                            .document(appointmentId)
                        transaction.delete(selectedAppointmentRef)

                        // Remove the appointment from appointments
                        val appointmentRef = firestore.collection("doctors")
                            .document(doctorId)
                            .collection("appointments")
                            .document(appointmentId)
                        transaction.delete(appointmentRef)





                        null
                    }.addOnSuccessListener {
                        firestore.collection("patients").document(userId)
                            .update("selected_appointments_ids", FieldValue.arrayRemove(appointmentId))

                        firestore.collection("doctors").document(doctorId)
                            .update("selected_appointments_ids", FieldValue.arrayRemove(appointmentId))
                        Toast.makeText(this, "Alarm details saved, appointment status updated, and appointment removed successfully", Toast.LENGTH_SHORT).show()
                        finish() // Close the current activity and return to MedicineActivity
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error saving alarm details, updating appointment status, or removing appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error fetching patient details", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching patient details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}