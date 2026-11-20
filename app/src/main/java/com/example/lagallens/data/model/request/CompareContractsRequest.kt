package com.example.lagallens.data.model.request

/** The app reads the text per clause, so it asks the backend to leave out the repeated full text. */
data class CompareContractsRequest(val firstId: Long, val secondId: Long, val includeContent: Boolean = false)
