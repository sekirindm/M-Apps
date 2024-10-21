package com.example.m_apps.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.android.gms.maps.model.LatLng

@Dao
interface MapsDao {

    @Query("SELECT * FROM maps")
    suspend fun getAllMarkers(): List<MapsDb>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapsDb: MapsDb)

    @Delete
    suspend fun delete(mapsDb: MapsDb)
}