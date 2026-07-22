package com.example.aquallera.orders.data

import android.util.Log
import com.example.aquallera.core.model.Order
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OrderRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {
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

    suspend fun getOrderById(orderId: String): Result<Order?> {
        return try {
            val snapshot = firebaseDatabase.getReference("orders").child(orderId).get().await()
            val order = snapshot.getValue(Order::class.java)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
}
