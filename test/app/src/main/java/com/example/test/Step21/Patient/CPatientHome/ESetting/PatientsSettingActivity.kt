package com.example.test.Step21.Patient.CPatientHome.ESetting

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class PatientsSettingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var edEmail: EditText
    private lateinit var edUsername: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edAge: EditText
    private lateinit var imageViewPatientPhoto: ImageView
    private lateinit var layoutMedicalRecords: LinearLayout
    private lateinit var buttonUpdateMedicalRecord: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null
    private var medicalRecordsUris: MutableList<Uri> = mutableListOf()
    private var medicalRecordsUrls: MutableList<String> = mutableListOf()
    private var userId: String = ""
    private var oldProfileImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patients_setting)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Processing, please wait...")

        edEmail = findViewById(R.id.editTextEmail)
        edUsername = findViewById(R.id.editTextUsername)
        edRegion = findViewById(R.id.editTextRegion)
        edPhoneNumber = findViewById(R.id.editTextPhoneNumber)
        edAge = findViewById(R.id.editTextAge)
        imageViewPatientPhoto = findViewById(R.id.imageViewPatientPhoto)
        layoutMedicalRecords = findViewById(R.id.layoutMedicalRecords)
        buttonUpdateMedicalRecord = findViewById(R.id.buttonUpdateMedicalRecord)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete)

        userId = auth.currentUser?.uid ?: return

        checkPatientStatus(userId)
        setSupportActionBar(findViewById(R.id.toolbar))

        loadPatientDetails()

        imageViewPatientPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        buttonUpdateMedicalRecord.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 2)
        }

        buttonSave.setOnClickListener {
            if (edPhoneNumber.text.toString().length != 10) {
                Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
            } else {
                progressDialog.show()
                val patientData = mapOf(
                    "email" to edEmail.text.toString(),
                    "username" to edUsername.text.toString(),
                    "region" to edRegion.text.toString(),
                    "phoneNumber" to edPhoneNumber.text.toString(),
                    "age" to edAge.text.toString()
                )
                uploadMedicalRecords(patientData)
                selectedImageUri?.let { uploadProfileImage() } ?: updatePatientDetails(patientData)
            }
        }

        buttonDelete.setOnClickListener {
            deleteAccount()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun loadPatientDetails() {
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                document?.let {
                    edEmail.setText(it.getString("email"))
                    edUsername.setText(it.getString("username"))
                    edRegion.setText(it.getString("region"))
                    edPhoneNumber.setText(it.getString("phoneNumber"))
                    edAge.setText(it.getString("age"))

                    oldProfileImageUrl = it.getString("imageUrl")
                    oldProfileImageUrl?.let { url -> Glide.with(this).load(url).into(imageViewPatientPhoto) }

                    val medicalRecords = it.get("medicalRecordsUrls") as? List<String>
                    medicalRecords?.let { urls ->
                        medicalRecordsUrls.clear()
                        medicalRecordsUrls.addAll(urls)
                        layoutMedicalRecords.removeAllViews()
                        urls.forEachIndexed { index, url -> displayMedicalRecord(index, url) }
                    }
                }
            }
            .addOnFailureListener { Toast.makeText(this, "Error loading patient details", Toast.LENGTH_SHORT).show() }
    }

    private fun displayMedicalRecord(index: Int, url: String) {
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(100, 100)
            Glide.with(this@PatientsSettingActivity).load(url).into(this)
        }
        val deleteButton = Button(this).apply {
            text = "Delete"
            setOnClickListener { deleteMedicalRecord(index, url) }
        }
        val recordLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(imageView)
            addView(deleteButton)
        }
        layoutMedicalRecords.addView(recordLayout)
    }

    private fun updatePatientDetails(patientData: Map<String, Any?>) {
        firestore.collection("patients").document(userId)
            .set(patientData, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Details updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error updating details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage() {
        selectedImageUri?.let {
            val imageRef = FirebaseStorage.getInstance().reference.child("profile_images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(it)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val patientData = mapOf(
                            "email" to edEmail.text.toString(),
                            "username" to edUsername.text.toString(),
                            "region" to edRegion.text.toString(),
                            "phoneNumber" to edPhoneNumber.text.toString(),
                            "age" to edAge.text.toString(),
                            "imageUrl" to downloadUri.toString(),
                            "medicalRecordsUrls" to medicalRecordsUrls
                        )
                        uploadMedicalRecords(patientData)
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error uploading profile image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadMedicalRecords(patientData: Map<String, Any?> = emptyMap()) {
        if (medicalRecordsUris.isNotEmpty()) {
            var uploadCounter = 0
            medicalRecordsUris.forEach { uri ->
                val recordRef = FirebaseStorage.getInstance().reference.child("medical_records/${UUID.randomUUID()}.jpg")
                recordRef.putFile(uri)
                    .addOnSuccessListener {
                        recordRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            medicalRecordsUrls.add(downloadUri.toString())
                            uploadCounter++
                            if (uploadCounter == medicalRecordsUris.size) {
                                updatePatientDetails(patientData.toMutableMap().apply {
                                    put("medicalRecordsUrls", medicalRecordsUrls)
                                })
                            }
                        }
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error uploading medical record", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            updatePatientDetails(patientData.toMutableMap().apply {
                put("medicalRecordsUrls", medicalRecordsUrls)
            })
        }
    }

    private fun deleteMedicalRecord(index: Int, url: String) {
        FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
            .addOnSuccessListener {
                medicalRecordsUrls.removeAt(index)
                updatePatientDetails(mapOf("medicalRecordsUrls" to medicalRecordsUrls))
                layoutMedicalRecords.removeViewAt(index)
                Toast.makeText(this, "Medical record deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting medical record", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAccount() {
        progressDialog.show()
        oldProfileImageUrl?.let {
            FirebaseStorage.getInstance().getReferenceFromUrl(it).delete()
                .addOnCompleteListener { deleteMedicalRecordsAndUser() }
        } ?: deleteMedicalRecordsAndUser()
    }

    private fun deleteMedicalRecordsAndUser() {
        if (medicalRecordsUrls.isNotEmpty()) {
            var deleteCounter = 0
            medicalRecordsUrls.forEach { url ->
                FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
                    .addOnSuccessListener {
                        deleteCounter++
                        if (deleteCounter == medicalRecordsUrls.size) deleteUserAccount()
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error deleting medical record", Toast.LENGTH_SHORT).show()
                    }
            }
        } else deleteUserAccount()
    }

    private fun deleteUserAccount() {
        firestore.collection("patients").document(userId).delete()
            .addOnSuccessListener {
                auth.currentUser?.delete()?.addOnSuccessListener {
                    progressDialog.dismiss()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPatientStatus(userId: String) {
        firestore.collection("patients").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Patient account not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { Toast.makeText(this, "Error checking patient status", Toast.LENGTH_SHORT).show() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                when (requestCode) {
                    1 -> {
                        selectedImageUri = it
                        imageViewPatientPhoto.setImageURI(it)
                    }
                    2 -> {
                        medicalRecordsUris.add(it)
                        displayMedicalRecord(layoutMedicalRecords.childCount, it.toString())
                    }
                }
            }
        }
    }
}