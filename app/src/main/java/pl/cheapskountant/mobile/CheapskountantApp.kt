package pl.cheapskountant.mobile

import android.app.Application
import pl.cheapskountant.mobile.data.AppContainer
import pl.cheapskountant.mobile.data.sync.OutboxSync

class CheapskountantApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        OutboxSync.enqueue(this)
    }
}
