package com.example.aquallera.orders.data

import android.util.Log
import com.example.aquallera.core.model.Order
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Order creation and retrieval under `/orders`.
 *
 * Reference numbers come from an atomic counter at `/orderCounter/{yyyyMMdd}` rather than being
 * generated client-side, so the app and the PWA never mint the same number.
 */
@Singleton
class OrderRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {
    /** Creates an order with a freshly allocated reference number; returns the new order key. */
    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val counterRef = firebaseDatabase.getReference("orderCounter").child(today)

            val counter = atomicIncrement(counterRef)

            val prefix = order.stationName.take(3).uppercase().ifEmpty { "AQU" }
            val refNumber = "$prefix-$today-${"%04d".format(counter)}"

            val ref = firebaseDatabase.getReference("orders").push()
            val orderId = ref.key ?: return Result.failure(Exception("Failed to generate order ID"))
            ref.setValue(order.copy(orderId = orderId, referenceNumber = refNumber)).await()
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Increments the daily counter transactionally, retrying on contention. */
    private suspend fun atomicIncrement(ref: DatabaseReference): Long = suspendCancellableCoroutine { cont ->
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val current = mutableData.getValue(Long::class.java) ?: 0L
                mutableData.value = current + 1
                return Transaction.success(mutableData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    cont.resumeWithException(error.toException())
                } else {
                    cont.resume(currentData?.getValue(Long::class.java) ?: 1L)
                }
            }
        })
    }

    /** One-shot fetch of a user's orders. Unreadable records are skipped, not fatal. */
    suspend fun getOrdersByUser(userId: String): Result<List<Order>> {
        return try {
            val snapshot = firebaseDatabase.getReference("orders")
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()
            val orders = snapshot.children.mapNotNull { child ->
                try {
                    child.getValue(Order::class.java)
                } catch (e: Exception) {
                    Log.e("OrderRepo", "Failed to parse order[${child.key}]: ${e.message}")
                    null
                }
            }.sortedByDescending { it.createdAt }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Emits the user's orders on every change, so admin status updates appear live. */
    fun getOrdersByUserFlow(userId: String): Flow<List<Order>> = callbackFlow {
        val ref = firebaseDatabase.getReference("orders")
            .orderByChild("userId")
            .equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { child ->
                    try {
                        child.getValue(Order::class.java)
                    } catch (e: Exception) {
                        Log.e("OrderRepo", "Failed to parse order[${child.key}]: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.createdAt }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Sets an order's `status`. Note this does not touch `updatedAt`, unlike the PWA, so an order
     * cancelled from the app keeps its original timestamp.
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            firebaseDatabase.getReference("orders")
                .child(orderId)
                .child("status")
                .setValue(status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancels the user's pending or confirmed orders whose station has since been revoked,
     * so orders cannot sit waiting on a station that no longer operates.
     */
    suspend fun autoCancelRevokedOrdersForUser(userId: String) {
        try {
            val snapshot = firebaseDatabase.getReference("orders")
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()

            val activeOrders = snapshot.children.mapNotNull { child ->
                try {
                    child.getValue(Order::class.java)?.copy(orderId = child.key ?: "")
                } catch (_: Exception) { null }
            }.filter { it.status in listOf("Pending", "Confirmed") }

            val stationIds = activeOrders.map { it.stationId }.distinct()
            for (stationId in stationIds) {
                val revokedSnapshot = firebaseDatabase.getReference("waterStations")
                    .child(stationId)
                    .child("revokedAt")
                    .get()
                    .await()
                if (revokedSnapshot.exists() && revokedSnapshot.getValue(Long::class.java) != null) {
                    val toCancel = activeOrders.filter { it.stationId == stationId }
                    for (order in toCancel) {
                        firebaseDatabase.getReference("orders")
                            .child(order.orderId)
                            .child("status")
                            .setValue("Cancelled")
                            .await()
                    }
                }
            }
        } catch (_: Exception) { }
    }
}
