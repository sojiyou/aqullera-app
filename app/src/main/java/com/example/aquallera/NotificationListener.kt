package com.example.aquallera

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class NotificationListener(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseConfig.getDatabaseReference()

    private val stationPreviousState = mutableMapOf<String, Boolean>()
    private val orderPreviousState = mutableMapOf<String, String>()
    private var stationInitialized = false
    private var orderInitialized = false

    private var stationListener: ValueEventListener? = null
    private var orderListener: ValueEventListener? = null

    fun start() {
        listenToStations()
        listenToOrders()
    }

    fun stop() {
        stationListener?.let { database.child("waterStations").removeEventListener(it) }
        orderListener?.let { database.child("orders").removeEventListener(it) }
        stationListener = null
        orderListener = null
    }

    private fun listenToStations() {
        val ref = database.child("waterStations")
        stationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                    val wasOnline = stationPreviousState[id]

                    if (stationInitialized && wasOnline == false && isOnline == true) {
                        val name = child.child("stationName").getValue(String::class.java) ?: "A station"
                        Toast.makeText(context, "$name is now online.", Toast.LENGTH_SHORT).show()
                    }

                    stationPreviousState[id] = isOnline
                }
                stationInitialized = true
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(stationListener!!)
    }

    private fun listenToOrders() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val ref = database.child("orders")
        orderListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val orderUserId = child.child("userId").getValue(String::class.java)
                        ?: child.child("customerId").getValue(String::class.java)
                        ?: continue

                    if (orderUserId != userId) continue

                    val orderId = child.key ?: continue
                    val currentStatus = child.child("status").getValue(String::class.java) ?: ""
                    val previousStatus = orderPreviousState[orderId]

                    if (orderInitialized && previousStatus != null && previousStatus != currentStatus) {
                        val refNumber = child.child("referenceNumber").getValue(String::class.java) ?: orderId
                        val shortRef = shortenRef(refNumber)
                        val displayStatus = normalizeStatus(currentStatus)
                        Toast.makeText(context, "Order $shortRef updated to $displayStatus.", Toast.LENGTH_SHORT).show()
                    }

                    orderPreviousState[orderId] = currentStatus
                }
                orderInitialized = true
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(orderListener!!)
    }

    private fun shortenRef(refStr: String): String {
        val parts = refStr.split("-")
        if (parts.size == 3 && parts[2].length == 4) {
            return "${parts[0]}-...-${parts[2]}"
        }
        return if (refStr.length > 10) "...${refStr.takeLast(4)}" else refStr
    }

    private fun normalizeStatus(status: String): String {
        if (status.isEmpty()) return ""
        return status.replaceFirstChar { it.uppercase() }
    }
}
