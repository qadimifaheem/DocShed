package com.example.test.Step22.Doctor.CDoctorHome.GBottomNavigation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment.PatientsDetailsAppionmentsActivity
import com.example.test.Step22.Doctor.CDoctorHome.DoctorHomeActivity
import com.example.test.Step22.Doctor.CDoctorHome.EHistory.CustomDoctorHistoryAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NavigationHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var historyList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: CustomDoctorHistoryAdapter

    private var doctorId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navigation_history)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val sha: SharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        doctorId = intent.getStringExtra("doctorId") ?: return

        // Check if the doctor is active
        doctorId?.let {
            checkPatientStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }



        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_History
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {

                R.id.navigation_History -> {
                // Navigate to Calls fragment/activity
                val doctorId = sha.getString("doctorId", null)
                if (doctorId != null) {
                    val intent =
                        Intent(this@NavigationHistoryActivity,NavigationHistoryActivity::class.java)
                    intent.putExtra("doctorId", doctorId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
                }
                true
            }

                R.id.navigation_Home -> {
                    // Navigate to Chats fragment/activity
                    val doctorId = sha.getString("doctorId", null)
                    if (doctorId != null) {
                        val intent =
                            Intent(this@NavigationHistoryActivity, DoctorHomeActivity::class.java)
                        intent.putExtra("doctorId", doctorId)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        setupListView()
        fetchDoctorHistory(doctorId!!)

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun setupListView() {
        historyList = ArrayList()
        adapter = CustomDoctorHistoryAdapter(this, historyList)
        val listView: ListView = findViewById(R.id.listViewHistory)
        listView.adapter = adapter
    }

    private fun fetchDoctorHistory(doctorId: String) {
        val doctorHistoryRef = firestore.collection("doctors").document(doctorId).collection("history")

        doctorHistoryRef.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                showToast("Error fetching history: ${exception.message}")
                return@addSnapshotListener
            }

            historyList.clear()
            for (document in querySnapshot?.documents ?: listOf()) {
                val historyData = document.data
                if (historyData != null) {
                    val patientName = historyData["patient_name"] as? String ?: "N/A"
                    val formattedTime = historyData["formatted_time"] as? String ?: "N/A"
                    val patientEmail = historyData["patient_email"] as? String ?: "N/A"
                    val appointmentNumber = historyData["appointment_number"] as? String ?: "N/A"
                    val patientAge = historyData["patient_age"]?.toString() ?: "N/A"
                    val patientPhoneNumber = historyData["patient_phone_number"] as? String ?: "N/A"
                    val patientGender = (historyData["patient_gender"] as? List<String>)?.joinToString(", ") ?: "N/A"
                    val status = historyData["status"] as? String ?: "N/A"

                    val item = HashMap<String, String>().apply {
                        put("patient_name", patientName)
                        put("formatted_time", formattedTime)
                        put("appointment_number", appointmentNumber)
                        put("patient_email", patientEmail)
                        put("patient_age", patientAge)
                        put("patient_phone_number", patientPhoneNumber)
                        put("patient_gender", patientGender)
                        put("status", status)
                    }

                    historyList.add(item)
                }
            }
            if (historyList.isEmpty()) {
                showToast("No history found")
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    // Function to check Doctors's account status
    private fun checkPatientStatus(doctorId: String) {
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
            startActivity(Intent(this@NavigationHistoryActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@NavigationHistoryActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}