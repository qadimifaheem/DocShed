package com.example.test.Step21.Patient.CPatientHome


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.example.test.Step21.Patient.CPatientHome.AFindDoctor.ADoctorDetailsAndBookAppointments.DoctorDetailsActivity
import com.example.test.Step21.Patient.CPatientHome.AFindDoctor.FindDoctorActivity
import com.example.test.Step21.Patient.CPatientHome.BAppointmentsBooked.AppointmentBookedActivity
import com.example.test.Step21.Patient.CPatientHome.CHistory.HistoryActivity
import com.example.test.Step21.Patient.CPatientHome.DMedicine.PatientMedicineAlarmActivity
import com.example.test.Step21.Patient.CPatientHome.ESetting.PatientsSettingActivity
import com.example.test.Step21.Patient.CPatientHome.FBottomNavigation.NavigationAppointmentBookedActivity
import com.example.test.Step21.Patient.CPatientHome.FBottomNavigation.NavigtionHistory2Activity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class PatientHomeActivity : AppCompatActivity() {


    private lateinit var cPhysician: CardView
    private lateinit var cDermatology: CardView
    private lateinit var cPsychiatry: CardView
    private lateinit var cPediatrics: CardView
    private lateinit var cStomachanddigestion: CardView

    private lateinit var cSetting: CardView
    private lateinit var cMedicine: CardView
    private lateinit var cFindDoctor: CardView
    private lateinit var cAppointments: CardView
    private lateinit var cHistory: CardView
    private lateinit var cLogOut: CardView

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
        setContentView(R.layout.activity_patient_home)

        ShowGIF()
        cSetting = findViewById(R.id.cardSetting)
        cMedicine = findViewById(R.id.Medicine)
        cHistory = findViewById(R.id.history)
        cFindDoctor = findViewById(R.id.cardFindDoctor)
        cAppointments = findViewById(R.id.Appointments)
        cLogOut = findViewById(R.id.Logout)

        cPhysician = findViewById(R.id.cardFDFamilyPhysician)
        cDermatology = findViewById(R.id.cardFDDermatology)
        cPsychiatry = findViewById(R.id.cardFDPsychiatry)
        cPediatrics = findViewById(R.id.cardFDPediatrics)
        cStomachanddigestion = findViewById(R.id.cardFDStomach)

        val sh: SharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        val userId = sh.getString("username", null)


        // Check if the patient is active
        userId?.let {
            checkPatientStatus(it)
        } ?: run {
            redirectToLogin() // No patientId means the patient is not logged in
        }



        setSupportActionBar(findViewById(R.id.toolbar))


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_Home
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_Home -> {
                    // Navigate to Chats fragment/activity
                    startActivity(Intent(this@PatientHomeActivity, this@PatientHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_Appointments -> {
                    // Navigate to Calls fragment/activity
                    startActivity(Intent(this@PatientHomeActivity, NavigationAppointmentBookedActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_History -> {
                    // Navigate to Calls fragment/activity
                    startActivity(Intent(this@PatientHomeActivity, NavigtionHistory2Activity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }



        cPhysician.setOnClickListener {
            navigateToDoctorDetails(FindDoctorActivity.TITLE_GENERAL_PHYSICIAN)
        }

        cDermatology.setOnClickListener {
            navigateToDoctorDetails(FindDoctorActivity.TITLE_DERMATOLOGY)
        }

        cPsychiatry.setOnClickListener {
            navigateToDoctorDetails(FindDoctorActivity.TITLE_PSYCHIATRY)
        }

        cPediatrics.setOnClickListener {
            navigateToDoctorDetails(FindDoctorActivity.TITLE_PEDIATRICS)
        }

        cStomachanddigestion.setOnClickListener {
            navigateToDoctorDetails(FindDoctorActivity.TITLE_STOMACH_AND_DIGESTION)
        }

        //for patients settings
        cSetting.setOnClickListener {
            startActivity(Intent(this@PatientHomeActivity, PatientsSettingActivity::class.java))
        }
        //for alarm and medicine
        cMedicine.setOnClickListener {
            startActivity(Intent(this@PatientHomeActivity, PatientMedicineAlarmActivity::class.java))
        }


        //for history of patients
        cHistory.setOnClickListener {
            startActivity(Intent(this@PatientHomeActivity, HistoryActivity::class.java))
        }


        // for book an appointments
        cFindDoctor.setOnClickListener {
            startActivity(Intent(this@PatientHomeActivity, FindDoctorActivity::class.java))
        }

        //for selected appointments list

        cAppointments.setOnClickListener {

            startActivity(Intent(this@PatientHomeActivity, AppointmentBookedActivity::class.java))
        }

        cLogOut.setOnClickListener {
            val editor: SharedPreferences.Editor = sh.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@PatientHomeActivity, LoginActivity::class.java))
            Toast.makeText(this@PatientHomeActivity, "Logout successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun ShowGIF()
    {
        val GIF : ImageView = findViewById(R.id.GIF)
        Glide.with(this).load(R.drawable.gif1).into(GIF)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val sh: SharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                val userId = sh.getString("username", null)

                if (userId != null) {
                    val intent = Intent(this@PatientHomeActivity, PatientsSettingActivity::class.java)
                    intent.putExtra("username", userId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_patient_home, menu)
        return true
    }

    private fun navigateToDoctorDetails(title: String) {
        val intent = Intent(this@PatientHomeActivity, DoctorDetailsActivity::class.java)
        intent.putExtra(EXTRA_TITLE, title)
        startActivity(intent)
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
            startActivity(Intent(this@PatientHomeActivity, LoginActivity::class.java))
            Toast.makeText(
                this@PatientHomeActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }

}