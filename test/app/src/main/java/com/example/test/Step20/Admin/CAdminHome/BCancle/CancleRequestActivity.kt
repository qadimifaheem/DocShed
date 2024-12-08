package com.example.test.Step20.Admin.CAdminHome.BCancle

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class CancleRequestActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val requestsList = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cancle_request)

        setSupportActionBar(findViewById(R.id.toolbar))
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        listView = findViewById(R.id.requestListView)
        fetchRequests()
    }

    private fun fetchRequests() {
        firestore.collection("admin").document("delete").collection("requests")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val doctorId = document.getString("doctorId") ?: "" // Provide default value
                    val appointmentId = document.getString("appointmentId") ?: "" // Provide default value
                    val formattedTime = document.getString("formattedTime") ?: "" // Provide default value

                    if (doctorId.isNotEmpty() && appointmentId.isNotEmpty()) { // Check if not empty
                        firestore.collection("doctors").document(doctorId)
                            .get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctorName = doctorDoc.getString("username") ?: "N/A"
                                val specializations =
                                    doctorDoc.get("specializations") as? List<String> ?: listOf()
                                val phoneNumber = doctorDoc.getString("phoneNumber") ?: "N/A"

                                val requestMap = mapOf(
                                    "doctorName" to doctorName,
                                    "specializations" to specializations.joinToString(", "),
                                    "phoneNumber" to phoneNumber,
                                    "formattedTime" to formattedTime,
                                    "doctorId" to doctorId,
                                    "appointmentId" to appointmentId // Add appointmentId to the map
                                )

                                requestsList.add(requestMap)
                                setupListAdapter()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching requests: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun setupListAdapter() {
        val adapter = object : SimpleAdapter(
            this,
            requestsList,
            R.layout.list_item_request,
            arrayOf("doctorName", "specializations", "phoneNumber", "formattedTime"),
            intArrayOf(
                R.id.doctorNameTextView,
                R.id.specializationsTextView,
                R.id.phoneNumberTextView,
                R.id.formattedTimeTextView
            )
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)

                val approveButton = view.findViewById<Button>(R.id.ApproveButton)
                val rejectButton = view.findViewById<Button>(R.id.rejectButton)

                val request = requestsList[position]
                val appointmentId = request["appointmentId"]
                val doctorId = request["doctorId"]

                approveButton.setOnClickListener {
                    if (appointmentId != null && doctorId != null) {
                        deleteAppointment(appointmentId, doctorId)
                        // Remove the approved request from the list and update the adapter
                        requestsList.removeAt(position)
                        notifyDataSetChanged()
                    }
                }

                rejectButton.setOnClickListener {
                    if (appointmentId != null) {
                        firestore.collection("admin").document("delete").collection("requests")
                            .document(appointmentId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this@CancleRequestActivity, "Request rejected.", Toast.LENGTH_SHORT).show()
                                // Remove the rejected request from the list and update the adapter
                                requestsList.removeAt(position)
                                notifyDataSetChanged()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@CancleRequestActivity,
                                    "Error rejecting request: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                return view
            }
        }
        listView.adapter = adapter
    }


    // Function to delete appointment by appointmentId or appointmentNumber
    private fun deleteAppointment(appointmentId: String? = null,doctorId: String) {
        val user = doctorId
        user?.let {
            val doctorId = user

            // Deleting by appointmentId (clicked from the list)
            if (appointmentId != null) {
                firestore.collection("doctors").document(doctorId)
                    .collection("appointments")
                    .document(appointmentId)
                    .delete()
                    .addOnSuccessListener {
                        removeAppointmentRequstFromAdmin(appointmentId)
                        removeAppointmentFromSelectedAppointments(doctorId, appointmentId)
                        updateStatusInHistory(doctorId, appointmentId)
                        Toast.makeText(
                            this,
                            "Appointment deleted successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error deleting appointment: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
             else {
                Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show()
            }
        }
    }







    // Function to update the status in both doctor and patient history
    private fun updateStatusInHistory(doctorId: String, appointmentId: String) {
        // Update in doctor's history
        firestore.collection("doctors").document(doctorId)
            .collection("history").document(appointmentId)
            .update("status", "appointment canceled")
            .addOnSuccessListener {
                // Now fetch the patientId from the same appointment document in the doctor's history
                firestore.collection("doctors").document(doctorId)
                    .collection("history").document(appointmentId)
                    .get()
                    .addOnSuccessListener { document ->
                        val patientId = document.getString("user_id")
                        if (patientId != null) {
                            // Update in patient's history
                            firestore.collection("patients").document(patientId)
                                .collection("history").document(appointmentId)
                                .update("status", "appointment canceled")
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Status updated in both histories.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error updating patient history: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Patient ID not found in doctor history document.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error retrieving doctor history: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating doctor history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun removeAppointmentRequstFromAdmin( appointmentId: String) {
        // Remove from the doctor's selected appointments
        firestore.collection("admin").document("delete").collection("requests")
            .whereEqualTo("appointmentId", appointmentId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error removing selected appointment: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // Function to remove appointment from selected_appointments collection for all patients
    private fun removeAppointmentFromSelectedAppointments(doctorId: String, appointmentId: String) {
        // Remove from the doctor's selected appointments
        firestore.collection("doctors").document(doctorId)
            .collection("selected_appointments")
            .whereEqualTo("appointment_id", appointmentId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error removing selected appointment: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // Remove appointment from doctor's selected_appointments_ids field
        removeAppointmentFromDoctorSelectedAppointmentsIds(doctorId, appointmentId)
        // Remove from each patient's selected_appointments_ids field
        removeAppointmentFromPatientsSelectedAppointmentsIds(doctorId, appointmentId)
    }




    // function to remove appointmentId from the doctor's selected_appointments_ids field
    private fun removeAppointmentFromDoctorSelectedAppointmentsIds(doctorId: String, appointmentId: String) {
        firestore.collection("doctors").document(doctorId)
            .get()
            .addOnSuccessListener { doctorDoc ->
                if (doctorDoc.exists()) {
                    val selectedAppointmentsIds = doctorDoc.get("selected_appointments_ids") as? MutableList<String>

                    if (selectedAppointmentsIds != null && selectedAppointmentsIds.contains(appointmentId)) {
                        // Remove the appointmentId from the list
                        selectedAppointmentsIds.remove(appointmentId)

                        // Update the field with the modified list
                        firestore.collection("doctors").document(doctorId)
                            .update("selected_appointments_ids", selectedAppointmentsIds)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Appointment removed from doctor's records successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error updating doctor record: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Appointment ID not found in doctor's selected appointments.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Doctor document does not exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error fetching doctor data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }





    private fun removeAppointmentFromPatientsSelectedAppointmentsIds(
        doctorId: String,
        appointmentId: String
    ) {
        // Step 1: Retrieve user IDs from doctor’s history
        firestore.collection("doctors").document(doctorId)
            .collection("history")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val patientId = document.getString("user_id") ?: continue // Fetch patient ID from history

                    // Step 2: Remove the appointmentId from the patient’s selected_appointments_ids field
                    firestore.collection("patients").document(patientId)
                        .get()
                        .addOnSuccessListener { patientDoc ->
                            val selectedAppointmentsIds =
                                patientDoc.get("selected_appointments_ids") as? MutableList<String>

                            if (selectedAppointmentsIds != null && selectedAppointmentsIds.contains(
                                    appointmentId
                                )
                            ) {
                                selectedAppointmentsIds.remove(appointmentId)

                                // Update the field with the modified list
                                firestore.collection("patients").document(patientId)
                                    .update("selected_appointments_ids", selectedAppointmentsIds)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Appointment removed from patient's records.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Error updating patient record: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error fetching patient data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving history: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

}
