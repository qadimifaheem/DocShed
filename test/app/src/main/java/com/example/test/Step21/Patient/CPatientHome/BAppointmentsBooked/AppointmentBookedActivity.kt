package com.example.test.Step21.Patient.CPatientHome.BAppointmentsBooked


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class AppointmentBookedActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bookedAppointmentsList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_booked)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setSupportActionBar(findViewById(R.id.toolbar))
        val userId = auth.currentUser?.uid ?: return
        // Check if the patient is active
        userId?.let {
            checkPatientStatus(it)
        } ?: run {
            redirectToLogin() // No patientId means the patient is not logged in
        }



        setupListView()
        setupRealTimeUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun setupListView() {
        bookedAppointmentsList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            bookedAppointmentsList,
            R.layout.appointment_details,
            arrayOf("doctor_name", "formatted_time", "specializations"),
            intArrayOf(R.id.doctorName, R.id.formattedTime, R.id.specializations)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val buttonAbsent: Button = view.findViewById(R.id.doctorabsent)

                val doctorId = bookedAppointmentsList[position]["doctor_id"]
                val appointmentId = bookedAppointmentsList[position]["appointment_id"]

                buttonAbsent.setOnClickListener {
                    doctorId?.let { docId ->
                        appointmentId?.let { appId ->
                            deleteAppointment(docId, appId)
                        }
                    }
                }

                return view
            }
        }

        val listView: ListView = findViewById(R.id.listViewAppointmentsBooked)
        listView.adapter = adapter
    }

    private fun setupRealTimeUpdates() {
        val userId = auth.currentUser?.uid ?: return
        val patientRef = firestore.collection("patients").document(userId)

        // Listen for changes in the patient's selected appointments
        patientRef.addSnapshotListener { documentSnapshot, exception ->
            if (exception != null) {
                showToast("Error fetching appointments: ${exception.message}")
                return@addSnapshotListener
            }

            val selectedAppointments = documentSnapshot?.get("selected_appointments_ids") as? List<String> ?: listOf()
            if (selectedAppointments.isNotEmpty()) {
                fetchAppointmentDetails(selectedAppointments)
            } else {
                bookedAppointmentsList.clear()
                adapter.notifyDataSetChanged()
                showToast("No appointments found")
            }
        }
    }

    private fun fetchAppointmentDetails(appointmentIds: List<String>) {
        val doctorAppointmentsRef = firestore.collection("doctors")
        bookedAppointmentsList.clear()

        // Listen for changes in each doctor's selected appointments
        doctorAppointmentsRef.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                showToast("Error fetching appointment details: ${exception.message}")
                return@addSnapshotListener
            }

            for (document in querySnapshot?.documents ?: listOf()) {
                val doctorId = document.id // Fetch the doctor's ID

                appointmentIds.forEach { appointmentId ->
                    val appointmentRef = document.reference.collection("selected_appointments").document(appointmentId)
                    appointmentRef.addSnapshotListener { appointmentSnapshot, exception ->
                        if (exception != null) {
                            showToast("Error fetching appointment details: ${exception.message}")
                            return@addSnapshotListener
                        }

                        val appointmentData = appointmentSnapshot?.data
                        if (appointmentData != null) {
                            val doctorName = appointmentData["doctor_name"] as? String ?: "N/A"
                            val formattedTime = appointmentData["formatted_time"] as? String ?: "N/A"
                            val specializations = (appointmentData["doctor_specializations"] as? List<String>)?.joinToString(", ") ?: "N/A"

                            val item = HashMap<String, String>().apply {
                                put("doctor_id", doctorId) // Save the doctor's ID
                                put("doctor_name", doctorName)
                                put("formatted_time", formattedTime)
                                put("specializations", specializations)
                                put("appointment_id", appointmentId) // Save the appointment ID
                            }

                            // Avoid adding duplicate items
                            if (!bookedAppointmentsList.any { it["doctor_id"] == doctorId && it["formatted_time"] == formattedTime }) {
                                bookedAppointmentsList.add(item)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun deleteAppointment(doctorId: String, appointmentId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Reference to the appointment in the patient's history
        val patientHistoryAppointmentRef = firestore.collection("patients")
            .document(userId)
            .collection("history")
            .document(appointmentId)
        // Fetch patient details (name and phone number)
        val patientRef = firestore.collection("patients").document(userId)
        patientRef.get().addOnSuccessListener { patientSnapshot ->
            if (patientSnapshot.exists()) {
                val patientName = patientSnapshot.getString("username") ?: "Unknown"
                val patientPhoneNumber = patientSnapshot.getString("phoneNumber") ?: "Unknown"

                // Start a batch to update the patient's history and delete appointment from other collections
                firestore.batch().apply {
                    // Update the status and patient details in the patient's history
                    update(patientHistoryAppointmentRef, mapOf(
                        "status" to "Doctor Absent",
                        "patient_name" to patientName,
                        "patient_phone_number" to patientPhoneNumber
                    ))


                    // Reference to the appointment in the doctor's history collection
                    val doctorHistoryAppointmentRef = firestore.collection("doctors")
                        .document(doctorId)
                        .collection("history")
                        .document(appointmentId)

                    // Update the status in the doctor's history
                    update(doctorHistoryAppointmentRef, mapOf("status" to "Doctor Absent"))

                    // Reference to the appointment in the doctor's appointments collection
                    val doctorAppointmentRef = firestore.collection("doctors")
                        .document(doctorId)
                        .collection("appointments")
                        .document(appointmentId)

                    firestore.collection("patients").document(userId).update("selected_appointments_ids", FieldValue.arrayRemove(appointmentId))

                    firestore.collection("doctors").document(doctorId)
                        .update("selected_appointments_ids", FieldValue.arrayRemove(appointmentId))

                    // Reference to the appointment in the patient's selected appointments collection
                    val patientAppointmentRef = firestore.collection("doctors")
                        .document(doctorId)
                        .collection("selected_appointments")
                        .document(appointmentId)

                    // Delete the appointment from the doctor's appointments and patient's selected appointments
                    delete(doctorAppointmentRef)
                    delete(patientAppointmentRef)
                }.commit()
                    .addOnSuccessListener {
                        showToast("Appointment status updated and deleted successfully")
                        // Remove from the local list
                        bookedAppointmentsList.removeAll { it["appointment_id"] == appointmentId }
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error deleting appointment: ${e.message}")
                    }
            } else {
                showToast("Patient details not found")
            }
        }.addOnFailureListener { e ->
            showToast("Error fetching patient details: ${e.message}")
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
            startActivity(Intent(this@AppointmentBookedActivity, LoginActivity::class.java))
            Toast.makeText(
                this@AppointmentBookedActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}