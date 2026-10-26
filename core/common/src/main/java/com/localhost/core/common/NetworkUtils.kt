package com.localhost.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.net.NetworkInterface

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    NONE
}

data class NetworkState(
    val isConnected: Boolean,
    val type: NetworkType,
    val localIpAddress: String?,
    val isVpnActive: Boolean = false
)

data class NetworkDiagnostics(
    val localIp: String?,
    val isConnected: Boolean,
    val type: NetworkType,
    val isVpnActive: Boolean,
    val activeInterfaces: List<String>,
    val proxyHost: String?,
    val proxyPort: Int?
)

object NetworkUtils {
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private val vpnCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Boolean, Long>>()
    private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes

    fun isIpVpn(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val cleanIp = ip.trim().removeSurrounding("[", "]").substringBeforeLast("%")
        val targetIp = if (cleanIp.contains(":") && !cleanIp.contains("::") && cleanIp.count { it == ':' } == 1) {
            cleanIp.substringBefore(":")
        } else {
            cleanIp
        }

        if (targetIp.isEmpty() ||
            targetIp == "127.0.0.1" ||
            targetIp == "localhost" ||
            targetIp == "::1" ||
            targetIp.startsWith("10.") ||
            targetIp.startsWith("192.168.") ||
            ((16..31).any { targetIp.startsWith("172.$it.") }) ||
            targetIp.startsWith("fc00:") ||
            targetIp.startsWith("fe80:")
        ) {
            return false
        }

        val now = System.currentTimeMillis()
        val cached = vpnCache[targetIp]
        if (cached != null && (now - cached.second) < CACHE_TTL_MS) {
            return cached.first
        }

        return try {
            val url = java.net.URL("http://ip-api.com/json/$targetIp?fields=status,message,proxy,hosting")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Localhost-Mobile-VPS")

            if (conn.responseCode == 200) {
                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(respText)
                val isProxy = json.optBoolean("proxy", false)
                val isHosting = json.optBoolean("hosting", false)
                val isVpn = isProxy || isHosting
                vpnCache[targetIp] = Pair(isVpn, now)
                android.util.Log.d("VpnProxy", "VPN check for $targetIp: proxy=$isProxy, hosting=$isHosting => isVpn=$isVpn")
                isVpn
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.w("VpnProxy", "Failed to check VPN for $targetIp: ${e.message}")
            false
        }
    }

    fun isVpnActive(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true
                }
            }
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                if (name.startsWith("tun") || name.startsWith("ppp") || 
                    name.startsWith("tap") || name.startsWith("wg") || 
                    name.startsWith("ipsec") || name.contains("vpn")) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    fun getDiagnostics(context: Context): NetworkDiagnostics {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val vpnActive = isVpnActive(context)
        val isConnected = caps != null && (
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        )
        val type = when {
            caps == null -> NetworkType.NONE
            vpnActive -> NetworkType.VPN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }

        val interfacesList = mutableListOf<String>()
        try {
            val intfs = NetworkInterface.getNetworkInterfaces()
            if (intfs != null) {
                for (it in intfs) {
                    if (it.isUp && !it.isLoopback) {
                        interfacesList.add(it.name)
                    }
                }
            }
        } catch (_: Exception) {}

        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull()

        return NetworkDiagnostics(
            localIp = getLocalIpAddress(),
            isConnected = isConnected,
            type = type,
            isVpnActive = vpnActive,
            activeInterfaces = interfacesList,
            proxyHost = proxyHost,
            proxyPort = proxyPort
        )
    }

    fun observeNetwork(context: Context): Flow<NetworkState> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun sendCurrentState() {
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val vpnActive = isVpnActive(context)
            val isConnected = caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            )
            val type = when {
                caps == null -> NetworkType.NONE
                vpnActive -> NetworkType.VPN
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.NONE
            }
            trySend(NetworkState(isConnected, type, getLocalIpAddress(), vpnActive))
        }

        sendCurrentState()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                sendCurrentState()
            }
            override fun onLost(network: Network) {
                sendCurrentState()
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                sendCurrentState()
            }
        }

        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, callback)

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }
}
