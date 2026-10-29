package com.example.lagallens.data.datasource.local.datastore

/** Generation changes on every login, including logging back into the same account. */
data class DemoSessionScope(val userId: String, val generation: String)
