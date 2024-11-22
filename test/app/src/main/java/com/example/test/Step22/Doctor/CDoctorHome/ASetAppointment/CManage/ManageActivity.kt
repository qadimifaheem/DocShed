package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.CManage

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AppointmentAdapterA
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.SetAppiomentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
  //  private lateinit var back: CardView
    private lateinit var appointments: MutableList<Pair<String, String>> // Pair of appointmentId and formattedTime

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        //    back = findViewById(R.id.Back)
        listView = findViewById(R.id.ManageAppointmentList)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


        val userId = auth.currentUser?.uid
        setSupportActionBar(findViewById(R.id.toolbar))

        // Check if the doctor is active
        userId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }


        appointments = mutableListOf()
        fetchAppointments()

        // Back button functionality
//        back.setOnClickListener {
//            startActivity(Intent(this@ManageActivity, SetAppiomentActivity::class.java))
//        }

        // List item click listener for deleting appointment by tapping on the list
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedAppointmentNumber = position + 1 // List position is zero-based
            showChangeAppointmentDialog(selectedAppointmentNumber)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchAppointments() {
        val user = auth.currentUser
        user?.let {
            val doctorId = it.uid
            firestore.collection("doctors").document(doctorId)
                .collection("appointments")
                .orderBy("appointment_time")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val appointmentId = document.id
                        val formattedTime = document.getString("formatted_time") ?: "No time"
                        appointments.add(Pair(appointmentId, formattedTime))
                    }
                    populateListView(appointments)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load appointments: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun populateListView(appointments: List<Pair<String, String>>) {
        val appointmentTimes = appointments.map { it.second } // List of formatted times
        val adapter = AppointmentAdapterA(this, appointmentTimes)
        listView.adapter = adapter
    }


    private fun showChangeAppointmentDialog(selectedAppointmentNumber: Int) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Change Appointment by confirming appointment no.")

        // Set up the input
        val input = EditText(this)
        input.hint = "Enter appointment number"
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("Change") { _, _ ->
            val appointmentNumber = input.text.toString().toIntOrNull()
            if (appointmentNumber != null && appointmentNumber == selectedAppointmentNumber) {
                showDateTimePickerForChange(appointmentNumber)
            } else {
                Toast.makeText(this, "Appointment number does not match the selected item", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    private fun showDateTimePickerForChange(appointmentNumber: Int) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                        if (calendar.timeInMillis < System.currentTimeMillis()) {
                            Toast.makeText(
                                this,
                                "Cannot set appointment in the past!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            checkTimeConflictAndProceed(appointmentNumber, calendar.timeInMillis)
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun checkTimeConflictAndProceed(appointmentNumber: Int, newTimestamp: Long) {
        val user = auth.currentUser
        user?.let {
            val doctorId = it.uid
            firestore.collection("doctors").document(doctorId)
                .collection("appointments")
                .get()
                .addOnSuccessListener { documents ->
                    var hasConflict = false
                    for (document in documents) {
                        val existingTimestamp = document.getLong("appointment_time") ?: continue
                        if (Math.abs(existingTimestamp - newTimestamp) < 15 * 60 * 1000) {
                            hasConflict = true
                            break
                        }
                    }

                    if (hasConflict) {
                        Toast.makeText(
                            this,
                            "Cannot set appointment within 15 minutes of another appointment.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        updateAppointmentInFirestore(appointmentNumber, newTimestamp)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking conflicts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("h:mm a dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(timestamp)
    }
    private fun updateAppointmentInFirestore(appointmentNumber: Int, newTimestamp: Long) {
        val user = auth.currentUser
        user?.let {
            val doctorId = it.uid
            val formattedTime = formatDate(newTimestamp)

            // Get the appointments collection to update
            firestore.collection("doctors").document(doctorId)
                .collection("appointments")
                .orderBy("appointment_time") // Ensure ordering is consistent
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "No appointments found", Toast.LENGTH_SHORT).show()
                    } else {
                        val appointments = documents.mapIndexed { index, document -> index to document.id }.toMap()
                        val appointmentId = appointments[appointmentNumber - 1]
                        if (appointmentId != null) {
                            // Update the appointment in the appointments collection
                            firestore.collection("doctors").document(doctorId)
                                .collection("appointments")
                                .document(appointmentId)
                                .update(
                                    mapOf(
                                        "appointment_time" to newTimestamp,
                                        "formatted_time" to formattedTime
                                    )
                                )
                                .addOnSuccessListener {
                                    // Also update the selected_appointments if it exists
                                    updateSelectedAppointments(doctorId, appointmentId, newTimestamp, formattedTime)
                                    // Also update the history if it exists
                                    updateHistory(doctorId, appointmentId, newTimestamp, formattedTime)

                                    Toast.makeText(this, "Appointment updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching appointments: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedAppointments(doctorId: String, appointmentId: String, newTimestamp: Long, formattedTime: String) {
        firestore.collection("doctors").document(doctorId)
            .collection("selected_appointments")
            .whereEqualTo("appointment_id", appointmentId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update(
                        mapOf(
                            "appointment_time" to newTimestamp,
                            "formatted_time" to formattedTime
                        )
                    ).addOnSuccessListener {
                        // Optionally, you can add logging or notifications here
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating selected appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching selected appointments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHistory(doctorId: String, appointmentId: String, newTimestamp: Long, formattedTime: String) {
        firestore.collection("doctors").document(doctorId)
            .collection("history")
            .document(appointmentId)
            .get() // Get the document to retrieve the user_id
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userId = document.getString("user_id") // Fetch the user_id
                    // Update the doctor's history
                    document.reference.update(
                        mapOf(
                            "appointment_time" to newTimestamp,
                            "formatted_time" to formattedTime
                        )
                    ).addOnSuccessListener {
                        // After updating doctor's history, update the patient's history
                        if (userId != null) {
                            updatePatientHistory(userId, appointmentId, newTimestamp, formattedTime)
                        } else {
                            Toast.makeText(this, "User ID not found in doctor's history.", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating doctor's history: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Appointment not found in doctor's history.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching doctor's history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePatientHistory(userId: String, appointmentId: String, newTimestamp: Long, formattedTime: String) {
        firestore.collection("patients").document(userId)
            .collection("history")
            .document(appointmentId)
            .update(
                mapOf(
                    "appointment_time" to newTimestamp,
                    "formatted_time" to formattedTime
                )
            ).addOnSuccessListener {
                Toast.makeText(this, "Patient's appointment updated successfully!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error updating patient's appointment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    //if disabled account by admin
    // Function to check doctor's account status
    private fun checkDoctorStatus(doctorId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("doctors").document(doctorId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") ?: true
                    if (!isActive) {
                        // If the account is inactive, log out and redirect to login
                        FirebaseAuth.getInstance().signOut()
                        redirectToLogin()
                    }
                } else {
                    Toast.makeText(this, "Doctor not found", Toast.LENGTH_SHORT).show()
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
            remove("doctorId")
            apply()
            val editor: SharedPreferences.Editor = sha.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@ManageActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@ManageActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }


}