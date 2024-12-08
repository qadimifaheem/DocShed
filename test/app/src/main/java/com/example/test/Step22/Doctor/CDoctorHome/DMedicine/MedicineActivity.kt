package com.example.test.Step22.Doctor.CDoctorHome.DMedicine

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MedicineActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var appointmentList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: AppointmentAdapter
    private var doctorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine)

        // Get doctor ID from intent
        doctorId = intent.getStringExtra("doctorId") ?: return


        // Check if the doctor is active
        doctorId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }

        setSupportActionBar(findViewById(R.id.toolbar))

        firestore = FirebaseFirestore.getInstance()
        setupAppointmentList()
        fetchAppointments()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun setupAppointmentList() {
        appointmentList = ArrayList()
        adapter = AppointmentAdapter(appointmentList)
        val listView: ListView = findViewById(R.id.listViewAppointments)
        listView.adapter = adapter
    }

    private fun fetchAppointments() {
        val appointmentsRef = firestore.collection("doctors")
            .document(doctorId!!)
            .collection("selected_appointments")

        appointmentsRef.orderBy("appointment_time", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e("DoctorAppointments", "Error fetching appointments: ${exception.message}")
                    showToast("Error fetching appointments")
                    return@addSnapshotListener
                }

                querySnapshot?.let {
                    appointmentList.clear()
                    for (document in it.documents) {
                        val appointment = document.data
                        appointment?.let { data ->
                            val formattedTime = data["formatted_time"]?.toString() ?: "Unknown"
                            val userId = data["user_id"]?.toString() ?: "Unknown"
                            val doctorName = data["doctor_name"]?.toString() ?: "Unknown"
                            val appointmentId = data["appointment_id"]?.toString() ?: "Unknown" // Fetch appointment_id
                            val appointmentNumber = data["appointment_number"]?.toString() ?: "Unknown" // Fetch appointment_number

                            // Fetch patient name from the patients collection
                            fetchPatientName(userId) { patientName ->
                                val item = HashMap<String, String>().apply {
                                    put("patient_name", patientName)
                                    put("formatted_time", formattedTime)
                                    put("appointment_number", appointmentNumber) // Use the fetched appointment_number
                                    put("user_id", userId)
                                    put("doctor_id", doctorId ?: "Unknown")
                                    put("doctor_name", doctorName)
                                    put("appointment_id", appointmentId) // Add appointment_id
                                }
                                appointmentList.add(item)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
    }



    private fun fetchPatientName(userId: String, callback: (String) -> Unit) {
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                val patientName = document.getString("username") ?: "Unknown"
                callback(patientName)
            }
            .addOnFailureListener { e ->
                Log.e("FetchPatientName", "Error fetching patient name: ${e.message}")
                callback("Unknown")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }





    // for absent patient and save alarm

    inner class AppointmentAdapter(private val data: ArrayList<HashMap<String, String>>) :
        SimpleAdapter(
            this@MedicineActivity,
            data,
            R.layout.patient_list_item,
            arrayOf("patient_name", "formatted_time", "appointment_number"),
            intArrayOf(R.id.patientName, R.id.formattedTime, R.id.appointmentNumber)
        ) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val buttonSetAlarm: Button = view.findViewById(R.id.setalarm)
            val buttonAbsent: Button = view.findViewById(R.id.absent) // Find the absent button

            buttonSetAlarm.setOnClickListener {
                val userId = data[position]["user_id"]
                val doctorId = data[position]["doctor_id"]
                val doctorName = data[position]["doctor_name"]
                val appointmentId = data[position]["appointment_id"]
                val intent = Intent(this@MedicineActivity, SetAlarmActivity::class.java).apply {
                    putExtra("user_id", userId)
                    putExtra("doctor_id", doctorId)
                    putExtra("doctor_name", doctorName)
                    putExtra("appointment_id", appointmentId) // Pass appointment_id to SetAlarmActivity
                }
                startActivity(intent)
            }

            buttonAbsent.setOnClickListener {
                val userId = data[position]["user_id"]
                val appointmentId = data[position]["appointment_id"]
                if (userId != null && appointmentId != null) {
                    handlePatientAbsence(userId, appointmentId)
                } else {
                    showToast("Invalid patient or appointment details")
                }
            }

            return view
        }
    }
    private fun handlePatientAbsence(userId: String, appointmentId: String) {
        firestore.runTransaction { transaction ->
            // Reference to the history document for this appointment
            val historyRef = firestore.collection("patients")
                .document(userId)
                .collection("history")
                .document(appointmentId)

            // Fetch existing history data
            val historyDocument = transaction.get(historyRef)
            val historyData = historyDocument.data?.toMutableMap() ?: mutableMapOf()

            // Fetch patient details (name and phone number) from the patients collection
            val patientRef = firestore.collection("patients").document(userId)
            val patientDocument = transaction.get(patientRef)

            if (patientDocument.exists()) {
                // Add patient details to history
                val patientName = patientDocument.getString("username") ?: "Unknown"
                val patientPhoneNumber = patientDocument.getString("phoneNumber") ?: "Unknown"

                // Update the history document with the status, patient name, and phone number
                historyData["status"] = "Patient absent"
                historyData["patient_name"] = patientName
                historyData["patient_phone_number"] = patientPhoneNumber

                transaction.set(historyRef, historyData) // Update the history document
            }
            // Reference to the appointment in the doctor's history collection
            val doctorHistoryRef = firestore.collection("doctors")
                .document(doctorId!!) // Use doctorId!! here
                .collection("history")
                .document(appointmentId)

            // Update the status in the doctor's history
            transaction.update(doctorHistoryRef, "status", "Patient absent")



            // Remove the appointment from the doctor's selected_appointments collection
            val selectedAppointmentRef = firestore.collection("doctors")
                .document(doctorId!!)
                .collection("selected_appointments")
                .document(appointmentId)
            transaction.delete(selectedAppointmentRef)

            // Remove the appointment from the doctor's appointments collection
            val appointmentRef = firestore.collection("doctors")
                .document(doctorId!!)
                .collection("appointments")
                .document(appointmentId)
            transaction.delete(appointmentRef)

            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Patient marked as absent and appointment removed", Toast.LENGTH_SHORT).show()
            fetchAppointments() // Refresh the list
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error marking patient as absent: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


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
                Toast.makeText(this, "Error checking account status: ${e.message}", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this@MedicineActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@MedicineActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }

}