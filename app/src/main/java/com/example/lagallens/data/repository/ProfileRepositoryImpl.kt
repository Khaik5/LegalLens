package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.ProfileLocalDataSource
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepositoryImpl(private val localDataSource: ProfileLocalDataSource) : ProfileRepository {
    override suspend fun getProfile(): UserProfile = withContext(Dispatchers.IO) { localDataSource.read() }
    override suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile =
        withContext(Dispatchers.IO) { localDataSource.save(name, phone, photoUri) }
    override suspend fun removeAvatar(): UserProfile = withContext(Dispatchers.IO) {
        val current = localDataSource.read()
        localDataSource.save(current.name, current.phone, null)
    }
    override suspend fun clearSessionData(): Boolean = withContext(Dispatchers.IO) { localDataSource.clearSessionData() }
}
