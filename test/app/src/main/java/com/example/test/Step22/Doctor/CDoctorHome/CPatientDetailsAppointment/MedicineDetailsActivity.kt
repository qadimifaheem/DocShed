package com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test.R
import com.google.firebase.firestore.FirebaseFirestore

class MedicineDetailsActivity : AppCompatActivity() {

    private var userId: String? = null
    private var doctorId: String? = null
    private lateinit var medicineDetailsListView: ListView
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_details)


        setSupportActionBar(findViewById(R.id.toolbar))
        // Initialize views and Firestore instance
        medicineDetailsListView = findViewById(R.id.medicineDetailsListView)
        db = FirebaseFirestore.getInstance()

        // Retrieve userId and doctorId from the intent
        userId = intent.getStringExtra("userId")
        doctorId = intent.getStringExtra("doctorId")

        if (userId.isNullOrEmpty() || doctorId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user or doctor ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch and display medicine details from Firestore
        fetchMedicineDetails()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun fetchMedicineDetails() {
        val alarmRef = db.collection("doctors")
            .document(doctorId!!)
            .collection("patients")
            .document(userId!!)
            .collection("Alarms")

        alarmRef.get()
            .addOnSuccessListener { documents ->
                val alarmList = mutableListOf<Map<String, String>>()

                for (document in documents) {
                    // Adding prefixes to each field value
                    val alarmData = mutableMapOf<String, String>()
                    alarmData["appointment_time"] = "Appointment Time: " + (document.getString("appointment_time") ?: "N/A")
                    alarmData["medication_instructions"] = "Medication Instructions: " + (document.getString("medication_instructions") ?: "N/A")

                    // Use getLong for 'number_of_days' to handle non-String types, with a prefix
                    val numberOfDays = document.getLong("number_of_days")?.toString() ?: "N/A"
                    alarmData["number_of_days"] = "Number of Days: $numberOfDays"

                    alarmData["breakfast_time"] = "Breakfast Time: " + (document.getString("breakfast_time") ?: "N/A")
                    alarmData["lunch_time"] = "Lunch Time: " + (document.getString("lunch_time") ?: "N/A")
                    alarmData["dinner_time"] = "Dinner Time: " + (document.getString("dinner_time") ?: "N/A")

                    // Add the map with prefixed data to the list
                    alarmList.add(alarmData)
                }

                // Set up the SimpleAdapter with prefixed data
                val adapter = SimpleAdapter(
                    this,
                    alarmList,
                    R.layout.multi_lines2,
                    arrayOf(
                        "appointment_time", "medication_instructions", "number_of_days",
                        "breakfast_time", "lunch_time", "dinner_time"
                    ),
                    intArrayOf(
                        R.id.appointmentTime, R.id.medicationInstructions, R.id.numberOfDays,
                        R.id.breakfastTime, R.id.lunchTime, R.id.dinnerTime
                    )
                )

                medicineDetailsListView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}