package com.cafeapp.feature.auth.domain.usecase

import com.cafeapp.core.common.CafeResult
import com.cafeapp.feature.auth.domain.AuthRepository
import com.cafeapp.feature.auth.domain.AuthUser
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): CafeResult<AuthUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return CafeResult.Error(IllegalArgumentException("Email is required"), "Email is required")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return CafeResult.Error(IllegalArgumentException("Invalid email format"), "Invalid email format")
        }
        if (password.isBlank()) {
            return CafeResult.Error(IllegalArgumentException("Password is required"), "Password is required")
        }
        return repository.login(trimmedEmail, password)
    }
}
