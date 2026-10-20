package br.com.fiap.letssign

import android.app.Application
import br.com.fiap.letssign.data.local.LetsSignDatabase
import br.com.fiap.letssign.data.repository.LetsSignRepository
import br.com.fiap.letssign.data.repository.LocalLetsSignRepositoryImpl

class LetsSignApplication : Application() {

    val database: LetsSignDatabase by lazy {
        LetsSignDatabase.getInstance(this)
    }

    val repository: LetsSignRepository by lazy {
        LocalLetsSignRepositoryImpl(database.letsSignDao())
    }
}
