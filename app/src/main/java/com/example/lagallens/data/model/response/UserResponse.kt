package com.example.lagallens.data.model.response

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val isActive: Boolean,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String
)
