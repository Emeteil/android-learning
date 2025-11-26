package com.example.android_project.DataClasses

import org.json.JSONObject

data class LocationData(val latitude: Double, val longitude: Double, val altitude: Double, val time: Long)
{
    fun toJSONObject(): JSONObject
    {
        val data = JSONObject()
        data.put("Latitude", latitude)
        data.put("Longitude", longitude)
        data.put("Altitude", altitude)
        data.put("Time", time)

        return data
    }

    override fun toString(): String
    {
        return "Lat: $latitude\nLon: $longitude\nAlt: $altitude\nTime: $time"
    }
}