package com.rentify.app.landlord

import android.app.Application
import com.rentify.app.core.data.local.datastore.DataStoreTokenRefresher
import com.rentify.app.core.network.client.NetworkClient

class LandlordApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NetworkClient.tokenRefresher = DataStoreTokenRefresher(this)
    }
}
