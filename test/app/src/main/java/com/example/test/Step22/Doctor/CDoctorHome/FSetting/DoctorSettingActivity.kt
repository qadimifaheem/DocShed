package com.example.test.Step22.Doctor.CDoctorHome.FSetting


import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class DoctorSettingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var edEmail: EditText
    private lateinit var edUsername: EditText
    private lateinit var edMedicalLicenseNumber: EditText
    private lateinit var edExperience: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edAge: EditText
    private lateinit var imageViewDoctorPhoto: ImageView
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button // Add this for account deletion
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null
    private var doctorId: String = ""
    private var oldProfileImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_setting)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Processing, please wait...")

        edEmail = findViewById(R.id.editTextEmail)
        edUsername = findViewById(R.id.editTextUsername)
        edMedicalLicenseNumber = findViewById(R.id.editTextMedicalLicenseNumber)
        edExperience = findViewById(R.id.editTextExperience)
        edRegion = findViewById(R.id.editTextRegion)
        edPhoneNumber = findViewById(R.id.editTextPhoneNumber)
        edAge = findViewById(R.id.editTextAge)
        imageViewDoctorPhoto = findViewById(R.id.imageViewDoctorPhoto)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete) // Initialize the delete button


        doctorId = intent.getStringExtra("doctorId") ?: return
        setSupportActionBar(findViewById(R.id.toolbar))

        // Check if the doctor is active
        doctorId?.let {
            checkDoctorStatus(it)
        } ?: run {
            redirectToLogin() // No doctorId means the doctor is not logged in
        }

        loadDoctorDetails()

        imageViewDoctorPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1) // Profile Image
        }

        buttonSave.setOnClickListener {
            progressDialog.show()
            if (selectedImageUri != null) {
                uploadProfileImage()
            } else {
                saveDoctorDetails()
            }
        }

        buttonDelete.setOnClickListener {
            deleteAccount() // Handle account deletion
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun loadDoctorDetails() {
        firestore.collection("doctors").document(doctorId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    edEmail.setText(document.getString("email"))
                    edUsername.setText(document.getString("username"))
                    edMedicalLicenseNumber.setText(document.getString("medicalLicenseNumber"))
                    edExperience.setText(document.getString("experience"))
                    edRegion.setText(document.getString("region"))
                    edPhoneNumber.setText(document.getString("phoneNumber"))
                    edAge.setText(document.getString("age"))

                    oldProfileImageUrl = document.getString("imageUrl")
                    oldProfileImageUrl?.let {
                        Glide.with(this).load(it).into(imageViewDoctorPhoto)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading doctor details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDoctorDetails(doctorData: Map<String, Any?> = emptyMap()) {
        val doctorDetails = mapOf(
            "email" to edEmail.text.toString(),
            "username" to edUsername.text.toString(),
            "medicalLicenseNumber" to edMedicalLicenseNumber.text.toString(),
            "experience" to edExperience.text.toString(),
            "region" to edRegion.text.toString(),
            "phoneNumber" to edPhoneNumber.text.toString(),
            "age" to edAge.text.toString(),
            "imageUrl" to oldProfileImageUrl // In case no new image was selected
        )
        firestore.collection("doctors").document(doctorId)
            .set(doctorDetails + doctorData, SetOptions.merge())
            .addOnSuccessListener {
                // If updating the 'doctors' collection is successful, proceed with updating the 'doctorsHistory' in 'admin > history'
                updateDoctorHistory(doctorDetails + doctorData)
                progressDialog.dismiss()
                Toast.makeText(this, "Details updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error updating details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDoctorHistory(updatedDetails: Map<String, Any?>) {
        // Update the 'admin > history > doctorsHistory > doctorId' collection
        firestore.collection("admin").document("history")
            .collection("doctorHistory").document(doctorId)
            .set(updatedDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Details updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error updating doctor history", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage() {
        selectedImageUri?.let {
            val imageRef =
                FirebaseStorage.getInstance().reference.child("doctor_images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(it)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveDoctorDetails(mapOf("imageUrl" to downloadUri.toString()))
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error uploading profile image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteAccount() {
        progressDialog.show()

        // Delete user profile image if exists
        oldProfileImageUrl?.let {
            val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(it)
            imageRef.delete()
                .addOnCompleteListener {
                    // Proceed with deleting Firestore document and Auth user
                    deleteDoctorDataAndAccount()
                }
        } ?: deleteDoctorDataAndAccount() // If no old profile image, just proceed
    }


    private fun deleteDoctorDataAndAccount() {
        // Delete Firestore document from the 'doctors' collection first
        firestore.collection("doctors").document(doctorId).delete()
            .addOnSuccessListener {
                // Now delete from 'admin > history > doctorHistory'
                firestore.collection("admin").document("history")
                    .collection("doctorHistory").document(doctorId).delete()
                    .addOnSuccessListener {
                        // After Firestore documents are deleted, delete the Firebase Authentication user
                        auth.currentUser?.delete()
                            ?.addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this,
                                    "Account deleted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val sharedPrefs =
                                    getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                                with(sharedPrefs.edit()) {
                                    remove("doctorId")
                                    apply()
                                }

                                // Redirect to LoginActivity
                                val intent = Intent(this, LoginDoctorActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                finish() // Close this activity
                            }
                            ?.addOnFailureListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this,
                            "Error deleting doctor's history data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error deleting doctor's main data", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 1) {
                selectedImageUri = data.data
                imageViewDoctorPhoto.setImageURI(selectedImageUri)
            }
        }
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

    // Function to redirect to Login page
    private fun redirectToLogin() {

        val sha = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        with(sha.edit()) {
            remove("doctorId")
            apply()
            val editor: SharedPreferences.Editor = sha.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@DoctorSettingActivity, LoginDoctorActivity::class.java))
            Toast.makeText(
                this@DoctorSettingActivity,
                "you have been disabled by admin",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
        }
    }
}
