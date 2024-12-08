package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.content.SharedPreferences
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AList.AppointmentListActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.BDelete.DeleteAppointmentListActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.CManage.ManageActivity
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.DSetAppoint.SetDateAndTimeActivity
import com.example.test.Step22.Doctor.CDoctorHome.DoctorHomeActivity

class SetAppiomentActivity : AppCompatActivity() {
 //   private lateinit var setBack: CardView
    private lateinit var showList: CardView
    private lateinit var setDnT: CardView
    private lateinit var dltAppio: CardView
    private lateinit var scheduleAppointments: CardView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_set_appioment)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        showList = findViewById(R.id.Showappiomentlist)
        setDnT = findViewById(R.id.SetTimeAndDates)
        dltAppio = findViewById(R.id.DeleteAppioment)
        scheduleAppointments = findViewById(R.id.SheduleAppionments)

        val userId = auth.currentUser?.uid
        setSupportActionBar(findViewById(R.id.toolbar))

        // Check if the doctor is active
        userId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }

        // Show list of appointments
        showList.setOnClickListener {
            startActivity(Intent(this@SetAppiomentActivity, AppointmentListActivity::class.java))
        }

        // Set Date and Time button
        setDnT.setOnClickListener {
            startActivity(Intent(this@SetAppiomentActivity, SetDateAndTimeActivity::class.java))
        }

        // Delete Appointments button
        dltAppio.setOnClickListener {
            startActivity(Intent(this@SetAppiomentActivity, DeleteAppointmentListActivity::class.java))
        }

        // Schedule Appointments button
        scheduleAppointments.setOnClickListener {

            startActivity(Intent(this@SetAppiomentActivity, ManageActivity::class.java))
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    //if disabled account by admin
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
            startActivity(Intent(this@SetAppiomentActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@SetAppiomentActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}



