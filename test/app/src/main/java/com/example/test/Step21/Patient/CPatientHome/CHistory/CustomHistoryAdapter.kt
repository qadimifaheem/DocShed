package com.example.test.Step21.Patient.CPatientHome.CHistory

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.test.R

class CustomHistoryAdapter(
    private val context: Context,
    private val data: ArrayList<HashMap<String, String>>
) : SimpleAdapter(
    context,
    data,
    R.layout.history_details,
    arrayOf("doctor_name", "formatted_time", "specializations", "status"),
    intArrayOf(R.id.doctorName, R.id.formattedTime, R.id.specializations, R.id.status)
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val statusTextView = view.findViewById<TextView>(R.id.status)
        val status = data[position]["status"]

        if (status == "processing" || status == "appointment over successfully") {
            statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
        } else {
            statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
        }
        return view
    }
}