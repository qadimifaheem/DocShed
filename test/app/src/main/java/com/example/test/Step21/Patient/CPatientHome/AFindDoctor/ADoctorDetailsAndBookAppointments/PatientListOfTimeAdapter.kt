package com.example.test.Step21.Patient.CPatientHome.AFindDoctor.ADoctorDetailsAndBookAppointments


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.test.R


class PatientListOfTimeAdapter(
    context: Context,
    private val appointmentList: List<DoctorAppointmentsActivity.Appointment>,
    private val currentUserId: String
) : ArrayAdapter<DoctorAppointmentsActivity.Appointment>(context, 0, appointmentList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.patientlistoftime, parent, false)
        val appointment = appointmentList[position]

        val appointmentNumber = view.findViewById<TextView>(R.id.appointmentNumber)
        val formattedTime = view.findViewById<TextView>(R.id.formatted_time)

        // Correctly setting the appointment number based on position
        appointmentNumber.text = "Appointment ${position + 1}"
        formattedTime.text = appointment.formattedTime

        // Update the color based on booking status
        updateAppointmentColor(view, appointment)

        return view
    }

    private fun updateAppointmentColor(view: View, appointment: DoctorAppointmentsActivity.Appointment) {
        val colorResId = when {
            appointment.bookedByUserId == currentUserId -> R.color.green // Booked by the current user
            appointment.isBooked -> R.color.red // Booked by another user
            else -> R.color.white // Available
        }
        view.setBackgroundColor(ContextCompat.getColor(context, colorResId))
    }
}
