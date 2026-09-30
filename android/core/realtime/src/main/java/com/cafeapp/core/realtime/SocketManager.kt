package com.cafeapp.core.realtime

import android.util.Log
import com.cafeapp.core.datastore.SessionPreferences
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class OrderNewEvent(
    val orderId: String,
    val orderNumber: Int,
    val tableNumber: Int?,
    val itemsCount: Int,
    val status: String,
    val createdAt: String?,
)

data class OrderStatusUpdateEvent(
    val orderId: String,
    val orderNumber: Int,
    val status: String,
)

private const val TAG = "SocketManager"
private const val SERVER_URL = "http://10.0.2.2:3000"

@Singleton
class SocketManager @Inject constructor(
    private val sessionPreferences: SessionPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _orderNewEvents = MutableSharedFlow<OrderNewEvent>(extraBufferCapacity = 64)
    val orderNewEvents: SharedFlow<OrderNewEvent> = _orderNewEvents.asSharedFlow()

    private val _orderStatusUpdates = MutableSharedFlow<OrderStatusUpdateEvent>(extraBufferCapacity = 64)
    val orderStatusUpdates: SharedFlow<OrderStatusUpdateEvent> = _orderStatusUpdates.asSharedFlow()

    fun connect(explicitToken: String? = null) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            return
        }

        try {
            val token = explicitToken ?: sessionPreferences.getAccessTokenBlocking()

            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 2000
                timeout = 10000
                if (!token.isNullOrBlank()) {
                    auth = mapOf("token" to token)
                }
            }

            val socketInstance = IO.socket(URI.create(SERVER_URL), opts)
            this.socket = socketInstance

            socketInstance.on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "Socket.IO connected successfully")
                _isConnected.value = true
            }

            socketInstance.on(Socket.EVENT_DISCONNECT) { args ->
                Log.w(TAG, "Socket.IO disconnected: ${args.firstOrNull()}")
                _isConnected.value = false
            }

            socketInstance.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket.IO connect error: ${args.firstOrNull()}")
                _isConnected.value = false
            }

            socketInstance.on("order:new") { args ->
                try {
                    val data = args.firstOrNull()
                    if (data is JSONObject) {
                        val event = OrderNewEvent(
                            orderId = data.optString("orderId", ""),
                            orderNumber = data.optInt("orderNumber", 0),
                            tableNumber = if (data.has("tableNumber") && !data.isNull("tableNumber")) data.optInt("tableNumber") else null,
                            itemsCount = data.optInt("itemsCount", 0),
                            status = data.optString("status", "PLACED"),
                            createdAt = data.optString("createdAt", null),
                        )
                        Log.d(TAG, "Received order:new -> $event")
                        scope.launch { _orderNewEvents.emit(event) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse order:new event", e)
                }
            }

            socketInstance.on("order:status_update") { args ->
                try {
                    val data = args.firstOrNull()
                    if (data is JSONObject) {
                        val event = OrderStatusUpdateEvent(
                            orderId = data.optString("orderId", ""),
                            orderNumber = data.optInt("orderNumber", 0),
                            status = data.optString("status", ""),
                        )
                        Log.d(TAG, "Received order:status_update -> $event")
                        scope.launch { _orderStatusUpdates.emit(event) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse order:status_update event", e)
                }
            }

            socketInstance.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Socket.IO client", e)
            _isConnected.value = false
        }
    }

    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
            socket = null
            _isConnected.value = false
            Log.i(TAG, "Socket.IO disconnected and cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting Socket.IO", e)
        }
    }
}
