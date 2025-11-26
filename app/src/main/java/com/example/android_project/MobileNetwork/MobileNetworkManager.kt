package com.example.android_project.Activites

import android.Manifest
import android.content.Context
import android.telephony.TelephonyManager
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.CellIdentityNr
import androidx.annotation.RequiresPermission

class MobileNetworkManager(private val context: Context)
{
    private val telephonyManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun GetMobileNetworkData(): MobileNetworkDataList
    {
        val networkDataList = mutableListOf<MobileNetworkData>()
        val cellInfoList: List<CellInfo> = telephonyManager.allCellInfo

        for (cellInfo in cellInfoList)
        {
            if (cellInfo is CellInfoLte)
            {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                val bands = cellIdentity.bands.joinToString(" ") { it.toString() }

                val networkData = MobileNetworkData(
                    networkType = "CellInfoLte",
                    cellIdentity = cellIdentity.ci.toString(),
                    mcc = cellIdentity.mccString,
                    mnc = cellIdentity.mncString,
                    pci = cellIdentity.pci,
                    tac = cellIdentity.tac,
                    bands = bands,
                    signalStrength = "RSRP: ${signalStrength.rsrp}",
                    rsrp = signalStrength.rsrp,
                    rsrq = signalStrength.rsrq,
                    rssi = signalStrength.rssi,
                    timingAdvance = signalStrength.timingAdvance,
                    time = System.currentTimeMillis()
                )
                networkDataList.add(networkData)
            }
            else if (cellInfo is CellInfoGsm)
            {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                val networkData = MobileNetworkData(
                    networkType = "CellInfoGsm",
                    cellIdentity = cellIdentity.cid.toString(),
                    mcc = cellIdentity.mccString,
                    mnc = cellIdentity.mncString,
                    pci = cellIdentity.psc,
                    tac = cellIdentity.lac,
                    bands = "",
                    signalStrength = "DBM: ${signalStrength.dbm}",
                    rsrp = signalStrength.dbm,
                    rsrq = 0,
                    rssi = signalStrength.rssi,
                    timingAdvance = signalStrength.timingAdvance,
                    time = System.currentTimeMillis()
                )
                networkDataList.add(networkData)
            }
            else if (cellInfo is CellInfoNr)
            {
                val cellIdentity = cellInfo.cellIdentity as CellIdentityNr
                val signalStrength = cellInfo.cellSignalStrength as CellSignalStrengthNr

                val bands = cellIdentity.bands.joinToString(" ") { it.toString() }

                val networkData = MobileNetworkData(
                    networkType = "CellInfoNr",
                    cellIdentity = cellIdentity.nci.toString(),
                    mcc = cellIdentity.mccString,
                    mnc = cellIdentity.mncString,
                    pci = cellIdentity.pci,
                    tac = cellIdentity.tac,
                    bands = bands,
                    signalStrength = "SS-RSRP: ${signalStrength.ssRsrp}",
                    rsrp = signalStrength.ssRsrp,
                    rsrq = signalStrength.ssRsrq,
                    rssi = 0,
                    timingAdvance = signalStrength.timingAdvanceMicros,
                    time = System.currentTimeMillis()
                )
                networkDataList.add(networkData)
            }
            else
            {
                val networkData = MobileNetworkData(
                    networkType = "NoName",
                    cellIdentity = "",
                    mcc = "",
                    mnc = "",
                    pci = 0,
                    tac = 0,
                    bands = "",
                    signalStrength = "",
                    rsrp = 0,
                    rsrq = 0,
                    rssi = 0,
                    timingAdvance = 0,
                    time = System.currentTimeMillis()
                )
                networkDataList.add(networkData)
            }
        }

        return MobileNetworkDataList(networkDataList)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun GetDetailedNetworkInfo(): String
    {
        val networkDataList = GetMobileNetworkData()
        return networkDataList.toString()
    }
}