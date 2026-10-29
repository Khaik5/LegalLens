package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.ProfileLocalDataSource
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ProfileRepository

class ProfileRepositoryImpl(private val localDataSource: ProfileLocalDataSource) : ProfileRepository {
    override fun getProfile(): UserProfile = localDataSource.read()
    override fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile = localDataSource.save(name, phone, photoUri)
    override fun clearSessionData(): Boolean = localDataSource.clearSessionData()
}
