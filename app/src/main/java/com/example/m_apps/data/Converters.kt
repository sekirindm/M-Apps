package com.example.m_apps.data

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng

class Converters {

    @TypeConverter
    fun fromLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }

    @TypeConverter
    fun toLatLng(latLngString: String): LatLng {
        val values = latLngString.split(",")
        return LatLng(values[0].toDouble(), values[1].toDouble())
    }
}