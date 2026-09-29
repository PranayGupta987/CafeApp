package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateReservationRequest(
    @Json(name = "partySize") val partySize: Int,
    @Json(name = "reservationTime") val reservationTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int = 90,
    @Json(name = "specialRequests") val specialRequests: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReservationTableSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "capacity") val capacity: Int,
)

@JsonClass(generateAdapter = true)
data class ReservationDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "tableId") val tableId: String? = null,
    @Json(name = "partySize") val partySize: Int,
    @Json(name = "reservationTime") val reservationTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int = 90,
    @Json(name = "status") val status: String = "PENDING", // PENDING, CONFIRMED, SEATED, COMPLETED, CANCELLED, NO_SHOW, REJECTED
    @Json(name = "specialRequests") val specialRequests: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "table") val table: ReservationTableSummaryDto? = null,
)

@JsonClass(generateAdapter = true)
data class ReservationsResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<ReservationDto>? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class SingleReservationResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: ReservationDto? = null,
    @Json(name = "message") val message: String? = null,
)
