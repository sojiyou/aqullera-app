package com.example.aquallera.stations.data

import android.util.Log
import com.example.aquallera.core.model.WaterStation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Reads water station records from `/waterStations`. */
@Singleton
class StationRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
) {
    /** One-shot fetch of a single station; the result holds null when the record does not exist. */
    suspend fun getStationById(stationId: String): Result<WaterStation?> = try {
        val snapshot = firebaseDatabase.getReference("waterStations").child(stationId).get().await()
        Result.success(snapshot.toStation(stationId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Emits the station on every change, so online/offline edits from the admin site appear live. */
    fun getStationByIdFlow(stationId: String): Flow<WaterStation?> = callbackFlow {
        val ref = firebaseDatabase.getReference("waterStations").child(stationId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toStation(stationId))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Emits the whole station list on every change. Unreadable records are skipped, not fatal. */
    fun getAllStationsFlow(): Flow<List<WaterStation>> = callbackFlow {
        val ref = firebaseDatabase.getReference("waterStations")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { child -> child.toStation(child.key ?: "") })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Deserialises one station record, returning null if it cannot be read.
     *
     * The online flag is written as `online` by the admin site but as `isOnline` by some older
     * records, so it is resolved from the raw map instead of relying on the data class mapping
     * alone. Only the list query used to do this, which could show a station as online in the
     * list while its details screen said offline.
     */
    private fun DataSnapshot.toStation(id: String): WaterStation? = try {
        getValue(WaterStation::class.java)?.copy(id = id)?.apply {
            val raw = value as? Map<*, *>
            isOnline = when (val flag = raw?.get("online") ?: raw?.get("isOnline")) {
                is Boolean -> flag
                is String -> flag.equals("true", ignoreCase = true)
                is Number -> flag.toInt() != 0
                else -> false
            }
        }
    } catch (e: Exception) {
        Log.e("StationRepo", "Failed to parse station[$id]: ${e.message}", e)
        null
    }
}
