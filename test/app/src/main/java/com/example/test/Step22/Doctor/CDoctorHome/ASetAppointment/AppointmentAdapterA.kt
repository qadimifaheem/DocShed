package com.example.test.Step22.Doctor.CDoctorHome.ASetAppointment

//for apptiointmentlist number
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.test.R

class AppointmentAdapterA(context: Context, private val appointments: List<String>) :
    ArrayAdapter<String>(context, 0, appointments) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.multi_lines1, parent, false)
        val appointmentNumberTextView: TextView = view.findViewById(R.id.appointmentLabel)
        val appointmentDateTextView: TextView = view.findViewById(R.id.line_1)

        appointmentNumberTextView.text = "Appointment No. ${position + 1}"
        appointmentDateTextView.text = appointments[position]

        return view
    }
}