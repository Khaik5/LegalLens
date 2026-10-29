package com.example.lagallens.core.network

class BackendNotConfiguredException(feature: String) :
    IllegalStateException("$feature RemoteRepository is unavailable until the Backend API contract is supplied.")
