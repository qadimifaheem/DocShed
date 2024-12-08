package com.example.test.Step20.Admin.CAdminHome.DList.AChoice.BPatient


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step20.Admin.CAdminHome.AdminHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class PatientListActivity : AppCompatActivity() {



    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var patientList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_list)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setSupportActionBar(findViewById(R.id.toolbar))


        patientList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            patientList,
            R.layout.multi_lines4, // Use an appropriate layout
            arrayOf("patient_image", "line1", "line2", "line3", "line4", "line5"),
            intArrayOf(R.id.Patients_image, R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e)
        ) {
            override fun setViewImage(view: ImageView, value: String?) {
                if (value.isNullOrEmpty()) {
                    view.setImageResource(R.drawable.noimage1) // Provide a default image
                } else {
                    Glide.with(this@PatientListActivity)
                        .load(value)
                        .placeholder(R.drawable.noimage1)
                        .into(view)
                }
            }


            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val disablePatientAccountButton: Button = view.findViewById(R.id.DisablePatientsAccount)
                val enablePatientAccountButton: Button = view.findViewById(R.id.EnablePatientsAccount)

                disablePatientAccountButton.setOnClickListener {
                    val patient = patientList[position]
                    val patientId = patient["userId"]

                    patientId?.let {
                        disablePatientAccount(it)
                    }
                }

                enablePatientAccountButton.setOnClickListener {
                    val patient = patientList[position]
                    val patientId = patient["userId"]

                    patientId?.let {
                        enablePatientAccount(it)
                    }
                }
                return view
            }
        }

        val listView: ListView = findViewById(R.id.listViewDD)
        listView.adapter = adapter

        fetchPatientDetails()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchPatientDetails() {
        firestore.collection("patients").get()
            .addOnSuccessListener { querySnapshot ->
                patientList.clear()
                for (document in querySnapshot.documents) {
                    val patient = document.toObject(Patient::class.java)
                    patient?.let {
                        val item = HashMap<String, String>()
                        item["patient_image"] = it.imageUrl ?: ""
                        item["line1"] = "Patient Name: ${it.username}"
                        item["line2"] = "Email: ${it.email}"
                        item["line3"] = "Mobile No: ${it.phoneNumber}"
                        item["line4"] = "Age: ${it.age}"
                        item["line5"] = "Gender: ${it.selectedGender.joinToString(", ")}"
                        item["userId"] = it.userId ?: ""
                        patientList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching patients: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun disablePatientAccount(patientId: String) {
        firestore.collection("patients").document(patientId)
            .update("isActive", false)
            .addOnSuccessListener {
                Toast.makeText(this, "Patient account disabled successfully!", Toast.LENGTH_SHORT).show()
                fetchPatientDetails() // Refresh list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error disabling patient account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enablePatientAccount(patientId: String) {
        firestore.collection("patients").document(patientId)
            .update("isActive", true)
            .addOnSuccessListener {
                Toast.makeText(this, "Patient account enabled successfully!", Toast.LENGTH_SHORT).show()
                fetchPatientDetails() // Refresh list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error enabling patient account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Patient(
        var email: String? = "",
        var phoneNumber: String? = "",
        var imageUrl: String? = "",
        var userId: String? = "",
        var username: String? = "",
        var age: String? = "",
        var selectedGender: List<String> = emptyList(),
    )
}