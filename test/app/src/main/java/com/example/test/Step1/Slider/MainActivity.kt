package com.example.test.Step1.Slider


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.test.R
import com.example.test.Step2.Choice.ChoiceActivity
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var adapter: SliderAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val slidesShown = sharedPreferences.getBoolean("slides_shown", false)

        if (slidesShown) {
            navigateToChoiceActivity()
        } else {
            setupSlider()
        }
    }

    private fun setupSlider() {
        viewPager = findViewById(R.id.viewpager)
        adapter = SliderAdapter(this)
        viewPager.adapter = adapter

        val dotsIndicator = findViewById<DotsIndicator>(R.id.dots_indicator)
        dotsIndicator.setViewPager(viewPager)

        // Set the PageTransformer (optional, can be commented out if causing issues)
        viewPager.setPageTransformer(true, DepthPageTransformer())

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                // Check if the user has reached the last page
                if (position == adapter.count -1) {
                    // Update SharedPreferences to indicate slides have been shown
                    sharedPreferences.edit().putBoolean("slides_shown", true).apply()
                    // Navigate to ChoiceActivity
                    navigateToChoiceActivity()
                }
            }
        })
    }

    private fun navigateToChoiceActivity() {
        startActivity(Intent(this, ChoiceActivity::class.java))
        finish()
    }
}