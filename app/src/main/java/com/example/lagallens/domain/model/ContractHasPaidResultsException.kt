package com.example.lagallens.domain.model

/**
 * Deleting the contract would also delete analysis results the user unlocked with Credit, which is not refunded,
 * so the deletion needs an explicit confirmation.
 */
class ContractHasPaidResultsException(cause: Throwable? = null) :
    IllegalStateException("Contract has analysis results unlocked with Credit", cause)
