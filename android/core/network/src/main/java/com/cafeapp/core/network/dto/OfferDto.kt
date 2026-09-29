package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OfferDto(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String,
    @Json(name = "discountType") val discountType: String, // "PERCENTAGE" or "FLAT_AMOUNT"
    @Json(name = "discountValue") val discountValue: Int, // e.g. 15 for 15% or 10000 for ₹100
    @Json(name = "minOrderAmountPaise") val minOrderAmountPaise: Int = 0,
    @Json(name = "maxDiscountAmountPaise") val maxDiscountAmountPaise: Int? = null,
    @Json(name = "validFrom") val validFrom: String,
    @Json(name = "validTo") val validTo: String,
    @Json(name = "isActive") val isActive: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class OffersResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<OfferDto>? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class ValidateOfferDataDto(
    @Json(name = "code") val code: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "discountType") val discountType: String,
    @Json(name = "discountValue") val discountValue: Int,
    @Json(name = "discountPaise") val discountPaise: Int,
)

@JsonClass(generateAdapter = true)
data class ValidateOfferResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "valid") val valid: Boolean = false,
    @Json(name = "data") val data: ValidateOfferDataDto? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "discountPaise") val discountPaise: Int? = 0,
)
