package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TableDto(
    @Json(name = "id") val id: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "capacity") val capacity: Int,
    @Json(name = "status") val status: String = "AVAILABLE",
    @Json(name = "qrToken") val qrToken: String,
    @Json(name = "isActive") val isActive: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class TablesResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<TableDto>? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class SingleTableResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: TableDto? = null,
    @Json(name = "message") val message: String? = null,
)
