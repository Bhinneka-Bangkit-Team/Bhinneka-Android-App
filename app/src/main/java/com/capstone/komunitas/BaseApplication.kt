package com.capstone.komunitas

import android.app.Application
import com.capstone.komunitas.data.db.AppDatabase
import com.capstone.komunitas.data.network.BackendApi
import com.capstone.komunitas.data.network.NetworkConnectionInterceptor
import com.capstone.komunitas.data.repositories.UserRepository
import com.capstone.komunitas.ui.auth.AuthViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class BaseApplication: Application(), KodeinAware {
    override val kodein = Kodein.lazy {
        import(androidXModule(this@BaseApplication))

        bind() from this.singleton { NetworkConnectionInterceptor(this.instance()) }
        bind() from this.singleton { BackendApi(this.instance()) }
        bind() from this.singleton { AppDatabase(this.instance()) }
        bind() from this.singleton { UserRepository(this.instance(), this.instance()) }
        bind() from this.provider { AuthViewModelFactory(this.instance()) }

    }
}