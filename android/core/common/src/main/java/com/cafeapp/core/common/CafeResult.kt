package com.cafeapp.core.common

sealed interface CafeResult<out T> {
    data class Success<T>(val data: T) : CafeResult<T>
    data class Error(val exception: Throwable, val message: String? = null) : CafeResult<Nothing>
    data object Loading : CafeResult<Nothing>
}
