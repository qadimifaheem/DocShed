package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.BDelete

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AppointmentAdapterA
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.SetAppiomentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeleteAppointmentListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
  //  private lateinit var back: CardView
    private lateinit var appointments: MutableList<Pair<String, String>> // Pair of appointmentId and formattedTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_appointment_list)

   //     back = findViewById(R.id.Back)
        listView = findViewById(R.id.DeleteAppointmentList)
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
//            startActivity(
//                Intent(
//                    this@DeleteAppointmentListActivity,
//                    SetAppiomentActivity::class.java
//                )
//            )
//        }

        // List item click listener for deleting appointment by tapping on the list
        listView.setOnItemClickListener { _, _, position, _ ->
            val appointmentId = appointments[position].first
            showDeleteConfirmationDialog(appointmentId)
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

    // Function to show confirmation dialog before deleting appointment
    private fun showDeleteConfirmationDialog(appointmentId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to request the deletion of this appointment?")
        builder.setPositiveButton("Request Deletion") { _, _ ->
            // Call the new function to send the request
            val doctorId = auth.currentUser?.uid ?: return@setPositiveButton
            val formattedTime = appointments.find { it.first == appointmentId }?.second ?: "No time"
            sendDeletionRequestToAdmin(doctorId, appointmentId, formattedTime)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun sendDeletionRequestToAdmin(doctorId: String, appointmentId: String, formattedTime: String) {
        val deletionRequest = hashMapOf(
            "doctorId" to doctorId,
            "appointmentId" to appointmentId,
            "appointmentTime" to System.currentTimeMillis(),  // You can store the appointment time here if it's available
            "formattedTime" to formattedTime,
            "status" to "pending"  // Status set to pending for admin review
        )

        firestore.collection("admin").document("delete").collection("requests").document(appointmentId)
            .set(deletionRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Deletion request sent to admin.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send deletion request: ${e.message}", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this@DeleteAppointmentListActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@DeleteAppointmentListActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}