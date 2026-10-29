package com.example.lagallens.domain.model

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String = "",
    val photoUri: String? = null
)
