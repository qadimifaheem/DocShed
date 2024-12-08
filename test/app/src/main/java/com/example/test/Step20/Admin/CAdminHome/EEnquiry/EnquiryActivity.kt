package com.example.test.Step20.Admin.CAdminHome.EEnquiry


import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.google.firebase.firestore.FirebaseFirestore

class EnquiryActivity : AppCompatActivity() {

   // private lateinit var back: CardView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var enquiryList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enquiry)

        firestore = FirebaseFirestore.getInstance()
     //   back = findViewById(R.id.cardDDBack)

        // Retrieve userId passed from EnquiryDoctorsListActivity
        val userId = intent.getStringExtra("userId")
        setSupportActionBar(findViewById(R.id.toolbar))
//        back.setOnClickListener { finish() }

        enquiryList = ArrayList()
        adapter = object : SimpleAdapter(
            this,
            enquiryList,
            R.layout.multi_lines5,  // Use your custom layout for the ListView rows
            arrayOf("line1", "line2", "line3", "line4", "line5", "line6"),
            intArrayOf(R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e, R.id.line_f)
        )
        {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                // Find the TextView for the status line
                val statusTextView: TextView = view.findViewById(R.id.line_f)

                // Get the status value from the data (ignore any prefixes)
                val status = enquiryList[position]["line6"]?.lowercase() // Convert to lowercase for case-insensitive comparison

                // Check if the status contains "appointment over" ignoring any prefixes
                if (status?.contains("appointment over successfully") == true || status?.contains("processing") == true) {
                    statusTextView.setTextColor(Color.GREEN) // Set green color for "Appointment Over"
                } else {
                    statusTextView.setTextColor(Color.RED) // Set red color for other statuses
                }

                return view
            }
        }

        val listView: ListView = findViewById(R.id.listViewAbsent)
        listView.adapter = adapter
        if (userId != null) {
            fetchEnquiryDetails(userId)
        } else {
            Toast.makeText(this, "Error: Doctor ID not found.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }



    private fun fetchEnquiryDetails(userId: String) {
        val historyRef = firestore.collection("doctors").document(userId).collection("history")

        historyRef.get()
            .addOnSuccessListener { historyQuerySnapshot ->
                enquiryList.clear() // Clear list to avoid duplication
                for (appointmentDocument in historyQuerySnapshot.documents) {
                    val appointmentData = appointmentDocument.data

                    val item = HashMap<String, String>()
                    item["line1"] = "Doctor Name: ${appointmentData?.get("doctor_name")}"
                    item["line2"] = "Specialization: ${appointmentData?.get("doctor_specializations")}"
                    item["line3"] = "Patient Name: ${appointmentData?.get("patient_name")}"
                    item["line4"] = "Patient Phone No: ${appointmentData?.get("patient_phone_number")}"
                    item["line5"] = "Time: ${appointmentData?.get("formatted_time")}"
                    item["line6"] = "Status: ${appointmentData?.get("status")}"

                    enquiryList.add(item)
                }
                if (enquiryList.isEmpty()) {
                    showToast("No history found")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
