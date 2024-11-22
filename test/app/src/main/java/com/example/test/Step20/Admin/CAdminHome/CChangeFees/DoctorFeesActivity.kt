package com.example.test.Step20.Admin.CAdminHome.CChangeFees


import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.google.firebase.firestore.FirebaseFirestore

class DoctorFeesActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var feeChangeList: ArrayList<HashMap<String, String>>
    private lateinit var adapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doctor_fees)

        firestore = FirebaseFirestore.getInstance()
        feeChangeList = ArrayList()
        setSupportActionBar(findViewById(R.id.toolbar))
        // Set up the adapter
        adapter = object : SimpleAdapter(
            this,
            feeChangeList,
            R.layout.item_fee_change,  // Custom layout for fee changes
            arrayOf("doctor_name", "current_fees", "requested_fees", "specializations"),
            intArrayOf(R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)
                val approveButton: Button = view.findViewById(R.id.approveButton)
                val rejectButton: Button = view.findViewById(R.id.rejectButton)

                rejectButton.setOnClickListener {
                    val feeRequest = feeChangeList[position]
                    val doctorId = feeRequest["doctorId"] ?: return@setOnClickListener

                    removeFeeChangeRequest(doctorId)
                }

                approveButton.setOnClickListener {
                    val feeRequest = feeChangeList[position]
                    val doctorId = feeRequest["doctorId"] ?: return@setOnClickListener

                    // Approve the fee change
                    approveFeeChange(doctorId, feeRequest["requested_fees"] ?: "")
                }

                return view
            }
        }

        val listView: ListView = findViewById(R.id.listViewDoctorFeesChanges)
        listView.adapter = adapter

        // Fetch fee change requests from Firestore
        fetchFeeChangeRequests()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    private fun fetchFeeChangeRequests() {
        val feeChangeRequestsRef = firestore.collection("admin").document("doctorChangeFees").collection("fees")
        feeChangeRequestsRef.get()
            .addOnSuccessListener { querySnapshot ->
                feeChangeList.clear()
                for (document in querySnapshot.documents) {
                    val doctorId = document.id
                    val doctorName = document.getString("username") ?: "Unknown Doctor"
                    val currentFees = document.getString("currentFees") ?: "N/A"
                    val requestedFees = document.getString("requestedFees") ?: "N/A"
                    val specializations = document.get("specializations") as? List<String> ?: emptyList()

                    val item = HashMap<String, String>()
                    item["doctor_name"] = doctorName
                    item["current_fees"] = "Current Fees: Rs $currentFees"
                    item["requested_fees"] = "Requested Fees: Rs $requestedFees"
                    item["specializations"] = "Specializations: ${specializations.joinToString(", ")}"
                    item["doctorId"] = doctorId
                    feeChangeList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch fee change requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun approveFeeChange(doctorId: String, requestedFees: String) {
        // Remove the "Requested Fees: Rs " prefix, leaving only the number
        val feeValue = requestedFees.replace("Requested Fees: Rs ", "").trim()

        // Update the doctor's fees in the 'doctors' collection
        firestore.collection("doctors").document(doctorId)
            .update("fees", feeValue)
            .addOnSuccessListener {
                Toast.makeText(this, "Fees updated successfully in doctors collection!", Toast.LENGTH_SHORT).show()

                // Update the fees in admin > doctorList > doctorsHistory > doctorId
                updateDoctorHistoryFees(doctorId, feeValue)
                // Remove the fee change request after successfully updating both locations
                removeFeeChangeRequest(doctorId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating fees in doctors collection: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDoctorHistoryFees(doctorId: String, feeValue: String) {
        val doctorHistoryRef = firestore.collection("admin").document("history")
            .collection("doctorHistory").document(doctorId)

        // First, check if the document exists before updating
        doctorHistoryRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Document exists, update the fees
                    doctorHistoryRef.update("fees", feeValue)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Fees updated successfully in doctor history!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating fees in doctor history: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Document doesn't exist, create it with the new fee value
                    val doctorHistoryData = mapOf("fees" to feeValue)
                    doctorHistoryRef.set(doctorHistoryData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Fees document created successfully in doctor history!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error creating doctor history document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching doctor history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun removeFeeChangeRequest(doctorId: String) {
        firestore.collection("admin").document("doctorChangeFees")
            .collection("fees").document(doctorId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Fee change request removed!", Toast.LENGTH_SHORT).show()

                // Optionally, refresh the list
                fetchFeeChangeRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error removing request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}