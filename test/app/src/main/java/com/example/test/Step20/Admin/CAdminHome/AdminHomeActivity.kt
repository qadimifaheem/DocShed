package com.example.test.Step20.Admin.CAdminHome

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.Step20.Admin.ALogin.AdminLoginActivity
import com.example.test.Step20.Admin.CAdminHome.ARequest.RequestActivity
import com.example.test.Step20.Admin.CAdminHome.BCancle.CancleRequestActivity
import com.example.test.Step20.Admin.CAdminHome.CChangeFees.DoctorFeesActivity
import com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor.DoctorListActivity
import com.example.test.Step20.Admin.CAdminHome.DList.AdminChoiceActivity
import com.example.test.Step20.Admin.CAdminHome.EEnquiry.EnquiryDoctorsListActivity
import com.example.test.Step20.Admin.CAdminHome.EEnquiry.EnquiryListActivity
import com.example.test.Step20.Admin.CAdminHome.FSetting.AdminSettingActivity
import com.example.test.Step20.Admin.CAdminHome.GBottomNavigation.NavigtionHistor3Activity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var request: CardView
    private lateinit var fees: CardView
    private lateinit var CancleAppio: CardView
    private lateinit var doclist: CardView
    private lateinit var enquiry: CardView
    private lateinit var cLogOut: CardView



    private lateinit var cPhysician: CardView
    private lateinit var cDermatology: CardView
    private lateinit var cPsychiatry: CardView
    private lateinit var cPediatrics: CardView
    private lateinit var cStomachanddigestion: CardView


    companion object {
        const val EXTRA_TITLE = "title"
        const val TITLE_GENERAL_PHYSICIAN = "General Physician"
        const val TITLE_DERMATOLOGY = "Dermatology"
        const val TITLE_PSYCHIATRY = "Psychiatry"
        const val TITLE_PEDIATRICS = "Pediatrics"
        const val TITLE_STOMACH_AND_DIGESTION = "Stomach and digestion"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_home)

        ShowGIF()
        val sha: SharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)

        fees = findViewById(R.id.AdminHomeChangeFees)
        enquiry = findViewById(R.id.AdminEnquiry)
        doclist = findViewById(R.id.AdminHomeShowPatientDetails)
        request = findViewById(R.id.AdminHomeRequestDoctor)
        CancleAppio = findViewById(R.id.CancleAppointmentRequest)
        cPhysician = findViewById(R.id.cardFDFamilyPhysician)
        cDermatology = findViewById(R.id.cardFDDermatology)
        cPsychiatry = findViewById(R.id.cardFDPsychiatry)
        cPediatrics = findViewById(R.id.cardFDPediatrics)
        cStomachanddigestion = findViewById(R.id.cardFDStomach)
        cLogOut = findViewById(R.id.Logout)



        setSupportActionBar(findViewById(R.id.toolbar))


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_Home
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_Home -> {
                    // Navigate to Chats fragment/activity
                    startActivity(Intent(this@AdminHomeActivity, AdminHomeActivity::class.java))
                    finish()
                    true
                }

                R.id.navigation_History -> {
                    val adminId = sha.getString("adminId", null)
                    if (adminId != null) {
                        val intent =
                            Intent(this@AdminHomeActivity, NavigtionHistor3Activity::class.java)
                        intent.putExtra("adminId", adminId)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                else -> false
            }
        }

        cPhysician.setOnClickListener {
            navigateToEnquiryDoctorsListActivity(DoctorListActivity.TITLE_GENERAL_PHYSICIAN)
        }

        cDermatology.setOnClickListener {
            navigateToEnquiryDoctorsListActivity(DoctorListActivity.TITLE_DERMATOLOGY)
        }

        cPsychiatry.setOnClickListener {
            navigateToEnquiryDoctorsListActivity(DoctorListActivity.TITLE_PSYCHIATRY)
        }

        cPediatrics.setOnClickListener {
            navigateToEnquiryDoctorsListActivity(DoctorListActivity.TITLE_PEDIATRICS)
        }

        cStomachanddigestion.setOnClickListener {
            navigateToEnquiryDoctorsListActivity(DoctorListActivity.TITLE_STOMACH_AND_DIGESTION)
        }


        //for logout
        cLogOut.setOnClickListener {
            val editor: SharedPreferences.Editor = sha.edit()
            editor.clear()
            editor.apply()
            startActivity(Intent(this@AdminHomeActivity, AdminLoginActivity::class.java))
            Toast.makeText(this@AdminHomeActivity, "Logout successfully", Toast.LENGTH_SHORT).show()
            finish()

        }

        enquiry.setOnClickListener {
            val adminId = sha.getString("adminId", null)
            if (adminId != null) {
                val intent = Intent(this@AdminHomeActivity, EnquiryListActivity::class.java)
                intent.putExtra("adminId", adminId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
            }

        }

        //for approved doctor list
        doclist.setOnClickListener {
            val adminId = sha.getString("adminId", null)
            if (adminId != null) {
                val intent = Intent(this@AdminHomeActivity, AdminChoiceActivity::class.java)
                intent.putExtra("adminId", adminId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
            }

        }


        //for doctor reschedule request
        CancleAppio.setOnClickListener {
            val adminId = sha.getString("adminId", null)
            if (adminId != null) {
                val intent = Intent(this@AdminHomeActivity, CancleRequestActivity::class.java)
                intent.putExtra("adminId", adminId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
            }
        }

        //for request
        request.setOnClickListener {
            val adminId = sha.getString("adminId", null)
            if (adminId != null) {
                val intent = Intent(this@AdminHomeActivity, RequestActivity::class.java)
                intent.putExtra("adminId", adminId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
            }
        }

        //for change fees
        fees.setOnClickListener {
            val adminId = sha.getString("adminId", null)
            if (adminId != null) {
                val intent = Intent(this@AdminHomeActivity, DoctorFeesActivity::class.java)
                intent.putExtra("adminId", adminId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Admin ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateToEnquiryDoctorsListActivity(title: String) {
        val intent = Intent(this@AdminHomeActivity, EnquiryDoctorsListActivity::class.java)
        intent.putExtra(DoctorListActivity.EXTRA_TITLE, title)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin_home, menu)
        return true
    }

    private fun ShowGIF() {
        val GIF: ImageView = findViewById(R.id.GIF)
        Glide.with(this).load(R.drawable.gif1).into(GIF)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val sha: SharedPreferences =
                    getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
                // Handle settings click
                val adminId = sha.getString("adminId", null)
                if (adminId != null) {
                    val intent = Intent(this@AdminHomeActivity, AdminSettingActivity::class.java)
                    intent.putExtra("adminId", adminId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Admin not found", Toast.LENGTH_SHORT).show()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}