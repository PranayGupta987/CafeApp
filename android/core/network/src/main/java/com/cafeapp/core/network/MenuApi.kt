package com.cafeapp.core.network

import com.cafeapp.core.network.dto.MenuCategoriesResponseDto
import com.cafeapp.core.network.dto.MenuItemsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MenuApi {
    @GET("menu/categories")
    suspend fun getCategories(
        @Query("includeInactive") includeInactive: Boolean? = null
    ): MenuCategoriesResponseDto

    @GET("menu/items")
    suspend fun getMenuItems(
        @Query("categoryId") categoryId: String? = null,
        @Query("isVeg") isVeg: Boolean? = null,
        @Query("isAvailable") isAvailable: Boolean? = null,
        @Query("search") search: String? = null,
    ): MenuItemsResponseDto
}
