package com.example.android_project.Activites

import org.json.JSONArray
import org.json.JSONObject

data class MobileNetworkData(
    val networkType: String,
    val cellIdentity: String,
    val mcc: String?,
    val mnc: String?,
    val pci: Int,
    val tac: Int,
    val bands: String,
    val signalStrength: String,
    val rsrp: Int,
    val rsrq: Int,
    val rssi: Int,
    val timingAdvance: Int,
    val time: Long
)
{
    fun ToJSONObject(): JSONObject
    {
        val data = JSONObject()
        data.put("NetworkType", networkType)
        data.put("CellIdentity", cellIdentity)
        data.put("MCC", mcc)
        data.put("MNC", mnc)
        data.put("PCI", pci)
        data.put("TAC", tac)
        data.put("Bands", bands)
        data.put("SignalStrength", signalStrength)
        data.put("RSRP", rsrp)
        data.put("RSRQ", rsrq)
        data.put("RSSI", rssi)
        data.put("TimingAdvance", timingAdvance)
        data.put("Time", time)

        return data
    }

    override fun toString(): String
    {
        var result = ""
        if (networkType == "CellInfoLte")
        {
            result += "CellInfoLte:\n"
            result += "  CellIdentityLte:\n"
            result += "    Bands: $bands\n"
            result += "    CellIdentity: $cellIdentity\n"
            result += "    MCC: $mcc\n"
            result += "    MNC: $mnc\n"
            result += "    PCI: $pci\n"
            result += "    TAC: $tac\n"
            result += "  CellSignalStrengthLte:\n"
            result += "    RSRP: $rsrp\n"
            result += "    RSRQ: $rsrq\n"
            result += "    RSSI: $rssi\n"
            result += "    Timing Advance: $timingAdvance\n\n"
        }
        else if (networkType == "CellInfoGsm")
        {
            result += "CellInfoGsm\n"
            result += "  CellIdentityGsm:\n"
            result += "    CellIdentity: $cellIdentity\n"
            result += "    MCC: $mcc\n"
            result += "    MNC: $mnc\n"
            result += "    PCI: $pci\n"
            result += "    TAC: $tac\n"
            result += "  CellSignalStrengthGsm:\n"
            result += "    DBM: $rsrp\n"
            result += "    RSSI: $rssi\n"
            result += "    Timing Advance: $timingAdvance\n\n"
        }
        else if (networkType == "CellInfoNr")
        {
            result += "CellInfoNr:\n"
            result += "  CellIdentityNr:\n"
            result += "    Bands: $bands\n"
            result += "    NCI: $cellIdentity\n"
            result += "    PCI: $pci\n"
            result += "    TAC: $tac\n"
            result += "    MCC: $mcc\n"
            result += "    MNC: $mnc\n"
            result += "  CellSignalStrengthNr:\n"
            result += "    SS-RSRP: $rsrp\n"
            result += "    SS-RSRQ: $rsrq\n"
            result += "    Timing Advance: $timingAdvance\n"
        }
        else
        {
            result += "NoName\n\n"
        }
        return result
    }
}

data class MobileNetworkDataList(val networks: List<MobileNetworkData>)
{
    fun ToJSONObject(): JSONObject
    {
        val data = JSONObject()
        val jsonArray = JSONArray()

        for (network in networks)
        {
            jsonArray.put(network.ToJSONObject())
        }

        data.put("MobileNetworks", jsonArray)
        return data
    }

    override fun toString(): String
    {
        var result = ""
        for (network in networks)
        {
            result += network.toString()
        }
        return result
    }
}