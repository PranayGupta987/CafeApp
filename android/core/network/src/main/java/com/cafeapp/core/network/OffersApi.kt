package com.cafeapp.core.network

import com.cafeapp.core.network.dto.OffersResponseDto
import com.cafeapp.core.network.dto.ValidateOfferResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OffersApi {
    @GET("offers")
    suspend fun getActiveOffers(
        @Query("all") all: Boolean? = null
    ): OffersResponseDto

    @GET("offers/{code}/validate")
    suspend fun validateOffer(
        @Path("code") code: String,
        @Query("subtotalPaise") subtotalPaise: Int,
    ): ValidateOfferResponseDto
}
