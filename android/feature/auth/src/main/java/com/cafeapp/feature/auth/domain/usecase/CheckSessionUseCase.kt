package com.cafeapp.feature.auth.domain.usecase

import com.cafeapp.feature.auth.domain.AuthRepository
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Boolean {
        return repository.checkSession()
    }
}
