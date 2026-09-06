package com.example.lagallens.data.datasource.local.datastore

import com.example.lagallens.data.model.response.LocalAuthAccount

class AuthLocalDataSource {
    fun getAccount(): LocalAuthAccount {
        return LocalAuthAccount(username = "Admin", password = "123456")
    }
}
