package com.example.lagallens.data.model.response

/** [reauthenticationRequired] is true: the backend ends every session after the email changes. */
data class EmailChangeConfirmationResponse(val email: String, val reauthenticationRequired: Boolean)
