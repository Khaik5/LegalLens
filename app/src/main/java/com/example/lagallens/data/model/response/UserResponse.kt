package com.example.lagallens.data.model.response

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null,
    /** Path of the authenticated avatar endpoint, or null when the account has no avatar. */
    val avatar: String? = null,
    val isActive: Boolean,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String
)
