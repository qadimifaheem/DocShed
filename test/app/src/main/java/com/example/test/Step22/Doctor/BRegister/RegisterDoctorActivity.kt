package com.example.test.Step22.Doctor.BRegister



import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class RegisterDoctorActivity : AppCompatActivity() {

    private lateinit var edUser: EditText
    private lateinit var edEmail: EditText
    private lateinit var edPass: EditText
    private lateinit var edCPass: EditText
    private lateinit var edMedicalLicenseNumber: EditText
    private lateinit var edExperience: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edFees: EditText
    private lateinit var edAge: EditText
    private lateinit var txtL: TextView
    private lateinit var btR: Button
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipGroup1: ChipGroup
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var imgDoctor: ImageView
    private lateinit var imgUri: Uri
    private lateinit var storage: FirebaseStorage
    private lateinit var btnSelectImage: Button
    private lateinit var progressDialog: ProgressDialog

    private lateinit var imgMedicalLicense: ImageView
    private lateinit var imgMedicalLicenseUri: Uri
    private lateinit var btnSelectMedicalLicenseImage: Button



    //upi
    private lateinit var edUPI: EditText









    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setContentView(R.layout.activity_register_doctor)

        // Initialize UI components
        edUser = findViewById(R.id.DoctorEdittextRegisterUsername)
        edEmail = findViewById(R.id.DoctorEdittextRegisterEmail)
        edPass = findViewById(R.id.DoctoredittextRegisterPassword)
        edCPass = findViewById(R.id.DoctoreditTextRegisterConfirmPassword)
        edMedicalLicenseNumber = findViewById(R.id.DoctorMedicalLicensenumber)
        edExperience = findViewById(R.id.DoctorExperience)
        edRegion = findViewById(R.id.DoctorRegion)
        edPhoneNumber = findViewById(R.id.DoctorPhoneNumber)
        edFees = findViewById(R.id.Doctorfees)
        edAge = findViewById(R.id.DoctorAge)
        chipGroup = findViewById(R.id.doctorSpecializationChipGroup)
        chipGroup1 = findViewById(R.id.chipGroupGender)
        btR = findViewById(R.id.DoctorRegisterIn)
        txtL = findViewById(R.id.DoctorTextViewGotoLoginPage)
        imgDoctor = findViewById(R.id.doctorProfileImageView)
        btnSelectImage = findViewById(R.id.selectDoctorImageButton)
        edUPI = findViewById(R.id.DoctorUPI)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registering, please wait...")
        progressDialog.setCancelable(false)

        // Set button click listener to select image
        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1000)
        }

        imgMedicalLicense = findViewById(R.id.DoctorMedicalLicenseImageView)
        btnSelectMedicalLicenseImage = findViewById(R.id.selectDoctorMedicalLicenseImageButton)

// Set button click listener to select medical license image
        btnSelectMedicalLicenseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 2000)  // Use a unique request code
        }


        // Set register button click listener
        btR.setOnClickListener {
            val phoneNumber = edPhoneNumber.text.toString()
            if (edEmail.text.toString().isNotEmpty() && edUser.text.toString().isNotEmpty() &&
                edPass.text.toString().isNotEmpty() && edCPass.text.toString().isNotEmpty() &&
                edPass.text.toString() == edCPass.text.toString() && ::imgUri.isInitialized) {
                if (edPass.text.toString().length < 8) {
                    Toast.makeText(this@RegisterDoctorActivity, "Password must be at least 8 digits", Toast.LENGTH_SHORT).show()
                }else if (phoneNumber.length != 10) {
                    Toast.makeText(this@RegisterDoctorActivity, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                }
                else {
                    showProgressDialog()
                    registerUser(edEmail.text.toString(), edPass.text.toString(), edUser.text.toString())
                }
            } else if (edPass.text.toString() != edCPass.text.toString()) {
                Toast.makeText(this@RegisterDoctorActivity, "Password did not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RegisterDoctorActivity, "Please fill all the details", Toast.LENGTH_SHORT).show()
            }
        }

        // Set login text click listener
        txtL.setOnClickListener {
            startActivity(Intent(this@RegisterDoctorActivity, LoginDoctorActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                1000 -> {
                    imgUri = data.data!!
                    imgDoctor.setImageURI(imgUri)
                }
                2000 -> {  // Medical License Image
                    imgMedicalLicenseUri = data.data!!
                    imgMedicalLicense.setImageURI(imgMedicalLicenseUri)
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
                        uploadImageToStorage(it.uid)
                    }
                } else {
                    hideProgressDialog()
                    Toast.makeText(this@RegisterDoctorActivity, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageToStorage(userId: String) {
        val profileImageRef = storage.reference.child("doctor_images/$userId.jpg")
        val licenseImageRef = storage.reference.child("doctor_medical_licenses/$userId.jpg")

        profileImageRef.putFile(imgUri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { profileUri ->
                    licenseImageRef.putFile(imgMedicalLicenseUri)
                        .addOnSuccessListener {
                            licenseImageRef.downloadUrl.addOnSuccessListener { licenseUri ->
                                // Pass both URLs as a map
                                val imageUrls = mapOf(
                                    "profileImageUrl" to profileUri.toString(),
                                    "medicalLicenseUrl" to licenseUri.toString()
                                )
                                writeUserToDatabase(userId, imageUrls)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun writeUserToDatabase(userId: String, imageUrls: Map<String, String>) {
        val selectedSpecializations = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedSpecializations.add(chip.text.toString())
            }
        }

        val selectedGender = mutableListOf<String>()
        for (i in 0 until chipGroup1.childCount) {
            val chip = chipGroup1.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedGender.add(chip.text.toString())
            }
        }

        val doctor = Doctor(
            userId,
            edEmail.text.toString(),
            edUser.text.toString(),
            edMedicalLicenseNumber.text.toString(),
            edExperience.text.toString(),
            edRegion.text.toString(),
            edPhoneNumber.text.toString(),
            edFees.text.toString(),
            edAge.text.toString(),
            edUPI.text.toString(),
            selectedSpecializations,
            selectedGender,
            imageUrls["profileImageUrl"] ?: "",
            imageUrls["medicalLicenseUrl"] ?: ""
        )

        val doctorRequest = mapOf(
            "userId" to doctor.userId,
            "email" to doctor.email,
            "upiId" to doctor.upiId,
            "username" to doctor.username,
            "medicalLicenseNumber" to doctor.medicalLicenseNumber,
            "experience" to doctor.experience,
            "region" to doctor.region,
            "phoneNumber" to doctor.phoneNumber,
            "fees" to doctor.fees,
            "age" to doctor.age,
            "specializations" to doctor.specializations,
            "gender" to doctor.gender,
            "imageUrl" to doctor.imageUrl,
            "medicalLicenseUrl" to doctor.medicalLicenseUrl,
            "status" to "pending"
        )

        firestore.collection("admin")
            .document("doctorRequests")
            .collection("requests")
            .document(userId)
            .set(doctorRequest)
            .addOnSuccessListener {
                hideProgressDialog()
                Toast.makeText(this, "Registration successful. Please wait for admin approval.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginDoctorActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Error saving doctor details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showProgressDialog() {
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }



    data class Doctor(
        val userId: String,
        val email: String,
        val username: String,
        val medicalLicenseNumber: String,
        val experience: String,
        val region: String,
        val phoneNumber: String,
        val fees: String,
        val age: String,
        val upiId : String,
        val specializations: List<String>,
        val gender: List<String>,
        val imageUrl: String,
        val medicalLicenseUrl: String
    )
}