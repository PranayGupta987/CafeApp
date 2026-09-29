package com.cafeapp.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MenuCategoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "displayOrder") val displayOrder: Int = 0,
    @Json(name = "isActive") val isActive: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class MenuItemCategorySummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
)

@JsonClass(generateAdapter = true)
data class MenuItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "categoryId") val categoryId: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "pricePaise") val pricePaise: Int,
    @Json(name = "preparationTimeMinutes") val preparationTimeMinutes: Int = 15,
    @Json(name = "isAvailable") val isAvailable: Boolean = true,
    @Json(name = "isVeg") val isVeg: Boolean = true,
    @Json(name = "isSpicy") val isSpicy: Boolean = false,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "displayOrder") val displayOrder: Int = 0,
    @Json(name = "category") val category: MenuItemCategorySummaryDto? = null,
)

@JsonClass(generateAdapter = true)
data class MenuCategoriesResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<MenuCategoryDto>? = null,
    @Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class MenuItemsResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<MenuItemDto>? = null,
    @Json(name = "message") val message: String? = null,
)
