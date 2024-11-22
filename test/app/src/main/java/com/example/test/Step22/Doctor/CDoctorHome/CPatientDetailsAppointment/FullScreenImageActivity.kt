package com.example.test.Step22.Doctor.CDoctorHome.CPatientDetailsAppointment

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.test.R

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageView: ImageView = findViewById(R.id.fullScreenImageView)
        val imageUrl = intent.getStringExtra("imageUrl")

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.custom_edittext)
                .into(imageView)
        }

        // Close the activity when the image is clicked
        imageView.setOnClickListener {
            finish()
        }
    }
}