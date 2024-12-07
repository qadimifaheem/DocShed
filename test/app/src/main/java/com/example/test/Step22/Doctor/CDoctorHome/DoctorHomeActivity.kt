package com.example.test.Step22.Doctor.CDoctorHome;


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
import com.example.test.Step22.Doctor.CDoctorHome.BChangeFees.ChangeFeesActivity
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AList.AppointmentListActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.BDelete.DeleteAppointmentListActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.CManage.ManageActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.DSetAppoint.SetDateAndTimeActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.SetAppiomentActivity
import com.example.test.Step22.Doctor.CDoctorHome.DMedicine.MedicineActivity
import com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment.PatientsDetailsAppionmentsActivity
import com.example.test.Step22.Doctor.CDoctorHome.EHistory.DoctorHistoryActivity
import com.example.test.Step22.Doctor.CDoctorHome.FSetting.DoctorSettingActivity
import com.example.test.Step22.Doctor.CDoctorHome.GBottomNavigation.NavigationHistoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class DoctorHomeActivity : AppCompatActivity() {
    private lateinit var setAppioment: CardView
    private lateinit var changeFees: CardView
    private lateinit var showPatient: CardView
    private lateinit var Medicine: CardView
    private lateinit var History: CardView


    private lateinit var showList: CardView
    private lateinit var setDnT: CardView
    private lateinit var dltAppio: CardView
    private lateinit var scheduleAppointments: CardView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // private lateinit var setting: CardView
    private lateinit var dlogout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_home)
        ShowGIF()
        val sha: SharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        val doctorId = sha.getString("doctorId", null)


        setAppioment = findViewById(R.id.DoctorHomeSetAppioment)
        changeFees = findViewById(R.id.DoctorHomeChangeFees)
        showPatient = findViewById(R.id.DoctorHomeShowPatientDetails)
        Medicine = findViewById(R.id.DoctorHomeMedicine)
        History = findViewById(R.id.DoctorHomeHistory)
        dlogout = findViewById(R.id.Logout)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        showList = findViewById(R.id.Showappiomentlist)
        setDnT = findViewById(R.id.SetTimeAndDates)
        dltAppio = findViewById(R.id.DeleteAppioment)
        scheduleAppointments = findViewById(R.id.SheduleAppionments)

        // Check if the doctor is active
        doctorId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }

        setSupportActionBar(findViewById(R.id.toolbar))


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_Home
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_Home -> {
                    // Navigate to Chats fragment/activity
                    val doctorId = sha.getString("doctorId", null)
                    if (doctorId != null) {
                        val intent =
                            Intent(this@DoctorHomeActivity, this::class.java)
                        intent.putExtra("doctorId", doctorId)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                R.id.navigation_History -> {
                    // Navigate to Calls fragment/activity
                    val doctorId = sha.getString("doctorId", null)
                    if (doctorId != null) {
                        val intent =
                            Intent(this@DoctorHomeActivity, NavigationHistoryActivity::class.java)
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

        // Show list of appointments
        showList.setOnClickListener {
            startActivity(Intent(this@DoctorHomeActivity, AppointmentListActivity::class.java))
        }

        // Set Date and Time button
        setDnT.setOnClickListener {
            startActivity(Intent(this@DoctorHomeActivity, SetDateAndTimeActivity::class.java))
        }

        // Delete Appointments button
        dltAppio.setOnClickListener {
            startActivity(Intent(this@DoctorHomeActivity, DeleteAppointmentListActivity::class.java))
        }

        // Schedule Appointments button
        scheduleAppointments.setOnClickListener {

            startActivity(Intent(this@DoctorHomeActivity, ManageActivity::class.java))
        }

        // Logout button
        dlogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Sign out from Firebase Authentication
            val editor: SharedPreferences.Editor = sha.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@DoctorHomeActivity, LoginDoctorActivity::class.java))
            Toast.makeText(this@DoctorHomeActivity, "Logout successfully", Toast.LENGTH_SHORT)
                .show()
            finish()
        }

        //Show patient button
        History.setOnClickListener {
            val doctorId = sha.getString("doctorId", null)
            if (doctorId != null) {
                val intent =
                    Intent(this@DoctorHomeActivity, DoctorHistoryActivity::class.java)
                intent.putExtra("doctorId", doctorId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
            }
        }

        //Show patient button
        showPatient.setOnClickListener {
            val doctorId = sha.getString("doctorId", null)
            if (doctorId != null) {
                val intent =
                    Intent(this@DoctorHomeActivity, PatientsDetailsAppionmentsActivity::class.java)
                intent.putExtra("doctorId", doctorId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Change fees button
        changeFees.setOnClickListener {
            startActivity(Intent(this@DoctorHomeActivity, ChangeFeesActivity::class.java))
        }

        // Set appointment button
        setAppioment.setOnClickListener {
            startActivity(Intent(this@DoctorHomeActivity, SetAppiomentActivity::class.java))
        }

        //for patients medicine
        Medicine.setOnClickListener {
            val doctorId = sha.getString("doctorId", null)
            if (doctorId != null) {
                val intent = Intent(this@DoctorHomeActivity, MedicineActivity::class.java)
                intent.putExtra("doctorId", doctorId)
                startActivity(intent)
            }


        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val sha: SharedPreferences =
                    getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                // Handle settings click
                val doctorId = sha.getString("doctorId", null)
                if (doctorId != null) {
                    val intent = Intent(this@DoctorHomeActivity, DoctorSettingActivity::class.java)
                    intent.putExtra("doctorId", doctorId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Doctor ID not found", Toast.LENGTH_SHORT).show()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_home, menu)
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

    private fun ShowGIF() {
        val GIF: ImageView = findViewById(R.id.GIF)
        Glide.with(this).load(R.drawable.gif1).into(GIF)
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
            startActivity(Intent(this@DoctorHomeActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@DoctorHomeActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}