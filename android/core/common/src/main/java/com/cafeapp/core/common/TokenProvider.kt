package com.cafeapp.core.common

interface TokenProvider {
    fun getAccessToken(): String?
}
