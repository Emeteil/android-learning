package com.example.android_project.Network

import android.content.Context
import com.example.android_project.Activites.MobileNetworkDataList
import com.example.android_project.DataClasses.LocationData
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient(private val context: Context)
{
    private var webSocket: WebSocket? = null
    private var isConnected = false

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val apiClient = ApiClient(context)

    private var listener: ConnectionListener? = null

    interface ConnectionListener
    {
        fun OnConnected()
        fun OnDisconnected()
        fun OnError(message: String)
        fun OnMessage(message: String)
    }

    fun SetListener(listener: ConnectionListener)
    {
        this.listener = listener
    }

    fun Connect()
    {
        val serverAddress = apiClient.GetServerAddress() ?: run {
            listener?.OnError("config_error")
            return
        }

        val token = apiClient.GetToken() ?: run {
            listener?.OnError("token_expired")
            return
        }

        val url = "ws://$serverAddress/api/mobile-network/ws?token=$token"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener()
        {
            override fun onOpen(webSocket: WebSocket, response: Response)
            {
                isConnected = true
                listener?.OnConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String)
            {
                listener?.OnMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String)
            {
                isConnected = false
                if (code == 1008 || code == 401 || code == 403 || reason.contains("expired", true) || reason.contains("unauthorized", true))
                {
                    listener?.OnError("token_expired")
                }
                else
                {
                    listener?.OnDisconnected()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?)
            {
                isConnected = false
                if (response?.code == 401 || response?.code == 403 || t.message?.contains("401") == true || t.message?.contains("403") == true)
                {
                    listener?.OnError("token_expired")
                }
                else
                {
                    listener?.OnError(t.message ?: "")
                }
            }
        })
    }

    fun Disconnect()
    {
        webSocket?.close(1000, "close")
        webSocket = null
        isConnected = false
    }

    fun IsConnected(): Boolean { return isConnected }

    fun SendData(mobileNetworkDataList: MobileNetworkDataList, locationData: LocationData): Boolean
    {
        if (!isConnected)
        {
            listener?.OnError("isConnected == false")
            return false
        }

        val jsonData = JSONObject()
        jsonData.put("mobile_network_data_list", mobileNetworkDataList.ToJSONObject())
        jsonData.put("location_data", locationData.toJSONObject())

        return webSocket?.send(jsonData.toString()) ?: false
    }
}