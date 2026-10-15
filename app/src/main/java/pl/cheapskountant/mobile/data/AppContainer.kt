package pl.cheapskountant.mobile.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pl.cheapskountant.mobile.data.local.AppDatabase
import pl.cheapskountant.mobile.data.local.ReceiptPhotoStore
import pl.cheapskountant.mobile.data.remote.ApiClient
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.data.repository.ExpenseRepository
import pl.cheapskountant.mobile.data.repository.LandingRepository
import pl.cheapskountant.mobile.data.repository.ReceiptRepository
import pl.cheapskountant.mobile.data.sync.NetworkStatus

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = AppDatabase.create(context)
    val api = ApiClient.create()
    val network = NetworkStatus(context)
    private val remote = RemoteCall()
    private val photos = ReceiptPhotoStore(context)
    val landing = LandingRepository(database, api, remote, network)
    val expenses = ExpenseRepository(context, database, api, remote, network)
    val receipts = ReceiptRepository(context, database, photos, api, remote, network)
}
