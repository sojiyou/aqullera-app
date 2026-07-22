package com.example.aquallera.orders.data

import android.util.Log
import com.example.aquallera.core.model.Order
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {
    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val ref = firebaseDatabase.getReference("orders").push()
            val orderId = ref.key ?: return Result.failure(Exception("Failed to generate order ID"))
            ref.setValue(order.copy(orderId = orderId)).await()
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
