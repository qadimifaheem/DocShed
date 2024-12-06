package com.example.test.Step20.Admin.CAdminHome.FSetting

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
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
import com.example.test.Step20.Admin.ALogin.AdminLoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.*


class AdminSettingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var edEmail: EditText
    private lateinit var edUsername: EditText
    private lateinit var edRegion: EditText
    private lateinit var edPhoneNumber: EditText
    private lateinit var edAge: EditText
    private lateinit var imageViewAdminPhoto: ImageView
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button // Add this for account deletion
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null
    private var adminId: String = ""
    private var oldProfileImageUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_setting)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Processing, please wait...")

        edEmail = findViewById(R.id.editTextEmail)
        edUsername = findViewById(R.id.editTextUsername)
        edRegion = findViewById(R.id.editTextRegion)
        edPhoneNumber = findViewById(R.id.editTextPhoneNumber)
        edAge = findViewById(R.id.editTextAge)
        imageViewAdminPhoto = findViewById(R.id.imageViewAdminPhoto)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete) // Initialize the delete button


        adminId = intent.getStringExtra("adminId") ?: return
        setSupportActionBar(findViewById(R.id.toolbar))
        loadAdminDetails()

        imageViewAdminPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1) // Profile Image
        }

        buttonSave.setOnClickListener {


            if (selectedImageUri != null) {
                uploadProfileImage()
            }
            else if (edPhoneNumber.text.toString().length != 10)
            {
                Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()

            }else {
                progressDialog.show()
                saveadminDetails()
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
    private fun loadAdminDetails() {
        firestore.collection("admin").document(adminId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    edEmail.setText(document.getString("email"))
                    edUsername.setText(document.getString("username"))
                    edRegion.setText(document.getString("region"))
                    edPhoneNumber.setText(document.getString("phoneNumber"))
                    edAge.setText(document.getString("age"))
                    oldProfileImageUrl = document.getString("imageUrl")
                    oldProfileImageUrl?.let {
                        Glide.with(this).load(it).into(imageViewAdminPhoto)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading admin details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveadminDetails(adminData: Map<String, Any?> = emptyMap()) {
        val adminDetails = mapOf(
            "email" to edEmail.text.toString(),
            "username" to edUsername.text.toString(),
            "region" to edRegion.text.toString(),
            "phoneNumber" to edPhoneNumber.text.toString(),
            "age" to edAge.text.toString(),
            "imageUrl" to oldProfileImageUrl // In case no new image was selected
        )
        firestore.collection("admin").document(adminId)
            .set(adminDetails + adminData, SetOptions.merge())
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
            val imageRef =
                FirebaseStorage.getInstance().reference.child("admin_images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(it)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveadminDetails(mapOf("imageUrl" to downloadUri.toString()))
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
                    deleteAdminDataAndAccount()
                }
        } ?: deleteAdminDataAndAccount() // If no old profile image, just proceed
    }

    private fun deleteAdminDataAndAccount() {
        // Delete Firestore document first
        firestore.collection("admin").document(adminId).delete()
            .addOnSuccessListener {
                // Then delete the Firebase Authentication user
                auth.currentUser?.delete()
                    ?.addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT)
                            .show()

                        val sharedPrefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                        with(sharedPrefs.edit()) {
                            remove("adminId")
                            apply()
                        }
                        // Redirect to LoginActivity
                        val intent = Intent(this, AdminLoginActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish() // Close this activity
                    }
                    ?.addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Error deleting Admin data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 1) {
                selectedImageUri = data.data
                imageViewAdminPhoto.setImageURI(selectedImageUri)
            }
        }
    }
}
