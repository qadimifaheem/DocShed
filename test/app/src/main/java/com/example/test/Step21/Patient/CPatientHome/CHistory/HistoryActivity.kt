package com.example.test.Step21.Patient.CPatientHome.CHistory


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var historyList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: CustomHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid ?: return
        // Check if the patient is active
        userId?.let {
            checkPatientStatus(it)
        } ?: run {
            redirectToLogin() // No patientId means the patient is not logged in
        }

        setSupportActionBar(findViewById(R.id.toolbar))


        setupListView()
        fetchPatientHistory()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }


    private fun setupListView() {
        historyList = ArrayList()
        adapter = CustomHistoryAdapter(
            this,
            historyList
        )

        val listView: ListView = findViewById(R.id.listViewHistory)
        listView.adapter = adapter
    }

    private fun fetchPatientHistory() {
        val userId = auth.currentUser?.uid ?: return
        val patientHistoryRef = firestore.collection("patients").document(userId).collection("history")

        patientHistoryRef.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                showToast("Error fetching history: ${exception.message}")
                return@addSnapshotListener
            }

            historyList.clear()
            for (document in querySnapshot?.documents ?: listOf()) {
                val historyData = document.data
                if (historyData != null) {
                    val formatted_time = historyData["formatted_time"] as? String ?: "N/A"
                    val doctorName = historyData["doctor_name"] as? String ?: "N/A"
                    val status = historyData["status"] as? String ?: "N/A"
                    val specializations = (historyData["doctor_specializations"] as? List<String>)?.joinToString(", ") ?: "N/A"
                    val item = HashMap<String, String>().apply {
                        put("doctor_name", doctorName)
                        put("formatted_time", formatted_time)
                        put("specializations", specializations)
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
            startActivity(Intent(this@HistoryActivity, LoginActivity::class.java))
            Toast.makeText(
                this@HistoryActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}