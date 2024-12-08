package com.example.test.Step21.Patient.CPatientHome.AFindDoctor.ADoctorDetailsAndBookAppointments

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step21.Patient.CPatientHome.AFindDoctor.FindDoctorActivity
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDetailsActivity : AppCompatActivity() {

    private lateinit var tv: TextView
 //   private lateinit var cDDBack: CardView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var doctorList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_details)

        firestore = FirebaseFirestore.getInstance()
  //      cDDBack = findViewById(R.id.cardDDBack)
        tv = findViewById(R.id.textViewDD)


        setSupportActionBar(findViewById(R.id.toolbar))

//        cDDBack.setOnClickListener {
//            finish()
//        }

        val title = intent.getStringExtra("title")
        tv.text = title

        doctorList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            doctorList,
            R.layout.multi_lines,
            arrayOf("doctor_image", "line1", "line2", "line3", "line4", "line5","line6"),
            intArrayOf(R.id.doctor_image, R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e,R.id.line_f)
        ) {
            override fun setViewImage(view: ImageView, value: String?) {
                if (value.isNullOrEmpty()) {
                    view.setImageResource(R.drawable.icons8_medical_doctor_64)
                } else {
                    Glide.with(this@DoctorDetailsActivity)
                        .load(value)
                        .placeholder(R.drawable.icons8_medical_doctor_64)
                        .into(view)
                }
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val showDoctorsAvailableButton: Button = view.findViewById(R.id.showdoctorsAvailable)
                showDoctorsAvailableButton.setOnClickListener {
                    val doctor = doctorList[position]
                    val intent = Intent(this@DoctorDetailsActivity, DoctorAppointmentsActivity::class.java)
                    intent.putExtra("doctorId", doctor["userId"])
                    startActivity(intent)
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
        val doctorsRef = firestore.collection("doctors")
        doctorsRef.whereArrayContains("specializations", specialization).get()
            .addOnSuccessListener { querySnapshot ->
                doctorList.clear()
                for (document in querySnapshot.documents) {
                    val doctor = document.toObject(Doctor::class.java)
                    doctor?.let {
                        val item = HashMap<String, String>()
                        item["doctor_image"] = it.imageUrl ?: ""
                        item["line1"] = "Doctor Name: ${it.username}"
                        item["line2"] = "Hospital Address: ${it.region}"
                        item["line3"] = "Gender: ${it.gender.joinToString(", ")}"
                        item["line4"] = "Exp: ${it.experience} yrs"
                        item["line5"] = "Mobile No: ${it.phoneNumber}"
                        item["line6"] = "Consultant fees: ${it.fees} Rs/-"
                        item["userId"] = it.userId ?: ""
                        doctorList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle errors
                // Show an error message or perform other appropriate error handling
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
        var specializations: List<String>? = emptyList(),
        var userId: String? = "",
        var username: String? = "",
        var gender: List<String> = emptyList()
    )
}