package com.cafeapp.feature.auth.domain.usecase

import com.cafeapp.core.common.CafeResult
import com.cafeapp.feature.auth.domain.AuthRepository
import com.cafeapp.feature.auth.domain.AuthUser
import javax.inject.Inject

class GetMeUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): CafeResult<AuthUser> {
        return repository.getMe()
    }
}
