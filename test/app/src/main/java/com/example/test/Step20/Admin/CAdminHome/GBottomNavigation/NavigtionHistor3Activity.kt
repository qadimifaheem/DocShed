package com.example.test.Step20.Admin.CAdminHome.GBottomNavigation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test.R
import com.example.test.Step20.Admin.CAdminHome.AdminHomeActivity
import com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor.DoctorListActivity
import com.example.test.Step20.Admin.CAdminHome.EEnquiry.EnquiryDoctorsListActivity
import com.example.test.Step21.Patient.CPatientHome.FBottomNavigation.NavigtionHistory2Activity
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavigtionHistor3Activity : AppCompatActivity() {


    private lateinit var cPhysician: CardView
    private lateinit var cDermatology: CardView
    private lateinit var cPsychiatry: CardView
    private lateinit var cPediatrics: CardView
    private lateinit var cStomachanddigestion: CardView
    private lateinit var cBack: CardView


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
        setContentView(R.layout.activity_navigtion_histor3)


        setSupportActionBar(findViewById(R.id.toolbar))

        cBack = findViewById(R.id.cardFDBack)
        cPhysician = findViewById(R.id.cardFDFamilyPhysician)
        cDermatology = findViewById(R.id.cardFDDermatology)
        cPsychiatry = findViewById(R.id.cardFDPsychiatry)
        cPediatrics = findViewById(R.id.cardFDPediatrics)
        cStomachanddigestion = findViewById(R.id.cardFDStomach)


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_History
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_Home -> {
                    // Navigate to Chats fragment/activity
                    startActivity(Intent(this@NavigtionHistor3Activity, AdminHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_History -> {
                    // Navigate to Calls fragment/activity
                    startActivity(Intent(this@NavigtionHistor3Activity, this@NavigtionHistor3Activity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        cBack.setOnClickListener {
            startActivity(Intent(this@NavigtionHistor3Activity, AdminHomeActivity::class.java))
            finish()
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }
    private fun navigateToEnquiryDoctorsListActivity(title: String) {
        val intent = Intent(this@NavigtionHistor3Activity, EnquiryDoctorsListActivity::class.java)
        intent.putExtra(DoctorListActivity.EXTRA_TITLE, title)
        startActivity(intent)
    }

}