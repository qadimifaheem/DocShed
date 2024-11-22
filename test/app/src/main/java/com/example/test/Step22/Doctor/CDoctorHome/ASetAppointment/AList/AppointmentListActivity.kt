package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AList

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import androidx.activity.enableEdgeToEdge
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.AppointmentAdapterA
import com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment.SetAppiomentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
   // private lateinit var back : CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment_list)
        setSupportActionBar(findViewById(R.id.toolbar))
   //     back = findViewById(R.id.Back)
        listView = findViewById(R.id.Appiomentlist)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchAppointments()


        // for back button
//        back.setOnClickListener {
//            startActivity(Intent(this@AppointmentListActivity, SetAppiomentActivity::class.java))
//        }

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }



    private fun fetchAppointments() {
        val user = auth.currentUser
        user?.let {
            val doctorId = it.uid
            firestore.collection("doctors").document(doctorId)
                .collection("appointments")
                .orderBy("appointment_time") // Ensure ordering is consistent
                .get()
                .addOnSuccessListener { documents ->
                    val appointments = documents.map { document ->
                        val formattedTime = document.getString("formatted_time") ?: "No time"
                        formattedTime
                    }
                    populateListView(appointments)
                }
                .addOnFailureListener { e ->
                    // Handle failure
                }
        }
    }

    private fun populateListView(appointments: List<String>) {
        val adapter = AppointmentAdapterA(this, appointments)
        listView.adapter = adapter
    }
}