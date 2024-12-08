package com.example.test.Step22.Doctor.CDoctorHome.EHistory

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.test.R


class CustomDoctorHistoryAdapter(
    private val context: Context,
    private val data: ArrayList<HashMap<String, String>>
) : SimpleAdapter(
    context,
    data,
    R.layout.history_details1,
    arrayOf("patient_name", "formatted_time", "patient_email", "patient_age", "patient_phone_number", "patient_gender", "status","appointment_number"),
    intArrayOf(R.id.textPatientName, R.id.textFormattedTime, R.id.textPatientEmail, R.id.textPatientAge, R.id.textPatientPhoneNumber, R.id.textPatientGender, R.id.textStatus,R.id.textAppointmentNumber)
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        // Find each TextView and add a prefix to its text
        val patientNameTextView = view.findViewById<TextView>(R.id.textPatientName)
        val formattedTimeTextView = view.findViewById<TextView>(R.id.textFormattedTime)
        val appointmentNumberTextView = view.findViewById<TextView>(R.id.textAppointmentNumber)
        val patientEmailTextView = view.findViewById<TextView>(R.id.textPatientEmail)
        val patientAgeTextView = view.findViewById<TextView>(R.id.textPatientAge)
        val patientPhoneNumberTextView = view.findViewById<TextView>(R.id.textPatientPhoneNumber)
        val patientGenderTextView = view.findViewById<TextView>(R.id.textPatientGender)
        val statusTextView = view.findViewById<TextView>(R.id.textStatus)

        val dataItem = data[position]

        patientNameTextView.text = "Patient Name: ${dataItem["patient_name"]}"
        formattedTimeTextView.text = "Appointment Time: ${dataItem["formatted_time"]}"
        appointmentNumberTextView.text = "Appointment no: ${dataItem["appointment_number"]}"
        patientEmailTextView.text = "Patient Email: ${dataItem["patient_email"]}"
        patientAgeTextView.text = "PatientAge: ${dataItem["patient_age"]}"
        patientPhoneNumberTextView.text = "Patients Phone no: ${dataItem["patient_phone_number"]}"
        patientGenderTextView.text = "Patients Gender: ${dataItem["patient_gender"]}"
        statusTextView.text = "Status: ${dataItem["status"]}"

        // Apply color to status based on its value
        val status = dataItem["status"]
        if (status == "processing" || status == "appointment over successfully") {
            statusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
        } else {
            statusTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
        }

        return view
    }
}
