package com.example.lagallens.data.model.request

data class CreateContractRequest(val title: String, val description: String? = null, val type: String = "KHAC")

data class UpdateContractRequest(val title: String, val description: String?)
