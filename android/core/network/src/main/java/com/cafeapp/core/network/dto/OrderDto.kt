package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderItemRequest(
    @Json(name = "menuItemId") val menuItemId: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "notes") val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlaceOrderRequest(
    @Json(name = "tableId") val tableId: String? = null,
    @Json(name = "tableNumber") val tableNumber: Int? = null,
    @Json(name = "items") val items: List<OrderItemRequest>,
    @Json(name = "offerCode") val offerCode: String? = null,
    @Json(name = "specialInstructions") val specialInstructions: String? = null,
)

@JsonClass(generateAdapter = true)
data class OrderItemMenuItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "pricePaise") val pricePaise: Int? = null,
    @Json(name = "isVeg") val isVeg: Boolean? = true,
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "menuItemId") val menuItemId: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "unitPricePaise") val unitPricePaise: Int,
    @Json(name = "subtotalPaise") val subtotalPaise: Int,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "menuItem") val menuItem: OrderItemMenuItemDto? = null,
)

@JsonClass(generateAdapter = true)
data class OrderTableSummaryDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "tableNumber") val tableNumber: Int,
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: String,
    @Json(name = "orderNumber") val orderNumber: Int,
    @Json(name = "sessionId") val sessionId: String? = null,
    @Json(name = "userId") val userId: String,
    @Json(name = "tableId") val tableId: String? = null,
    @Json(name = "status") val status: String = "PLACED", // PLACED, ACCEPTED, PREPARING, READY, SERVED, COMPLETED, CANCELLED
    @Json(name = "subtotalPaise") val subtotalPaise: Int,
    @Json(name = "discountPaise") val discountPaise: Int = 0,
    @Json(name = "taxPaise") val taxPaise: Int = 0,
    @Json(name = "totalPaise") val totalPaise: Int,
    @Json(name = "specialInstructions") val specialInstructions: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "items") val items: List<OrderItemDto> = emptyList(),
    @Json(name = "table") val table: OrderTableSummaryDto? = null,
)

@JsonClass(generateAdapter = true)
data class OrdersResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<OrderDto>? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class SingleOrderResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: OrderDto? = null,
    @Json(name = "message") val message: String? = null,
)
