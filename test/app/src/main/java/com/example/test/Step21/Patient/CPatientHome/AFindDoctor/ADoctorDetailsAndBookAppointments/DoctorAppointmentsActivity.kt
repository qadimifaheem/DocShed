package com.example.test.Step21.Patient.CPatientHome.AFindDoctor.ADoctorDetailsAndBookAppointments

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf


class DoctorAppointmentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var doctorId: String
    private lateinit var listViewAppointments: ListView
    private lateinit var appointmentAdapter: PatientListOfTimeAdapter
    private val appointmentList = mutableListOf<Appointment>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_appointments)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid ?: return

        setSupportActionBar(findViewById(R.id.toolbar))

        listViewAppointments = findViewById(R.id.listViewAppointments)
        doctorId = intent.getStringExtra("doctorId") ?: return

        appointmentAdapter = PatientListOfTimeAdapter(this, appointmentList, userId)
        listViewAppointments.adapter = appointmentAdapter

        fetchAppointments()
        updateSelectedAppointments()
        listenForAppointmentChanges()

        listViewAppointments.setOnItemClickListener { _, view, position, _ ->
            val appointment = appointmentList[position]
            showConfirmationDialog(userId, appointment, view)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }


    private fun fetchAppointments() {
        firestore.collection("doctors").document(doctorId).get()
            .addOnSuccessListener { doctorSnapshot ->
                val bookedAppointments =
                    doctorSnapshot.get("selected_appointments_ids") as? List<String> ?: listOf()
                firestore.collection("patients").document(auth.currentUser?.uid!!).get()
                    .addOnSuccessListener { patientSnapshot ->
                        val userBookedAppointments =
                            patientSnapshot.get("selected_appointments_ids") as? List<String>
                                ?: listOf()
                        firestore.collection("doctors").document(doctorId)
                            .collection("appointments")
                            .orderBy("appointment_time", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                appointmentList.clear()
                                val currentTime = Calendar.getInstance().time
                                val dateFormat =
                                    SimpleDateFormat("h:mm a dd MMM yyyy", Locale.getDefault())

                                for (document in querySnapshot) {
                                    val appointmentId = document.id
                                    val formattedTime = document.getString("formatted_time") ?: ""
                                    val isBooked = bookedAppointments.contains(appointmentId)
                                    val bookedByUserId =
                                        if (userBookedAppointments.contains(appointmentId)) auth.currentUser?.uid else null

                                    val appointmentDate = dateFormat.parse(formattedTime)
                                    val isPast = appointmentDate != null && appointmentDate.before(
                                        currentTime
                                    )

                                    // Add appointment regardless of past or future
                                    appointmentList.add(
                                        Appointment(
                                            appointmentId,
                                            formattedTime,
                                            isBooked,
                                            bookedByUserId,
                                            isPast
                                        )
                                    )
                                }

                                appointmentList.sortBy { dateFormat.parse(it.formattedTime) }
                                appointmentAdapter.notifyDataSetChanged()

                                // Update Firestore with appointment numbers after sorting
                                appointmentList.forEachIndexed { index, appointment ->
                                    val appointmentNumber = "Appointment ${index + 1}"
                                    updateAppointmentNumber(
                                        appointment.appointmentId,
                                        appointmentNumber
                                    )
                                }
                            }
                    }
            }
    }

    private fun updateSelectedAppointments() {
        firestore.collection("doctors").document(doctorId).collection("selected_appointments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.forEach { document ->
                    val appointmentId = document.id
                    val bookedByUserId = document.getString("user_id") ?: ""

                    val appointmentIndex =
                        appointmentList.indexOfFirst { it.appointmentId == appointmentId }
                    if (appointmentIndex != -1) {
                        appointmentList[appointmentIndex].apply {
                            isBooked = true
                            this.bookedByUserId = bookedByUserId
                        }
                    }
                }
                appointmentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("DoctorAppointments", "Error fetching selected appointments", e)
            }
    }


    private fun showConfirmationDialog(userId: String, appointment: Appointment, view: View) {
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { patientDocument ->
                val selectedAppointments =
                    patientDocument.get("selected_appointments_ids") as? List<String> ?: listOf()

                if (selectedAppointments.isNotEmpty() && !selectedAppointments.contains(appointment.appointmentId)) {
                    // Patient already has a booked appointment, show dialog
                    AlertDialog.Builder(this)
                        .setTitle("Booking Limit Reached")
                        .setMessage("You can only book one appointment at a time. Please cancel the current appointment to book a new one.")
                        .setPositiveButton("OK", null)
                        .show()
                } else if (appointment.isBooked && appointment.bookedByUserId != userId) {
                    // Appointment is booked by another patient
                    AlertDialog.Builder(this)
                        .setTitle("Appointment Unavailable")
                        .setMessage("This appointment has been booked by another patient.")
                        .setPositiveButton("OK", null)
                        .show()
                } else if (selectedAppointments.contains(appointment.appointmentId)) {
                    // Appointment already booked by current user, show cancellation dialog
                    AlertDialog.Builder(this)
                        .setTitle("Cancel Appointment")
                        .setMessage("Are you sure you want to cancel the appointment?")
                        .setPositiveButton("Yes") { _, _ ->
                            cancelAppointment(userId, appointment)
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Appointment not booked, show booking confirmation dialog
                    AlertDialog.Builder(this)
                        .setTitle("Confirm Appointment")
                        .setMessage("Are you sure you want to book this appointment?")
                        .setPositiveButton("Confirm") { _, _ ->
                            bookAppointment(userId, appointment, view)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error fetching appointment status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cancelAppointment(userId: String, appointment: Appointment) {
        val appointmentIndex = appointmentList.indexOf(appointment)
        if (appointmentIndex != -1) {
            appointmentList[appointmentIndex].isBooked = false
            appointmentList[appointmentIndex].bookedByUserId = null
            appointmentAdapter.notifyDataSetChanged()
        }


        val batch = firestore.batch()

        // Remove appointment ID from patient's selected appointments
        val patientRef = firestore.collection("patients").document(userId)
        batch.update(patientRef, "selected_appointments_ids", FieldValue.arrayRemove(appointment.appointmentId))

        // Remove appointment ID from doctor's selected appointments
        val doctorRef = firestore.collection("doctors").document(doctorId)
        batch.update(doctorRef, "selected_appointments_ids", FieldValue.arrayRemove(appointment.appointmentId))

        // Delete the selected appointment document under doctor's collection
        val selectedAppointmentRef = doctorRef.collection("selected_appointments").document(appointment.appointmentId)
        batch.delete(selectedAppointmentRef)

        // Update the status in patient's history
        val patientHistoryRef = patientRef.collection("history").document(appointment.appointmentId)
        batch.update(patientHistoryRef, "status", "cancelled by patient")

        // Update the status in doctor's history
        val doctorHistoryRef = doctorRef.collection("history").document(appointment.appointmentId)
        batch.update(doctorHistoryRef, "status", "cancelled by patient")

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                // All operations succeeded
                fetchAppointments()
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("FirestoreBatch", "Error updating appointments", e)
            }
        // Trigger a cancellation notification and message
        val cancellationMessage =
            "Appointment cancelled successfully for ${appointment.formattedTime}"
        Toast.makeText(this, cancellationMessage, Toast.LENGTH_SHORT).show()
        scheduleNotification(
            "Appointment Cancelled",
            cancellationMessage,
            appointment.appointmentId
        )
    }

    private fun bookAppointment(userId: String, appointment: Appointment, view: View) {
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { patientDocument ->
                // Fetch patient details and handle possible type mismatches
                val patientName = patientDocument.getString("username") ?: ""
                val patientEmail = patientDocument.getString("email") ?: ""
                val patientAge = patientDocument.getString("age") ?: ""
                val patientPhoneNumber = patientDocument.getString("phoneNumber") ?: ""
                val patientGender =
                    patientDocument.get("selectedGender") as? List<String> ?: listOf()

                val appointmentIndex = appointmentList.indexOf(appointment)
                if (appointmentIndex != -1) {
                    appointmentList[appointmentIndex].apply {
                        isBooked = true
                        bookedByUserId = userId
                    }
                    appointmentAdapter.notifyDataSetChanged()
                }

                firestore.collection("doctors").document(doctorId).get()
                    .addOnSuccessListener { doctorDocument ->
                        val doctorName = doctorDocument.getString("username") ?: ""
                        val doctorSpecializations =
                            doctorDocument.get("specializations") as? List<String> ?: listOf()

                        val appointmentData = mapOf(
                            "appointment_id" to appointment.appointmentId,
                            "appointment_number" to "Appointment ${
                                appointmentList.indexOf(
                                    appointment
                                ) + 1
                            }",
                            "appointment_time" to System.currentTimeMillis().toString(),
                            "doctor_name" to doctorName,
                            "doctor_specializations" to doctorSpecializations,
                            "formatted_time" to appointment.formattedTime,
                            "doctor_id" to doctorId,
                            "status" to "processing",
                            "user_id" to userId,
                            "patient_name" to patientName,
                            "patient_email" to patientEmail,
                            "patient_age" to patientAge,
                            "patient_phone_number" to patientPhoneNumber,
                            "patient_gender" to patientGender
                        )

                        val batch = firestore.batch()




                        batch.set(
                            firestore.collection("doctors").document(doctorId)
                                .collection("selected_appointments")
                                .document(appointment.appointmentId), appointmentData
                        )
                        batch.update(
                            firestore.collection("doctors").document(doctorId),
                            "selected_appointments_ids",
                            FieldValue.arrayUnion(appointment.appointmentId)
                        )
                        batch.update(
                            firestore.collection("patients").document(userId),
                            "selected_appointments_ids",
                            FieldValue.arrayUnion(appointment.appointmentId)
                        )
                        batch.set(
                            firestore.collection("doctors").document(doctorId)
                                .collection("history").document(appointment.appointmentId),
                            appointmentData
                        )
                        batch.set(
                            firestore.collection("patients").document(userId)
                                .collection("history").document(appointment.appointmentId),
                            appointmentData
                        )

                        batch.update(
                            firestore.collection("doctors").document(doctorId)
                                .collection("appointments").document(appointment.appointmentId),
                            "isBooked",
                            true
                        )

                        batch.commit()
                            .addOnSuccessListener {
                                // ... (trigger booking notification and message) ...
                            }
                            .addOnFailureListener { e ->
                                // ... (handle error) ...
                            }
                        // Trigger a booking notification and message
                        val bookingMessage =
                            "Appointment booked successfully at ${appointment.formattedTime}"
                        Toast.makeText(this, bookingMessage, Toast.LENGTH_SHORT).show()
                        scheduleNotification(
                            "Appointment Booked",
                            bookingMessage,
                            appointment.appointmentId
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error fetching doctor details: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error fetching patient details: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateAppointmentNumber(appointmentId: String, newAppointmentNumber: String) {
        // Update in doctor's selected_appointments collection
        firestore.collection("doctors").document(doctorId)
            .collection("selected_appointments").document(appointmentId)
            .update("appointment_number", newAppointmentNumber)

        // Update in doctor's history collection
        firestore.collection("doctors").document(doctorId)
            .collection("history").document(appointmentId)
            .update("appointment_number", newAppointmentNumber)

        // Update in patient's history collection
        firestore.collection("patients").document(auth.currentUser?.uid!!)
            .collection("history").document(appointmentId)
            .update("appointment_number", newAppointmentNumber)
    }


    private fun listenForAppointmentChanges() {
        firestore.collection("doctors").document(doctorId).collection("appointments")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("DoctorAppointments", "Listen failed.", e)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { dc ->
                    val appointmentId = dc.document.id
                    val newFormattedTime = dc.document.getString("formatted_time") ?: ""

                    when (dc.type) {
                        DocumentChange.Type.MODIFIED -> {
                            val appointmentIndex =
                                appointmentList.indexOfFirst { it.appointmentId == appointmentId }

                            if (appointmentIndex != -1) {
                                val appointment = appointmentList[appointmentIndex]
                                if (appointment.formattedTime != newFormattedTime) {
                                    appointmentList[appointmentIndex] =
                                        appointment.copy(formattedTime = newFormattedTime)
                                    appointmentList.sortBy {
                                        SimpleDateFormat(
                                            "h:mm a dd MMM yyyy",
                                            Locale.getDefault()
                                        ).parse(it.formattedTime)
                                    }
                                    appointmentAdapter.notifyDataSetChanged()

                                    // Recalculate and update appointment numbers after sorting
                                    appointmentList.forEachIndexed { index, updatedAppointment ->
                                        val newAppointmentNumber = "Appointment ${index + 1}"
                                        updateAppointmentNumber(
                                            updatedAppointment.appointmentId,
                                            newAppointmentNumber
                                        )

                                        // Schedule a notification with the new time
                                        scheduleNotification(
                                            "Appointment Rescheduled",
                                            "Your appointment has been rescheduled to $newFormattedTime",
                                            appointmentId
                                        )
                                    }


                                }
                                val isBooked = dc.document.getBoolean("isBooked") ?: false
                                if (appointment.isBooked != isBooked) {
                                    appointment.isBooked = isBooked
                                    appointmentAdapter.notifyDataSetChanged()
                                }

                            }

                        }

                        DocumentChange.Type.REMOVED -> {
                            // If an appointment is removed, check if it was booked by the current patient
                            val userId = auth.currentUser?.uid ?: return@addSnapshotListener
                            firestore.collection("patients").document(userId).get()
                                .addOnSuccessListener { patientSnapshot ->
                                    val selectedAppointments =
                                        patientSnapshot.get("selected_appointments_ids") as? List<String>
                                            ?: listOf()
                                    if (selectedAppointments.contains(appointmentId)) {
                                        // Remove the deleted appointment ID from the patient's selected appointments
                                        firestore.collection("patients").document(userId)
                                            .update(
                                                "selected_appointments_ids",
                                                FieldValue.arrayRemove(appointmentId)
                                            )
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this,
                                                    "Booked appointment was deleted by the doctor. You can now book a new appointment.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                }
                        }

                        else -> {}
                    }
                }
            }
    }


    private fun scheduleNotification(title: String, message: String, appointmentId: String) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(
                workDataOf(
                    "title" to title,
                    "message" to message,
                    "appointmentId" to appointmentId
                )
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    data class Appointment(
        val appointmentId: String,
        val formattedTime: String,
        var isBooked: Boolean = false,
        var bookedByUserId: String? = null,
        val isPast: Boolean = false  // New field to indicate if the appointment is past
    )


}

