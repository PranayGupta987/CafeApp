package com.cafeapp.core.network

import com.cafeapp.core.network.dto.OrdersResponseDto
import com.cafeapp.core.network.dto.PlaceOrderRequest
import com.cafeapp.core.network.dto.SingleOrderResponseDto
import com.cafeapp.core.network.dto.UpdateOrderStatusRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrdersApi {
    @GET("orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("tableId") tableId: String? = null,
        @Query("sessionId") sessionId: String? = null,
        @Query("limit") limit: Int? = null,
    ): OrdersResponseDto

    @GET("orders/{id}")
    suspend fun getOrderById(
        @Path("id") id: String
    ): SingleOrderResponseDto

    @POST("orders")
    suspend fun placeOrder(
        @Body request: PlaceOrderRequest
    ): SingleOrderResponseDto

    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body request: UpdateOrderStatusRequest
    ): SingleOrderResponseDto
}
