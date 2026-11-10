package com.example.lagallens.domain.model.auth

enum class ChangePasswordResult {
    Updated,
    UpdatedSignInRequired,
    IncorrectCurrentPassword,
    RateLimited,
    Failed
}
