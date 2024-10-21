package com.example.m_apps.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.m_apps.data.AppDatabase
import com.example.m_apps.data.MapsDb
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class MapsViewModel(application: Application) : AndroidViewModel(application){

    private val mapsDao = AppDatabase.getInstance(application).mapsDao()

    private val _mapsDbLiveData = MutableLiveData<List<MapsDb>>()
    val mapsDbLiveData: LiveData<List<MapsDb>> get() = _mapsDbLiveData

    fun insertMarker(latLng: LatLng, name: String) {
        viewModelScope.launch {
            val mapsDb = MapsDb(latLng = latLng, name = name)
            mapsDao.insert(mapsDb)
        }
    }

    fun getAllMarkers() {
        viewModelScope.launch {
            val map = mapsDao.getAllMarkers()
            _mapsDbLiveData.value = map
        }
    }

    fun deleteMarker(latLng: LatLng, name: String) {
        viewModelScope.launch {
            val mapsDb = MapsDb(latLng = latLng, name = name)
            mapsDao.delete(mapsDb)
        }
    }
}