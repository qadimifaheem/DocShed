package com.example.test.Step21.Patient.ALogin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.Step21.Patient.CPatientHome.PatientHomeActivity
import com.example.test.Step21.Patient.BRegister.RegisterActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var edE: EditText
    private lateinit var edP: EditText
    private lateinit var bt: Button
    private lateinit var txt: TextView
    private lateinit var fptxt: TextView
    private lateinit var gsin: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val RC_SIGN_IN = 9001
    }


    private fun clearDoctorSession() {
        val sharedPrefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove("doctorId")
            apply()
        }
    }
    private fun clearAdminSession() {
        val sharedPrefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove("adminId")
            apply()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        clearDoctorSession() // Clear doctor session before setting up patient login
        clearAdminSession() // Clear Admin session before setting up patient login

        // Check login status BEFORE setting content view
        if (isUserLoggedIn()) {
            startActivity(Intent(this@LoginActivity, PatientHomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        edE = findViewById(R.id.PEmail)
        edP = findViewById(R.id.edittextpassword)
        bt = findViewById(R.id.LoginButton)
        txt = findViewById(R.id.registerNewUser)
        gsin = findViewById(R.id.GoodleSignin)
        fptxt = findViewById(R.id.patientforgotpassword)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        gsin.setOnClickListener {
            signInWithGoogle()
        }

        bt.setOnClickListener {
            if (edE.text.toString().isNotEmpty() && edP.text.toString().isNotEmpty()) {
                loginUser(edE.text.toString(), edP.text.toString())
            } else {
                Toast.makeText(this@LoginActivity, "Please fill all details", Toast.LENGTH_SHORT).show()
            }
        }

        txt.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }


        // Add the click listener for forgot password
        fptxt.setOnClickListener {
            val email = edE.text.toString()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Function to send the password reset email
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error sending reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        firestore.collection("patients").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val isActive = document.getBoolean("isActive") ?: true // Check if account is active
                                    if (!isActive) {
                                        auth.signOut() // Sign out if account is disabled
                                        Toast.makeText(this, "Your account has been disabled. Please contact support.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val userId = document.id
                                        val sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        editor.putString("username", userId)
                                        editor.apply()

                                        startActivity(Intent(this@LoginActivity, PatientHomeActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    Toast.makeText(this@LoginActivity, "Patient not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@LoginActivity, "Error retrieving patient details: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        val savedUsername = sharedPrefs.getString("username", null)
        return savedUsername != null
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener { // Sign out first to ensure the account picker shows up
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        firestore.collection("patients").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val isActive = document.getBoolean("isActive") ?: true // Check if account is active
                                    if (!isActive) {
                                        auth.signOut() // Sign out if account is disabled
                                        Toast.makeText(this, "Your account has been disabled. Please contact support.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val userId = document.id
                                        val sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        editor.putString("username", userId)
                                        editor.apply()

                                        startActivity(Intent(this@LoginActivity, PatientHomeActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    Toast.makeText(this@LoginActivity, "patient not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error retrieving patient details: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

}