package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.UserResponse
import com.example.lagallens.domain.model.UserProfile

internal fun UserResponse.toProfile(phone: String, photoUri: String?) =
    UserProfile(name = name, email = email, phone = phone, photoUri = photoUri)
