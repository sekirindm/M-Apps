package com.example.m_apps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
@Entity(tableName = "maps")
data class MapsDb(
    @PrimaryKey
    val latLng: LatLng,
    val name: String = ""
)