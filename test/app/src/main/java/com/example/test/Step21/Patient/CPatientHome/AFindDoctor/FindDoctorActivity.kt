package com.example.test.Step21.Patient.CPatientHome.AFindDoctor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.example.test.Step21.Patient.CPatientHome.PatientHomeActivity
import com.example.test.Step21.Patient.CPatientHome.AFindDoctor.ADoctorDetailsAndBookAppointments.DoctorDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindDoctorActivity : AppCompatActivity() {

    private lateinit var cPhysician: CardView
    private lateinit var cDermatology: CardView
    private lateinit var cPsychiatry: CardView
    private lateinit var cPediatrics: CardView
    private lateinit var cStomachanddigestion: CardView
    private lateinit var cBack: CardView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        const val EXTRA_TITLE = "title"
        const val TITLE_GENERAL_PHYSICIAN = "General Physician"
        const val TITLE_DERMATOLOGY = "Dermatology"
        const val TITLE_PSYCHIATRY = "Psychiatry"
        const val TITLE_PEDIATRICS = "Pediatrics"
        const val TITLE_STOMACH_AND_DIGESTION = "Stomach and digestion"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_find_doctor)

        cBack = findViewById(R.id.cardFDBack)
        cPhysician = findViewById(R.id.cardFDFamilyPhysician)
        cDermatology = findViewById(R.id.cardFDDermatology)
        cPsychiatry = findViewById(R.id.cardFDPsychiatry)
        cPediatrics = findViewById(R.id.cardFDPediatrics)
        cStomachanddigestion = findViewById(R.id.cardFDStomach)

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


        cBack.setOnClickListener {
            finish()
        }

        cPhysician.setOnClickListener {
            navigateToDoctorDetails(TITLE_GENERAL_PHYSICIAN)
        }

        cDermatology.setOnClickListener {
            navigateToDoctorDetails(TITLE_DERMATOLOGY)
        }

        cPsychiatry.setOnClickListener {
            navigateToDoctorDetails(TITLE_PSYCHIATRY)
        }

        cPediatrics.setOnClickListener {
            navigateToDoctorDetails(TITLE_PEDIATRICS)
        }

        cStomachanddigestion.setOnClickListener {
            navigateToDoctorDetails(TITLE_STOMACH_AND_DIGESTION)
        }
    }

    private fun navigateToDoctorDetails(title: String) {
        val intent = Intent(this@FindDoctorActivity, DoctorDetailsActivity::class.java)
        intent.putExtra(EXTRA_TITLE, title)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
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
            startActivity(Intent(this@FindDoctorActivity, LoginActivity::class.java))
            Toast.makeText(
                this@FindDoctorActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}