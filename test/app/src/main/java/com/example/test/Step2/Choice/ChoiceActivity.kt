package com.example.test.Step2.Choice

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.test.Step21.Patient.ALogin.LoginActivity
import com.example.test.R
import com.example.test.Step20.Admin.ALogin.AdminLoginActivity
import com.example.test.Step22.Doctor.ALogin.LoginDoctorActivity

class ChoiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_choice)


        val breathingAnimation = ScaleAnimation(
            1f, 1.05f, // Scale from 100% to 105%
            1f, 1.05f, // Scale from 100% to 105%
            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point X (center)
            Animation.RELATIVE_TO_SELF, 0.5f // Pivot point Y (center)
        )
        breathingAnimation.duration = 1000 // Animation duration in milliseconds
        breathingAnimation.repeatCount = Animation.INFINITE // Repeat indefinitely
        breathingAnimation.repeatMode = Animation.REVERSE // Reverse animation on repeat

        val pt: CardView = findViewById(R.id.patient12)
        val dt: CardView = findViewById(R.id.Doctor11)
        val ad: CardView = findViewById(R.id.admin)
        val txt : TextView = findViewById(R.id.CHCHCH)

        pt.startAnimation(breathingAnimation)
        dt.startAnimation(breathingAnimation)
        ad.startAnimation(breathingAnimation)
        txt.startAnimation(breathingAnimation)

        pt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish();
        }

        dt.setOnClickListener {
            startActivity(Intent(this, LoginDoctorActivity::class.java))
            finish();
        }
        ad.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish();
        }

    }

}