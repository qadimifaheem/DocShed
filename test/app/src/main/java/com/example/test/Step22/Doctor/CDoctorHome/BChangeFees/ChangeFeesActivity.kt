package com.example.test.Step22.Doctor.CDoctorHome.BChangeFees

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChangeFeesActivity : AppCompatActivity() {
    private lateinit var currentcharge: TextView
    private lateinit var newfees: EditText
    private lateinit var cnewfees: EditText
    private lateinit var changebutton: Button


    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_fees)

        currentcharge = findViewById(R.id.currentfees)
        newfees = findViewById(R.id.NewFees)
        cnewfees = findViewById(R.id.ConfirmNewFees)
        changebutton = findViewById(R.id.changeFeesButton)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get the current user's ID
        val userId = auth.currentUser?.uid


        // Check if the doctor is active
        userId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }
        setSupportActionBar(findViewById(R.id.toolbar))
        if (userId != null) {
            // Fetch the doctor's details from Firestore
            firestore.collection("doctors").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fees = document.getString("fees")
                        currentcharge.text = "Rs ${fees ?: "No fees available"}"
                    } else {
                        currentcharge.text = "No data found"
                    }
                }
                .addOnFailureListener { e ->
                    currentcharge.text = "Error fetching data: ${e.message}"
                }
        } else {
            currentcharge.text = "User not logged in"
        }

        // Change fees button functionality
        changebutton.setOnClickListener {
            val newFeesValue = newfees.text.toString().trim()
            val confirmNewFeesValue = cnewfees.text.toString().trim()

            if (newFeesValue.isEmpty() || confirmNewFeesValue.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newFeesValue != confirmNewFeesValue) {
                Toast.makeText(this, "New fees do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the current user's ID again
            val userId = auth.currentUser?.uid

            if (userId != null) {
                // Get current doctor's details
                firestore.collection("doctors").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Extract the doctor's details
                            val doctorDetails = mapOf(
                                "userId" to userId,
                                "email" to document.getString("email"),
                                "username" to document.getString("username"),
                                "medicalLicenseNumber" to document.getString("medicalLicenseNumber"),
                                "experience" to document.getString("experience"),
                                "region" to document.getString("region"),
                                "phoneNumber" to document.getString("phoneNumber"),
                                "currentFees" to document.getString("fees"),
                                "requestedFees" to newFeesValue,
                                "age" to document.getString("age"),
                                "specializations" to document.get("specializations"),
                                "gender" to document.get("gender"),
                                "imageUrl" to document.getString("imageUrl")
                            )

                            // Store the change fee request in the admin collection
                            firestore.collection("admin").document("doctorChangeFees")
                                .collection("fees").document(userId).set(doctorDetails)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Fee change request submitted for approval",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Error submitting request: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Error fetching doctor details",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
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
            startActivity(Intent(this@ChangeFeesActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@ChangeFeesActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}
