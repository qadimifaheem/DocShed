package com.example.test.Step21.Patient.BRegister


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import android.app.ProgressDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class RegisterActivity : AppCompatActivity() {

    private lateinit var edUser: EditText
    private lateinit var edEmail: EditText
    private lateinit var edPass: EditText
    private lateinit var edCPass: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edAge: EditText
    private lateinit var txtL: TextView
    private lateinit var btR: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var imageViewPatientPhoto: ImageView
    private lateinit var buttonSelectPhoto: Button
    private lateinit var layoutMedicalRecords: LinearLayout
    private lateinit var buttonAddMedicalRecord: Button
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null
    private var medicalRecordsUris: MutableList<Uri> = mutableListOf()
    private lateinit var chipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        setContentView(R.layout.activity_register)

        edUser = findViewById(R.id.EdittextRegisterUsername)
        edEmail = findViewById(R.id.EdittextRegisterEmail)
        edPass = findViewById(R.id.edittextRegisterPassword)
        edCPass = findViewById(R.id.editTextRegisterConfirmPassword)
        edRegion = findViewById(R.id.PatientRegion)
        edPhoneNumber = findViewById(R.id.PatientPhoneNumber)
        edAge = findViewById(R.id.PatientAge)
        btR = findViewById(R.id.RegisterIn)
        txtL = findViewById(R.id.TextViewGotoLoginPage)
        imageViewPatientPhoto = findViewById(R.id.imageViewPatientPhoto)
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto)
        layoutMedicalRecords = findViewById(R.id.layoutMedicalRecords)
        buttonAddMedicalRecord = findViewById(R.id.buttonAddMedicalRecord)
        chipGroup = findViewById(R.id.chipGroupGender)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registering, please wait...")
        progressDialog.setCancelable(false)

        buttonSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        buttonAddMedicalRecord.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 2)
        }

        btR.setOnClickListener {
            val phoneNumber = edPhoneNumber.text.toString()

            if (edEmail.text.toString().isNotEmpty() && edUser.text.toString().isNotEmpty() &&
                edPass.text.toString().isNotEmpty() && edCPass.text.toString().isNotEmpty() &&
                edPass.text.toString() == edCPass.text.toString()) {
                if (edPass.text.toString().length < 8) {
                    Toast.makeText(this@RegisterActivity, "Password must be at least 8 digits", Toast.LENGTH_SHORT).show()
                }else if (phoneNumber.length != 10) {
                    Toast.makeText(this@RegisterActivity, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                }
                else {
                    progressDialog.show()
                    registerUser(edEmail.text.toString(), edPass.text.toString(), edUser.text.toString())
                }
            } else if (edPass.text.toString() != edCPass.text.toString()) {
                Toast.makeText(this@RegisterActivity, "Password did not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RegisterActivity, "Please fill all the details", Toast.LENGTH_SHORT).show()
            }
        }

        txtL.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 1) { // Patient Photo
                selectedImageUri = data.data
                imageViewPatientPhoto.setImageURI(selectedImageUri)
            } else if (requestCode == 2) { // Medical Records
                val selectedUri = data.data
                if (selectedUri != null) {
                    medicalRecordsUris.add(selectedUri)
                    val imageView = ImageView(this)
                    imageView.layoutParams = LinearLayout.LayoutParams(100, 100)
                    imageView.setImageURI(selectedUri)
                    layoutMedicalRecords.addView(imageView)
                }
            }
        }
    }

    private fun registerUser(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        uploadImageToStorage(it.uid, email, username)
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@RegisterActivity, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageToStorage(userId: String, email: String, username: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val patientImageRef = storageRef.child("patient_images/${UUID.randomUUID()}.jpg")
        val medicalRecordsUrls = mutableListOf<String>()

        if (selectedImageUri != null) {
            patientImageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    patientImageRef.downloadUrl.addOnSuccessListener { uri ->
                        uploadMedicalRecords(userId, email, username, uri.toString(), medicalRecordsUrls)
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    writeUserToDatabase(userId, email, username, null, medicalRecordsUrls)
                }
        } else {
            uploadMedicalRecords(userId, email, username, null, medicalRecordsUrls)
        }
    }

    private fun uploadMedicalRecords(userId: String, email: String, username: String, profileImageUrl: String?, medicalRecordsUrls: MutableList<String>) {
        if (medicalRecordsUris.isEmpty()) {
            writeUserToDatabase(userId, email, username, profileImageUrl, medicalRecordsUrls)
            return
        }

        medicalRecordsUris.forEachIndexed { index, uri ->
            val recordRef = FirebaseStorage.getInstance().reference.child("medical_records/${UUID.randomUUID()}.jpg")
            recordRef.putFile(uri)
                .addOnSuccessListener {
                    recordRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        medicalRecordsUrls.add(downloadUri.toString())
                        if (index == medicalRecordsUris.size - 1) {
                            writeUserToDatabase(userId, email, username, profileImageUrl, medicalRecordsUrls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Medical record upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    if (index == medicalRecordsUris.size - 1) {
                        writeUserToDatabase(userId, email, username, profileImageUrl, medicalRecordsUrls)
                    }
                }
        }
    }

    private fun writeUserToDatabase(userId: String, email: String, username: String, profileImageUrl: String?, medicalRecordsUrls: List<String>) {
        val selectedGender = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedGender.add(chip.text.toString())
            }
        }

        val patient = Patient(
            userId,
            email,
            username,
            edRegion.text.toString(),
            edPhoneNumber.text.toString(),
            edAge.text.toString(),
            profileImageUrl,
            medicalRecordsUrls,
            selectedGender
        )

        firestore.collection("patients").document(userId).set(patient)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this@RegisterActivity, "Patient details saved successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this@RegisterActivity, "Error saving patient details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Patient(
        val userId: String,
        val email: String,
        val username: String,
        val region: String,
        val phoneNumber: String,
        val age: String,
        val imageUrl: String?,
        val medicalRecordsUrls: List<String>,
        val selectedGender: List<String>
    )
}