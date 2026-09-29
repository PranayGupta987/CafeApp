package com.cafeapp.core.network

import com.cafeapp.core.network.dto.CreateReservationRequest
import com.cafeapp.core.network.dto.ReservationsResponseDto
import com.cafeapp.core.network.dto.SingleReservationResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationsApi {
    @GET("reservations")
    suspend fun getReservations(
        @Query("status") status: String? = null
    ): ReservationsResponseDto

    @GET("reservations/{id}")
    suspend fun getReservationById(
        @Path("id") id: String
    ): SingleReservationResponseDto

    @POST("reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): SingleReservationResponseDto

    @DELETE("reservations/{id}")
    suspend fun cancelReservation(
        @Path("id") id: String
    ): SingleReservationResponseDto
}
