package com.example.aquallera.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.aquallera.auth.data.AuthRepository
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun NotificationListener() {
    val context = LocalContext.current
    val entryPoint: NotificationListenerEntryPoint = remember {
        EntryPointAccessors.fromApplication(context, NotificationListenerEntryPoint::class.java)
    }
    val authRepository = entryPoint.authRepository()
    val firebaseDatabase = entryPoint.firebaseDatabase()

    DisposableEffect(Unit) {
        val user = authRepository.getCurrentUser()
        if (user == null) return@DisposableEffect onDispose {}

        val previousStatuses = mutableMapOf<String, String>()

        val ordersListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val orderUserId = snapshot.child("userId").value as? String ?: return
                if (orderUserId != user.uid) return
                val status = snapshot.child("status").value as? String ?: return
                previousStatuses[snapshot.key ?: ""] = status
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val orderUserId = snapshot.child("userId").value as? String ?: return
                if (orderUserId != user.uid) return
                val orderId = snapshot.key ?: return
                val newStatus = snapshot.child("status").value as? String ?: return
                val oldStatus = previousStatuses[orderId]
                if (oldStatus != null && oldStatus != newStatus) {
                    val refNum = snapshot.child("referenceNumber").value as? String ?: orderId.take(8)
                    Toast.makeText(
                        context,
                        "Order $refNum is now $newStatus",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                previousStatuses[orderId] = newStatus
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        val ordersRef = firebaseDatabase.getReference("orders")
        ordersRef.addChildEventListener(ordersListener)

        onDispose {
            ordersRef.removeEventListener(ordersListener)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationListenerEntryPoint {
    fun authRepository(): AuthRepository
    fun firebaseDatabase(): FirebaseDatabase
}
