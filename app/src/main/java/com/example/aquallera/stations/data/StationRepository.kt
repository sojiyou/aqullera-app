package com.example.aquallera.stations.data

import android.util.Log
import com.example.aquallera.core.model.WaterStation
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {
    suspend fun getAllStations(): Result<List<WaterStation>> {
        return try {
            val ref = firebaseDatabase.getReference("waterStations")
            Log.i("StationRepo", "Fetching from: ${ref.toString()}")
            val snapshot = ref.get().await()
            Log.i("StationRepo", "Snapshot exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")
            val stations = snapshot.children.mapNotNull { child ->
                val raw = child.value as? Map<*, *>
                if (raw != null) {
                    Log.i("StationRepo", "Child[${child.key}] keys: ${raw.keys.joinToString(", ")}")
                } else {
                    Log.w("StationRepo", "Child[${child.key}] value is not a Map: ${child.value?.javaClass?.name}")
                }
                try {
                    child.getValue(WaterStation::class.java)?.copy(id = child.key ?: "")
                } catch (e: Exception) {
                    Log.e("StationRepo", "Failed to parse child[${child.key}]: ${e.message}", e)
                    null
                }
            }
            Log.i("StationRepo", "Parsed ${stations.size} stations")
            Result.success(stations)
        } catch (e: Exception) {
            Log.e("StationRepo", "Error fetching stations", e)
            Result.failure(e)
        }
    }

    suspend fun getStationById(stationId: String): Result<WaterStation?> {
        return try {
            val snapshot = firebaseDatabase.getReference("waterStations").child(stationId).get().await()
            val station = snapshot.getValue(WaterStation::class.java)?.copy(id = stationId)
            Result.success(station)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOnlineStations(): Result<List<WaterStation>> {
        return try {
            val snapshot = firebaseDatabase.getReference("waterStations")
                .orderByChild("isOnline")
                .equalTo(true)
                .get()
                .await()
            val stations = snapshot.children.mapNotNull { child ->
                child.getValue(WaterStation::class.java)?.copy(id = child.key ?: "")
            }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
