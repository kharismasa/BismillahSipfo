package com.example.bismillahsipfo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.bismillahsipfo.BuildConfig

object DebugHelper {

    private const val TAG = "DebugHelper"

    fun logApiInfo(context: Context) {
        val info = """
            === API CONFIGURATION ===
            Base URL: ${BuildConfig.BASE_URL}
            API Key exists: ${BuildConfig.API_KEY.isNotEmpty()}
            API Key length: ${BuildConfig.API_KEY.length}
            Build Type: ${BuildConfig.BUILD_TYPE}
            Debug: ${BuildConfig.DEBUG}
            ========================
        """.trimIndent()

        Log.d(TAG, info)

        // Show toast in debug builds or when debugging is enabled
        if (BuildConfig.DEBUG) {
//            Toast.makeText(context, "Check logs for API info", Toast.LENGTH_SHORT).show()
        }
    }

    fun logNetworkRequest(tag: String, url: String, method: String = "GET") {
        Log.d(tag, "🌐 Network Request: $method $url")
    }

    fun logNetworkResponse(tag: String, url: String, success: Boolean, responseCode: Int = -1, message: String = "") {
        val status = if (success) "✅ SUCCESS" else "❌ FAILED"
        Log.d(tag, "🌐 Network Response: $status [$responseCode] $url - $message")
    }

    fun logDatabaseQuery(tag: String, operation: String, table: String, result: String = "") {
        Log.d(tag, "🗃️ Database: $operation on $table - $result")
    }

    fun testNetworkConnectivity(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            Log.d(TAG, "📱 Network connected (API ${Build.VERSION.SDK_INT}): $isConnected")

            if (BuildConfig.DEBUG && !isConnected) {
//                Toast.makeText(context, "No network connection!", Toast.LENGTH_LONG).show()
            }

            isConnected
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo
            val isConnected = activeNetwork?.isConnectedOrConnecting == true

            Log.d(TAG, "📱 Network connected (Legacy API): $isConnected")

            if (BuildConfig.DEBUG && !isConnected) {
//                Toast.makeText(context, "No network connection!", Toast.LENGTH_LONG).show()
            }

            isConnected
        }
    }

    fun testApiEndpoint(context: Context) {
        Log.d(TAG, "🔍 Testing API endpoint: ${BuildConfig.BASE_URL}")

        if (BuildConfig.BASE_URL.isEmpty()) {
            Log.e(TAG, "❌ BASE_URL is empty!")
            if (BuildConfig.DEBUG) {
//                Toast.makeText(context, "BASE_URL is empty!", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (BuildConfig.API_KEY.isEmpty()) {
            Log.e(TAG, "❌ API_KEY is empty!")
            if (BuildConfig.DEBUG) {
//                Toast.makeText(context, "API_KEY is empty!", Toast.LENGTH_LONG).show()
            }
            return
        }

        Log.d(TAG, "✅ API configuration looks good")

        if (BuildConfig.DEBUG) {
//            Toast.makeText(context, "API config OK - Check logs", Toast.LENGTH_SHORT).show()
        }
    }
}