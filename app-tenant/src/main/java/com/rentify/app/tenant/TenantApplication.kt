package com.rentify.app.tenant

import android.app.Application
import com.rentify.app.core.data.local.datastore.DataStoreTokenRefresher
import com.rentify.app.core.network.client.NetworkClient

class TenantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NetworkClient.tokenRefresher = DataStoreTokenRefresher(this)
    }
}
