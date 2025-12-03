package com.example.android_project.Network

import android.content.Context
import androidx.preference.PreferenceManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context)
{
    companion object
    {
        private const val BASE_URL_KEY = "server_address"
        private const val TOKEN_KEY = "auth_token"
        private const val NICKNAME_KEY = "user_nickname"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun SaveServerAddress(address: String)
    {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(BASE_URL_KEY, address)
            .apply()
    }

    fun GetServerAddress(): String?
    {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(BASE_URL_KEY, null)
    }

    fun SaveAuthData(token: String?, nickname: String)
    {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(TOKEN_KEY, token)
            .putString(NICKNAME_KEY, nickname)
            .apply()
    }

    fun GetToken(): String?
    {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(TOKEN_KEY, null)
    }

    fun GetNickname(): String?
    {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(NICKNAME_KEY, null)
    }

    fun ClearAuthData()
    {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(TOKEN_KEY)
            .remove(NICKNAME_KEY)
            .apply()
    }

    fun IsLoggedIn(): Boolean
    {
        return GetToken() != null
    }

    fun Register(nickname: String, password: String, callback: (Boolean, String?) -> Unit)
    {
        val serverAddress = GetServerAddress() ?: run {
            callback(false, "Сервер не настроен")
            return
        }

        val json = JSONObject()
            .put("nickname", nickname)
            .put("password", password)

        val request = Request.Builder()
            .url("http://$serverAddress/api/authorization/register")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback
        {
            override fun onFailure(call: Call, e: IOException)
            {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response)
            {
                val body = response.body?.string()
                if (response.isSuccessful && body != null)
                {
                    val jsonResponse = JSONObject(body)
                    if (jsonResponse.getString("status") == "success")
                    {
                        val data = jsonResponse.getJSONObject("data")
                        val token = response.headers["Set-Cookie"]?.substringAfter("token=")?.substringBefore(";")

                        SaveAuthData(token, data["nickname"] as String)
                        callback(true, "Регистрация успешна")
                    }
                    else
                    {
                        callback(false, jsonResponse.getJSONObject("error").getString("message"))
                    }
                }
                else if (response.code == 400)
                {
                    Login(nickname, password, callback)
                }
                else
                {
                    callback(false, response.code.toString())
                }
            }
        })
    }

    fun Login(nickname: String, password: String, callback: (Boolean, String?) -> Unit)
    {
        val serverAddress = GetServerAddress() ?: run {
            callback(false, "Сервер не настроен")
            return
        }

        val json = JSONObject()
            .put("nickname", nickname)
            .put("password", password)

        val request = Request.Builder()
            .url("http://$serverAddress/api/authorization/login")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback
        {
            override fun onFailure(call: Call, e: IOException)
            {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response)
            {
                val body = response.body?.string()
                if (response.isSuccessful && body != null)
                {
                    val jsonResponse = JSONObject(body)
                    if (jsonResponse.getString("status") == "success")
                    {
                        val data = jsonResponse.getJSONObject("data")
                        val token = response.headers["Set-Cookie"]?.substringAfter("token=")?.substringBefore(";")

                        SaveAuthData(token, data["nickname"] as String)
                        callback(true, "Вход успешен")
                    }
                    else
                    {
                        callback(false, jsonResponse.getJSONObject("error").getString("message"))
                    }
                }
                else
                {
                    callback(false, response.code.toString())
                }
            }
        })
    }

    fun CheckLogin(callback: (Boolean, String?) -> Unit)
    {
        val serverAddress = GetServerAddress() ?: run {
            callback(false, "Сервер не настроен")
            return
        }

        val token = GetToken() ?: run {
            callback(false, "Токен не найден")
            return
        }

        val request = Request.Builder()
            .url("http://$serverAddress/api/admin/loged_ping")
            .addHeader("Cookie", "token=$token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback
        {
            override fun onFailure(call: Call, e: IOException)
            {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response)
            {
                if (response.isSuccessful)
                {
                    callback(true, "Авторизация подтверждена")
                }
                else if (response.code == 401)
                {
                    ClearAuthData()
                    callback(false, "Требуется повторная авторизация")
                }
                else
                {
                    callback(false, response.code.toString())
                }
            }
        })
    }
}