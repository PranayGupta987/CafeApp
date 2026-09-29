package com.cafeapp.feature.auth.domain.usecase

import com.cafeapp.core.common.CafeResult
import com.cafeapp.feature.auth.domain.AuthRepository
import com.cafeapp.feature.auth.domain.AuthUser
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(name: String, email: String, password: String): CafeResult<AuthUser> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isBlank()) {
            return CafeResult.Error(IllegalArgumentException("Name is required"), "Name is required")
        }
        if (trimmedEmail.isBlank()) {
            return CafeResult.Error(IllegalArgumentException("Email is required"), "Email is required")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return CafeResult.Error(IllegalArgumentException("Invalid email format"), "Invalid email format")
        }
        if (password.length < 8) {
            return CafeResult.Error(IllegalArgumentException("Password must be at least 8 characters"), "Password must be at least 8 characters")
        }
        return repository.register(trimmedName, trimmedEmail, password)
    }
}
