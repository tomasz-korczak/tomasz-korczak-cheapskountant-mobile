package pl.cheapskountant.mobile.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkStatus(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun online(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
