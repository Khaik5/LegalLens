package com.example.lagallens.data.model.request

class RefreshTokenRequest(val refreshToken: String)
class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
