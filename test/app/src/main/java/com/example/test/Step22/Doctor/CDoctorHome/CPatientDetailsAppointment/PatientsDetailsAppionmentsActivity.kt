package com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientsDetailsAppionmentsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var appointmentList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter
    private var doctorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patients_details_appionments)

        firestore = FirebaseFirestore.getInstance()

        // Get doctor ID from intent
        doctorId = intent.getStringExtra("doctorId") ?: return


        // Check if the doctor is active
        doctorId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }
        setSupportActionBar(findViewById(R.id.toolbar))


        // Initialize the appointment list and adapter
        appointmentList = ArrayList()
        adapter = SimpleAdapter(
            this,
            appointmentList,
            R.layout.patientinfo,
            arrayOf("appointment_number", "formatted_time"),
            intArrayOf(R.id.appointmentLabel, R.id.line_1)
        )

        val listView: ListView = findViewById(R.id.appionmentList)
        listView.adapter = adapter

        // Fetch the selected appointments
        fetchSelectedAppointments()

        // Start the NotificationService with doctorId
        val serviceIntent = Intent(this, NotificationService::class.java)
        serviceIntent.putExtra("doctorId", doctorId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // For Android Oreo and above
        } else {
            startService(serviceIntent) // For older versions
        }

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchSelectedAppointments() {
        val selectedAppointmentsRef = firestore.collection("doctors")
            .document(doctorId ?: return)
            .collection("selected_appointments")

        selectedAppointmentsRef.get()
            .addOnSuccessListener { snapshot ->
                appointmentList.clear()
                for (document in snapshot.documents) {
                    val data = document.data
                    if (data != null) {
                        val item = HashMap<String, String>()
                        item["appointment_number"] = data["appointment_number"] as? String ?: "N/A"
                        item["formatted_time"] = data["formatted_time"] as? String ?: "N/A"
                        item["user_id"] = data["user_id"] as? String ?: "N/A"
                        appointmentList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("PatientsDetailsAppionments", "Error fetching selected appointments: ${exception.message}")
                Toast.makeText(this, "Error fetching appointments", Toast.LENGTH_SHORT).show()
            }
    }

    fun showPatientDetails(view: View) {
        val parent = view.parent as? View ?: return
        val appointmentNumberTextView = parent.findViewById<TextView>(R.id.appointmentLabel)
        val appointmentNumber = appointmentNumberTextView.text.toString()

        val selectedAppointment = appointmentList.find { it["appointment_number"] == appointmentNumber }
        val userId = selectedAppointment?.get("user_id") ?: return
        doctorId = intent.getStringExtra("doctorId") ?: return

        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val patientDetails = document.data
                    if (patientDetails != null) {
                        val detailsList = ArrayList<HashMap<String, String>>()
                        val imageUrl = patientDetails["imageUrl"] as? String
                        val medicalRecordsUrls = patientDetails["medicalRecordsUrls"] as? List<String>

                        for ((key, value) in patientDetails) {
                            if (key == "userId" || key == "selected_appointments_ids"||key == "medicalRecordsUrls" || key == "imageUrl") continue

                            val detailItem = HashMap<String, String>()
                            detailItem["key"] = key
                            detailItem["value"] = value.toString()
                            detailsList.add(detailItem)
                        }

                        // Pass the URLs for images and medical records
                        // Pass the userId along with  data
                        val intent = Intent(this, PatientDetailsActivity::class.java)
                        intent.putExtra("patientDetails", detailsList)
                        intent.putExtra("imageUrl", imageUrl)
                        intent.putExtra("doctorId", doctorId)
                        intent.putExtra("userId", userId) // Passing userId here
                        intent.putStringArrayListExtra("medicalRecordsUrls", ArrayList(medicalRecordsUrls))
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No patient details found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No patient details found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PatientsDetailsAppionments", "Error fetching patient details: ${exception.message}")
                Toast.makeText(this, "Error fetching patient details", Toast.LENGTH_SHORT).show()
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
            startActivity(
                Intent(
                    this@PatientsDetailsAppionmentsActivity,
                    LoginDoctorActivity::class.java
                )
            )
            Toast.makeText(
                this@PatientsDetailsAppionmentsActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }


    }
}