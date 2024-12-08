package com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor.ADoctorsDetails

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor.DoctorListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class AllDoctorsActivity : AppCompatActivity() {

    private lateinit var tv: TextView
  //  private lateinit var cDDBack: CardView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var doctorList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_doctors)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
      //  cDDBack = findViewById(R.id.cardDDBack)
        tv = findViewById(R.id.textViewDD)

        setSupportActionBar(findViewById(R.id.toolbar))

//        cDDBack.setOnClickListener {
//            startActivity(Intent(this@AllDoctorsActivity, DoctorListActivity::class.java))
//            finish()
//        }

        val title = intent.getStringExtra("title")
        tv.text = title

        doctorList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            doctorList,
            R.layout.multi_lines3,
            arrayOf("doctor_image", "line1", "line2", "line3", "line4", "line5","line6","line7","line8","line9","line10"),
            intArrayOf(R.id.doctor_image, R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e,R.id.line_f,R.id.line_g,R.id.line_h,R.id.line_i,R.id.line_j)
        ) {
            override fun setViewImage(view: ImageView, value: String?) {
                if (value.isNullOrEmpty()) {
                    view.setImageResource(R.drawable.icons8_medical_doctor_64)
                } else {
                    Glide.with(this@AllDoctorsActivity)
                        .load(value)
                        .placeholder(R.drawable.icons8_medical_doctor_64)
                        .into(view)
                }
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val disableDoctorsAccountButton: Button = view.findViewById(R.id.DisableDoctorsAccount)
                val enableDoctorsAccountButton: Button = view.findViewById(R.id.EnableDoctorsAccount)
                disableDoctorsAccountButton.setOnClickListener {
                    val doctor = doctorList[position]
                    val doctorId = doctor["userId"]

                    doctorId?.let {
                        disableDoctorAccount(it) // Change to disableDoctorAccount


                    }
                }
                // Enable Account Button Click Listener
                enableDoctorsAccountButton.setOnClickListener {
                    val doctor = doctorList[position]
                    val doctorId = doctor["userId"]

                    doctorId?.let {
                        enableDoctorAccount(it) // Enable account
                    }
                }
                return view
            }

        }

        val listView: ListView = findViewById(R.id.listViewDD)
        listView.adapter = adapter

        fetchDoctorDetails(title ?: "")
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchDoctorDetails(specialization: String) {
        val doctorsRef = firestore.collection("admin").document("history").collection("doctorHistory")
        doctorsRef.whereArrayContains("specializations", specialization).get()
            .addOnSuccessListener { querySnapshot ->
                doctorList.clear()
                for (document in querySnapshot.documents) {
                    val doctor = document.toObject(Doctor::class.java)
                    doctor?.let {
                        val item = HashMap<String, String>()
                        item["doctor_image"] = it.imageUrl ?: ""
                        item["line1"] = "Doctor Name: ${it.username}"
                        item["line2"] = "Email: ${it.email}"
                        item["line3"] = "Mobile No: ${it.phoneNumber}"
                        item["line4"] = "Age: ${it.age}"
                        item["line5"] = "Gender: ${it.gender.joinToString(", ")}"
                        item["line6"] = "Hospital Address: ${it.region}"
                        item["line7"] = "Exp: ${it.experience} yrs"
                        item["line8"] = "Specializations: ${it.specializations.joinToString(", ")}"
                        item["line9"] = "Medical License Number: ${it.medicalLicenseNumber} yrs"
                        item["line10"] = "Consultant fees: ${it.fees} Rs/-"
                        item["userId"] = it.userId ?: ""
                        doctorList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle errors
                Toast.makeText(this, "Error fetching doctors: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun disableDoctorAccount(doctorId: String) {
        // Update the "isActive" field to false in the doctor's document
        firestore.collection("doctors").document(doctorId)
            .update("isActive", false)
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor account disabled successfully!", Toast.LENGTH_SHORT).show()
                fetchDoctorDetails(tv.text.toString()) // Refresh list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error disabling doctor account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enableDoctorAccount(doctorId: String) {
        // Update the "isActive" field to true in the doctor's document
        firestore.collection("doctors").document(doctorId)
            .update("isActive", true)
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor account enabled successfully!", Toast.LENGTH_SHORT).show()
                fetchDoctorDetails(tv.text.toString()) // Refresh list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error enabling doctor account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    data class Doctor(
        var email: String? = "",
        var experience: String? = "",
        var fees: String? = "",
        var medicalLicenseNumber: String? = "",
        var phoneNumber: String? = "",
        var imageUrl: String? = "",
        var region: String? = "",
        var specializations: List<String> = emptyList(),
        var userId: String? = "",
        var username: String? = "",
        val age: String? = "",
        var gender: List<String> = emptyList()
    )
}