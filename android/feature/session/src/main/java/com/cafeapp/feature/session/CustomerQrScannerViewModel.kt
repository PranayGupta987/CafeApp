package com.cafeapp.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.datastore.SessionPreferences
import com.cafeapp.core.network.TableSessionsApi
import com.cafeapp.core.network.TablesApi
import com.cafeapp.core.network.dto.CheckInRequest
import com.cafeapp.core.network.dto.TableDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrScannerUiState(
    val isScanning: Boolean = true,
    val manualCodeInput: String = "",
    val isLoading: Boolean = false,
    val detectedTable: TableDto? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class CustomerQrScannerViewModel @Inject constructor(
    private val tablesApi: TablesApi,
    private val tableSessionsApi: TableSessionsApi,
    private val sessionPreferences: SessionPreferences,
    private val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    fun onManualCodeChanged(code: String) {
        _uiState.update { it.copy(manualCodeInput = code, errorMessage = null) }
    }

    fun onQrScanned(rawContent: String) {
        // Parse token if full URL or raw token
        val token = when {
            rawContent.contains("/qr/") -> rawContent.substringAfterLast("/qr/")
            rawContent.contains("/table/") -> rawContent.substringAfterLast("/table/")
            rawContent.contains("token=") -> rawContent.substringAfter("token=").substringBefore("&")
            else -> rawContent.trim()
        }

        verifyAndSelectTable(token)
    }

    fun submitManualEntry() {
        val input = _uiState.value.manualCodeInput.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a table number or QR code") }
            return
        }

        val tableNum = input.toIntOrNull()
        if (tableNum != null) {
            // Find table by number
            lookupTableByNumber(tableNum)
        } else {
            verifyAndSelectTable(input)
        }
    }

    private fun verifyAndSelectTable(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = tablesApi.getTableByQrToken(token)
                val table = response.data
                if (table != null) {
                    saveTableSelection(table)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Invalid QR code. Table not found."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not find table for this code. Please verify."
                    )
                }
            }
        }
    }

    private fun lookupTableByNumber(tableNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = tablesApi.getTables()
                val tables = response.data ?: emptyList()
                val matched = tables.find { it.tableNumber == tableNumber }
                if (matched != null) {
                    saveTableSelection(matched)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Table $tableNumber does not exist. Available tables: 1-12"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to lookup table"
                    )
                }
            }
        }
    }

    private suspend fun saveTableSelection(table: TableDto) {
        try {
            tableSessionsApi.checkIn(CheckInRequest(tableId = table.id, guestCount = 1))
        } catch (_: Exception) {
            // Already active or table session created
        }
        sessionPreferences.saveTable(table.id, table.tableNumber)
        cartManager.setTable(table.id, table.tableNumber)
        _uiState.update {
            it.copy(
                isLoading = false,
                detectedTable = table,
                isSuccess = true,
                errorMessage = null
            )
        }
    }

    fun resetScanner() {
        _uiState.update {
            QrScannerUiState()
        }
    }
}
