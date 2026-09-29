package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class CafeDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "address") val address: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String,
    @Json(name = "gstin") val gstin: String? = null,
    @Json(name = "gstPercent") val gstPercent: String? = "5.00",
    @Json(name = "currency") val currency: String = "INR",
    @Json(name = "timezone") val timezone: String = "Asia/Kolkata",
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "openingTime") val openingTime: String,
    @Json(name = "closingTime") val closingTime: String,
    @Json(name = "isAcceptingOrders") val isAcceptingOrders: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class CafeResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: CafeDto? = null,
    @Json(name = "message") val message: String? = null,
)
