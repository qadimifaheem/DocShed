package com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.test.R
import com.example.test.Step20.Admin.CAdminHome.AdminHomeActivity
import com.example.test.Step20.Admin.CAdminHome.DList.AChoice.ADoctor.ADoctorsDetails.AllDoctorsActivity

class DoctorListActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_doctor_list)

        setSupportActionBar(findViewById(R.id.toolbar))


        cBack = findViewById(R.id.cardFDBack)
        cPhysician = findViewById(R.id.cardFDFamilyPhysician)
        cDermatology = findViewById(R.id.cardFDDermatology)
        cPsychiatry = findViewById(R.id.cardFDPsychiatry)
        cPediatrics = findViewById(R.id.cardFDPediatrics)
        cStomachanddigestion = findViewById(R.id.cardFDStomach)

        cBack.setOnClickListener {
            startActivity(Intent(this@DoctorListActivity, AdminHomeActivity::class.java))
            finish()
        }

        cPhysician.setOnClickListener {
            navigateToAllDoctorsActivity(TITLE_GENERAL_PHYSICIAN)
        }

        cDermatology.setOnClickListener {
            navigateToAllDoctorsActivity(TITLE_DERMATOLOGY)
        }

        cPsychiatry.setOnClickListener {
            navigateToAllDoctorsActivity(TITLE_PSYCHIATRY)
        }

        cPediatrics.setOnClickListener {
            navigateToAllDoctorsActivity(TITLE_PEDIATRICS)
        }

        cStomachanddigestion.setOnClickListener {
            navigateToAllDoctorsActivity(TITLE_STOMACH_AND_DIGESTION)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun navigateToAllDoctorsActivity(title: String) {
        val intent = Intent(this@DoctorListActivity, AllDoctorsActivity::class.java)
        intent.putExtra(EXTRA_TITLE, title)
        startActivity(intent)
    }

}
