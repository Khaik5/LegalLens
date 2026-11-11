package com.example.lagallens.data.model.request

/** `phone = null` clears the number; the backend accepts Vietnamese numbers only. */
data class UpdateMyProfileRequest(val name: String, val phone: String?)
