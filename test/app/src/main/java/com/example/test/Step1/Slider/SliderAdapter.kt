package com.example.test.Step1.Slider

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.example.test.Step2.Choice.ChoiceActivity
import com.example.test.R

class SliderAdapter(private val context: Context) : PagerAdapter() {

    private lateinit var layoutInflater: LayoutInflater

    val listImg = listOf(

        R.drawable.icon3_modified,
        R.drawable._2,
        R.drawable.r121
    )
    val title = listOf(
        "Welcome to DocShed", "Easy Appointment Booking", "Medication Reminders"
    )
    val desc1 = listOf(
        "Welcome to DocShed,",
        "Booking appointments is simple",
        "Never miss a dose again"
    )
    val desc2 = listOf(
        "your ultimate healthcare companion.",
        "with our intuitive interface.",
        "with our timely medication reminders."
    )
    val bg = listOf(
        R.drawable._21, R.drawable._212, R.drawable._234
    )

    //for page
//    val pg = listOf(
//        "slide to go next\n >>>>>>>>>>>>>",
//        "slide to go next\n >>>>>>>>>>>>>",
//        "press start to choose your role"
//    )
    override fun getCount(): Int {
        return title.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`  // No need to cast
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.slide, container, false)

        val linearLayout: LinearLayout = view.findViewById(R.id.slide_layout)
        val img: ImageView = view.findViewById(R.id.slide_img)
        val heading: TextView = view.findViewById(R.id.slide_heading)
        val description1: TextView = view.findViewById(R.id.slide_descrip1)
        val description2: TextView = view.findViewById(R.id.slide_descrip2)

        //for page
        //val page: TextView = view.findViewById(R.id.slidepages)



        linearLayout.setBackgroundResource(bg[position])
        img.setImageResource(listImg[position])
        heading.text = title[position]
        description1.text = desc1[position]
        description2.text = desc2[position]

        //for page
        // page.text = pg[position]


        val bt : Button = view.findViewById(R.id.start_button)
        if (position == count - 1) {
            bt.visibility = View.VISIBLE
            bt.setOnClickListener {
                context.startActivity(Intent(context, ChoiceActivity::class.java))
                (context as Activity).finish() // Optionally finish the current activity
            }
        }
        container.addView(view)
        return view
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View) // Cast to View for removal
    }

}