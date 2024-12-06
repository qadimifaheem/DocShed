package com.example.test.Step20.Admin.CAdminHome.ARequest

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
import com.bumptech.glide.Glide
import com.example.test.R
import com.google.firebase.firestore.FirebaseFirestore

class RequestActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var doctorRequestList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_request)

        firestore = FirebaseFirestore.getInstance()
        doctorRequestList = ArrayList()
        setSupportActionBar(findViewById(R.id.toolbar))
        // Set up the adapter
        adapter = object : SimpleAdapter(
            this,
            doctorRequestList,
            R.layout.item_doctor_request,  // Custom layout
            arrayOf("doctor_image", "line1", "line2", "line3", "line4", "line5", "line6", "line7", "line8", "line9", "line10","line11"),
            intArrayOf(R.id.doctor_image, R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e, R.id.line_f, R.id.line_g, R.id.line_h, R.id.line_i, R.id.line_j, R.id.li)
        ) {
            override fun setViewImage(view: ImageView, value: String?) {
                if (value.isNullOrEmpty()) {
                    view.setImageResource(R.drawable.icons8_medical_doctor_64)
                } else {
                    Glide.with(this@RequestActivity)
                        .load(value)
                        .placeholder(R.drawable.icons8_medical_doctor_64)
                        .into(view)
                }
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val approveButton: Button = view.findViewById(R.id.approveButton)

                approveButton.setOnClickListener {
                    val doctor = doctorRequestList[position]
                    val doctorId = doctor["userId"] ?: return@setOnClickListener

                    // Create a Doctor object with raw data (no prefixes)
                    val doctorDetails = Doctor(
                        userId = doctor["userId"] ?: "",
                        upiId = doctor["upiId"] ?: "",
                        email = doctor["line10"]?.removePrefix("Email: ") ?: "",
                        username = doctor["line1"]?.removePrefix("Doctor name: ") ?: "",
                        medicalLicenseNumber = doctor["line2"]?.removePrefix("License No: ") ?: "",
                        experience = doctor["line3"]?.removePrefix("Experience: ") ?: "",
                        region = doctor["line4"]?.removePrefix("Region: ") ?: "",
                        phoneNumber = doctor["line5"]?.removePrefix("Phone: ") ?: "",
                        fees = doctor["line6"]?.removePrefix("Fees: ") ?: "",
                        age = doctor["line7"]?.removePrefix("Age: ") ?: "",
                        specializations = doctor["line8"]?.removePrefix("Specializations: ")?.split(", ") ?: emptyList(),
                        gender = doctor["line9"]?.removePrefix("Gender: ")?.split(", ") ?: emptyList(),
                        imageUrl = doctor["doctor_image"] ?: "",
                        medicalLicenseUrl = doctor["line11"] ?: ""

                    )

                    // Save the doctor details to the 'doctors' collection
                    saveDoctorToFirestore(doctorId, doctorDetails)

                    // Save the doctor details to the 'history' collection under 'admin'
                    saveToHistory(doctorId, doctorDetails)

                    // Update the status to "approved"
                    updateRequestStatus(doctorId)

                    // Remove the doctor request from the requests collection
                    removeDoctorRequest(doctorId)
                }


                return view
            }
        }

        val listView: ListView = findViewById(R.id.DoctorRequestListView)
        listView.adapter = adapter

        // Fetch doctor request details from Firestore
        fetchDoctorRequests()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun fetchDoctorRequests() {
        val doctorRequestsRef = firestore.collection("admin").document("doctorRequests").collection("requests")
        doctorRequestsRef.get()
            .addOnSuccessListener { querySnapshot ->
                doctorRequestList.clear()
                for (document in querySnapshot.documents) {
                    val doctor = document.toObject(Doctor::class.java)
                    doctor?.let {
                        val item = HashMap<String, String>()
                        item["doctor_image"] = it.imageUrl ?: ""
                        item["line1"] = "Doctor name: ${it.username}"
                        item["line2"] = "License No: ${it.medicalLicenseNumber}"
                        item["line11"] = it.medicalLicenseUrl ?: ""
                        item["line3"] = "Experience: ${it.experience}"
                        item["line4"] = "Region: ${it.region}"
                        item["line5"] = "Phone: ${it.phoneNumber}"
                        item["line6"] = "Fees: ${it.fees}"
                        item["line7"] = "Age: ${it.age}"
                        item["line8"] = "Specializations: ${it.specializations.joinToString(", ")}"
                        item["line9"] = "Gender: ${it.gender.joinToString(", ")}"
                        item["line10"] = "Email: ${it.email}"
                        item["userId"] = it.userId
                        item["upiId"] = it.upiId
                        doctorRequestList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch doctor requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDoctorToFirestore(doctorId: String, doctorDetails: Doctor) {
        val doctorData = hashMapOf(
            "userId" to doctorDetails.userId,
            "email" to doctorDetails.email,
            "username" to doctorDetails.username,
            "medicalLicenseNumber" to doctorDetails.medicalLicenseNumber,
            "experience" to doctorDetails.experience,
            "region" to doctorDetails.region,
            "phoneNumber" to doctorDetails.phoneNumber,
            "fees" to doctorDetails.fees,
            "age" to doctorDetails.age,
            "specializations" to doctorDetails.specializations,
            "gender" to doctorDetails.gender,
            "imageUrl" to doctorDetails.imageUrl,
            "medicalLicenseUrl" to doctorDetails.medicalLicenseUrl,
            "upiId" to doctorDetails.upiId,
            "isActive" to true // Set isActive to true
        )
        firestore.collection("doctors").document(doctorId)
            .set(doctorData)
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor approved and added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding doctor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToHistory(doctorId: String, doctorDetails: Doctor) {
        val historyRef = firestore.collection("admin").document("history").collection("doctorHistory")
        historyRef.document(doctorId)
            .set(doctorDetails)
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor details added to history!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding to history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateRequestStatus(doctorId: String) {
        val requestRef = firestore.collection("admin").document("doctorRequests").collection("requests").document(doctorId)
        requestRef.update("status", "approved")
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor status updated to approved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeDoctorRequest(doctorId: String) {
        val requestRef = firestore.collection("admin").document("doctorRequests").collection("requests").document(doctorId)
        requestRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Doctor request removed!", Toast.LENGTH_SHORT).show()
                // Optionally, you can refresh the request list here
                fetchDoctorRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error removing request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Doctor data class for Firestore mapping
    data class Doctor(
        val userId: String = "",
        val email: String = "",
        val username: String = "",
        val medicalLicenseNumber: String = "",
        val experience: String = "",
        val region: String = "",
        val phoneNumber: String = "",
        val fees: String = "",
        val age: String = "",
        val specializations: List<String> = emptyList(),
        val gender: List<String> = emptyList(),
        val imageUrl: String = "",
        val medicalLicenseUrl: String = "",
        val isActive : Boolean = true,
        val upiId : String = ""
    )
}