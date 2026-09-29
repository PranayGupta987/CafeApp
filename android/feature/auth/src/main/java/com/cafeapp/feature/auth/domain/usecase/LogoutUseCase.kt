package com.cafeapp.feature.auth.domain.usecase

import com.cafeapp.core.common.CafeResult
import com.cafeapp.feature.auth.domain.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): CafeResult<Unit> {
        return repository.logout()
    }
}
