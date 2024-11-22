package com.example.test.Step20.Admin.BRegister

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step20.Admin.ALogin.AdminLoginActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup



class AdminRegisterActivity : AppCompatActivity() {



    private lateinit var edUser: EditText
    private lateinit var edEmail: EditText
    private lateinit var edPass: EditText
    private lateinit var edCPass: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edAge: EditText
    private lateinit var txtL: TextView
    private lateinit var btR: Button
    private lateinit var chipGroup1: ChipGroup
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var imgAdmin: ImageView
    private lateinit var imgUri: Uri
    private lateinit var storage: FirebaseStorage
    private lateinit var btnSelectImage: Button
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()


        setContentView(R.layout.activity_admin_register)

        // Initialize UI components
        edUser = findViewById(R.id.AdminEdittextRegisterUsername)
        edEmail = findViewById(R.id.AdminEdittextRegisterEmail)
        edPass = findViewById(R.id.AdminedittextRegisterPassword)
        edCPass = findViewById(R.id.AdmineditTextRegisterConfirmPassword)
        edRegion = findViewById(R.id.AdminRegion)
        edPhoneNumber = findViewById(R.id.AdminPhoneNumber)
        edAge = findViewById(R.id.AdminAge)
        chipGroup1 = findViewById(R.id.chipGroupGender)
        btR = findViewById(R.id.AdminRegisterIn)
        txtL = findViewById(R.id.AdminTextViewGotoLoginPage)
        imgAdmin = findViewById(R.id.AdminProfileImageView)
        btnSelectImage = findViewById(R.id.selectAdminImageButton)

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

        // Set register button click listener
        btR.setOnClickListener {
            val phoneNumber = edPhoneNumber.text.toString()
            if (edEmail.text.toString().isNotEmpty() && edUser.text.toString().isNotEmpty() &&
                edPass.text.toString().isNotEmpty() && edCPass.text.toString().isNotEmpty() &&
                edPass.text.toString() == edCPass.text.toString() && ::imgUri.isInitialized) {

                if (edPass.text.toString().length < 8) {
                    Toast.makeText(this@AdminRegisterActivity, "Password must be at least 8 digits", Toast.LENGTH_SHORT).show()
                } else if (phoneNumber.length != 10) {
                    Toast.makeText(this@AdminRegisterActivity, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                } else {
                    showProgressDialog()
                    registerUser(edEmail.text.toString(), edPass.text.toString(), edUser.text.toString())
                }
            } else if (edPass.text.toString() != edCPass.text.toString()) {
                Toast.makeText(this@AdminRegisterActivity, "Password did not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@AdminRegisterActivity, "Please fill all the details", Toast.LENGTH_SHORT).show()
            }
        }



        // Set login text click listener
        txtL.setOnClickListener {
            startActivity(Intent(this@AdminRegisterActivity,AdminLoginActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            imgUri = data?.data!!
            imgAdmin.setImageURI(imgUri)
        }
    }

    private fun registerUser(email: String, password: String, username: String) {
        // Check if an admin already exists
        firestore.collection("admin").get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                // No admin exists, proceed with registration
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                uploadImageToStorage(it.uid)
                            }
                        } else {
                            hideProgressDialog()
                            Toast.makeText(this@AdminRegisterActivity, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // An admin already exists
                hideProgressDialog()
                Toast.makeText(this@AdminRegisterActivity, "An admin is already registered. Only one admin is allowed.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            hideProgressDialog()
            Toast.makeText(this@AdminRegisterActivity, "Error checking admin: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToStorage(userId: String) {
        val storageRef = storage.reference.child("admin_images/$userId.jpg")
        storageRef.putFile(imgUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    writeUserToDatabase(userId, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this@AdminRegisterActivity, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun writeUserToDatabase(userId: String, imageUrl: String) {

        val selectedGender = mutableListOf<String>()
        for (i in 0 until chipGroup1.childCount) {
            val chip = chipGroup1.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedGender.add(chip.text.toString())
            }
        }


        val admin = Admin(
            userId,
            edEmail.text.toString(),
            edUser.text.toString(),
            edRegion.text.toString(),
            edPhoneNumber.text.toString(),
            edAge.text.toString(),
            selectedGender,
            imageUrl
        )

        firestore.collection("admin").document(userId).set(admin)
            .addOnSuccessListener {
                hideProgressDialog()
                Toast.makeText(this@AdminRegisterActivity, "Admin details saved successfully", Toast.LENGTH_SHORT).show()


                // Save doctorId to SharedPreferences
                val sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("adminId", userId)
                editor.apply()

                startActivity(Intent(this@AdminRegisterActivity, AdminLoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this@AdminRegisterActivity, "Error saving Admin details: ${e.message}", Toast.LENGTH_SHORT).show()
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

    data class Admin(
        val userId: String,
        val email: String,
        val username: String,
        val region: String,
        val phoneNumber: String,
        val age: String,
        val gender: List<String>,
        val imageUrl: String
    )
}

