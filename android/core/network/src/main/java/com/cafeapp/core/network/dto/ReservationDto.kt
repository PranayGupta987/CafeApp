package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateReservationRequest(
    @Json(name = "partySize") val partySize: Int,
    @Json(name = "reservationTime") val reservationTime: String,
    @Json(name = "tableId") val tableId: String? = null,
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
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "table") val table: ReservationTableSummaryDto? = null,
)

@JsonClass(generateAdapter = true)
data class AvailableTableDto(
    @Json(name = "id") val id: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "capacity") val capacity: Int,
    @Json(name = "status") val status: String? = null,
    @Json(name = "isAvailable") val isAvailable: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class TimeSlotDto(
    @Json(name = "time") val time: String,
    @Json(name = "isoString") val isoString: String,
    @Json(name = "availableTablesCount") val availableTablesCount: Int = 0,
    @Json(name = "isAvailable") val isAvailable: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class AvailabilityDataDto(
    @Json(name = "date") val date: String? = null,
    @Json(name = "partySize") val partySize: Int = 2,
    @Json(name = "durationMinutes") val durationMinutes: Int = 90,
    @Json(name = "requestedTime") val requestedTime: String? = null,
    @Json(name = "availableTables") val availableTables: List<AvailableTableDto> = emptyList(),
    @Json(name = "suitableTablesCount") val suitableTablesCount: Int = 0,
    @Json(name = "timeSlots") val timeSlots: List<TimeSlotDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AvailabilityResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: AvailabilityDataDto? = null,
    @Json(name = "message") val message: String? = null,
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
