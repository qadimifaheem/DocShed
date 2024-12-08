package com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment;



import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.test.R
import com.google.firebase.firestore.FirebaseFirestore

class PatientDetailsActivity : AppCompatActivity() {

    private lateinit var patientDetailsList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var patientImageView: ImageView
    private lateinit var medicalRecordsLayout: LinearLayout
    private lateinit var previousVisitsTextView: TextView
    private var userId: String? = null
    private var doctorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_details)

        // Initialize views
        val listView: ListView = findViewById(R.id.patientDetailsList)
        patientImageView = findViewById(R.id.patientImageView)
        medicalRecordsLayout = findViewById(R.id.medicalRecordsLayout)
        previousVisitsTextView = findViewById(R.id.previousVisitsTextView)
        val previousVisitsButton: Button = findViewById(R.id.previousVisitsButton)

        // Retrieve patient details and image URLs from intent
        patientDetailsList = intent.getSerializableExtra("patientDetails") as ArrayList<HashMap<String, String>>
        val imageUrl = intent.getStringExtra("imageUrl")
        val medicalRecordsUrls = intent.getStringArrayListExtra("medicalRecordsUrls")

        userId = intent.getStringExtra("userId") // Retrieve userId here
        doctorId = intent.getStringExtra("doctorId")
        setSupportActionBar(findViewById(R.id.toolbar))


        // Load the patient's profile image
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.custom_edittext) // Placeholder image
                .into(patientImageView)
            patientImageView.tag = it // Set the tag to URL for full-screen viewing

            patientImageView.setOnClickListener {
                openFullScreenImage(it as ImageView)
            }
        }

        // Load the medical records images
        medicalRecordsUrls?.forEach { url ->
            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(300, 300) // Adjust size as needed
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.custom_edittext)
                .into(imageView)
            imageView.tag = url // Set the tag to URL for full-screen viewing

            imageView.setOnClickListener {
                openFullScreenImage(it as ImageView)
            }

            medicalRecordsLayout.addView(imageView)
        }

        // Concatenate patient details into a single string for display
        val detailsText = buildString {
            for (detail in patientDetailsList) {
                append("${detail["key"]}: ${detail["value"]}\n")
            }
        }

        loadPreviousVisits()

        previousVisitsButton.setOnClickListener {
            val intent = Intent(this, MedicineDetailsActivity::class.java)
            intent.putExtra("doctorId", doctorId)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
        // Set up the adapter to display patient details
        adapter = ArrayAdapter(this, R.layout.patient_detail_item, R.id.detailText, listOf(detailsText))
        listView.adapter = adapter
    }

    // Method to open the full-screen image view
    private fun openFullScreenImage(imageView: ImageView) {
        val intent = Intent(this, FullScreenImageActivity::class.java)
        intent.putExtra("imageUrl", imageView.tag as String) // Pass the image URL to the full-screen activity
        startActivity(intent)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }



    // Method to fetch and display previous visits count
    private fun loadPreviousVisits() {
        if (userId == null || doctorId == null) {
            Toast.makeText(this, "User ID or Doctor ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val alarmsRef = firestore.collection("doctors")
            .document(doctorId!!)
            .collection("patients")
            .document(userId!!)
            .collection("Alarms")

        alarmsRef.get()
            .addOnSuccessListener { documents ->
                val visitsCount = documents.size()
                val message = if (visitsCount > 0) {
                    "This patient has visited $visitsCount times."
                } else {
                    "This patient has no previous visits recorded."
                }
                previousVisitsTextView.text = message
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching visits: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}

