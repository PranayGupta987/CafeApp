package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckInRequest(
    @Json(name = "qrToken") val qrToken: String? = null,
    @Json(name = "tableId") val tableId: String? = null,
    @Json(name = "guestCount") val guestCount: Int? = 1,
)

@JsonClass(generateAdapter = true)
data class TableSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "tableId") val tableId: String,
    @Json(name = "hostUserId") val hostUserId: String,
    @Json(name = "guestCount") val guestCount: Int = 1,
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "table") val table: TableDto? = null,
)

@JsonClass(generateAdapter = true)
data class TableSessionResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: TableSessionDto? = null,
    @Json(name = "message") val message: String? = null,
)
