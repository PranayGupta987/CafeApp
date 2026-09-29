package com.cafeapp.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.Discount
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.ui.components.CafeBottomNavItem
import com.cafeapp.core.ui.components.CafeBottomNavigationBar
import com.cafeapp.feature.cafe.CustomerHomeScreen
import com.cafeapp.feature.cafe.CustomerProfileScreen
import com.cafeapp.feature.cart.CustomerCartScreen
import com.cafeapp.feature.menu.CustomerMenuScreen
import com.cafeapp.feature.offers.CustomerOffersScreen
import com.cafeapp.feature.orders.CustomerOrdersScreen
import com.cafeapp.feature.reservations.CustomerReservationsScreen
import com.cafeapp.feature.session.CustomerQrScannerScreen

enum class CustomerTab(val route: String, val label: String) {
    HOME("home", "Home"),
    MENU("menu", "Menu"),
    OFFERS("offers", "Offers"),
    ORDERS("orders", "Orders"),
    PROFILE("profile", "Profile")
}

enum class CustomerSubScreen {
    NONE,
    CART,
    RESERVATIONS,
    QR_SCANNER
}

@Composable
fun CustomerMainScreen(
    viewModel: CustomerMainViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cartManager = viewModel.cartManager
    var currentTab by remember { mutableStateOf(CustomerTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(CustomerSubScreen.NONE) }
    val cartState by cartManager.cartState.collectAsStateWithLifecycle()

    val navItems = listOf(
        CafeBottomNavItem(
            route = CustomerTab.HOME.route,
            label = CustomerTab.HOME.label,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
        ),
        CafeBottomNavItem(
            route = CustomerTab.MENU.route,
            label = CustomerTab.MENU.label,
            selectedIcon = Icons.Filled.RestaurantMenu,
            unselectedIcon = Icons.Outlined.RestaurantMenu,
            badgeCount = cartState.totalItemCount,
        ),
        CafeBottomNavItem(
            route = CustomerTab.OFFERS.route,
            label = CustomerTab.OFFERS.label,
            selectedIcon = Icons.Filled.Discount,
            unselectedIcon = Icons.Outlined.Discount,
        ),
        CafeBottomNavItem(
            route = CustomerTab.ORDERS.route,
            label = CustomerTab.ORDERS.label,
            selectedIcon = Icons.Filled.Receipt,
            unselectedIcon = Icons.Outlined.Receipt,
        ),
        CafeBottomNavItem(
            route = CustomerTab.PROFILE.route,
            label = CustomerTab.PROFILE.label,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
        ),
    )

    if (currentSubScreen != CustomerSubScreen.NONE) {
        when (currentSubScreen) {
            CustomerSubScreen.CART -> {
                CustomerCartScreen(
                    onNavigateToMenu = {
                        currentSubScreen = CustomerSubScreen.NONE
                        currentTab = CustomerTab.MENU
                    },
                    onNavigateToOffers = {
                        currentSubScreen = CustomerSubScreen.NONE
                        currentTab = CustomerTab.OFFERS
                    },
                    onNavigateToQrScanner = {
                        currentSubScreen = CustomerSubScreen.QR_SCANNER
                    },
                    onNavigateToOrders = {
                        currentSubScreen = CustomerSubScreen.NONE
                        currentTab = CustomerTab.ORDERS
                    },
                    onNavigateBack = {
                        currentSubScreen = CustomerSubScreen.NONE
                    }
                )
            }
            CustomerSubScreen.RESERVATIONS -> {
                CustomerReservationsScreen(
                    onNavigateBack = {
                        currentSubScreen = CustomerSubScreen.NONE
                    }
                )
            }
            CustomerSubScreen.QR_SCANNER -> {
                CustomerQrScannerScreen(
                    onNavigateToMenu = {
                        currentSubScreen = CustomerSubScreen.NONE
                        currentTab = CustomerTab.MENU
                    },
                    onNavigateBack = {
                        currentSubScreen = CustomerSubScreen.NONE
                    }
                )
            }
            CustomerSubScreen.NONE -> {}
        }
    } else {
        Scaffold(
            bottomBar = {
                CafeBottomNavigationBar(
                    items = navItems,
                    currentRoute = currentTab.route,
                    onNavigateToRoute = { route ->
                        CustomerTab.values().find { it.route == route }?.let {
                            currentTab = it
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = currentTab, label = "CustomerTabCrossfade") { tab ->
                    when (tab) {
                        CustomerTab.HOME -> {
                            CustomerHomeScreen(
                                onNavigateToMenu = { currentTab = CustomerTab.MENU },
                                onNavigateToOffers = { currentTab = CustomerTab.OFFERS },
                                onNavigateToReservations = { currentSubScreen = CustomerSubScreen.RESERVATIONS },
                                onNavigateToQrScanner = { currentSubScreen = CustomerSubScreen.QR_SCANNER },
                                onNavigateToCart = { currentSubScreen = CustomerSubScreen.CART },
                                onNavigateToOrders = { currentTab = CustomerTab.ORDERS },
                            )
                        }
                        CustomerTab.MENU -> {
                            CustomerMenuScreen(
                                onNavigateToCart = { currentSubScreen = CustomerSubScreen.CART },
                            )
                        }
                        CustomerTab.OFFERS -> {
                            CustomerOffersScreen()
                        }
                        CustomerTab.ORDERS -> {
                            CustomerOrdersScreen(
                                onNavigateToMenu = { currentTab = CustomerTab.MENU },
                            )
                        }
                        CustomerTab.PROFILE -> {
                            CustomerProfileScreen(
                                onNavigateToQrScanner = { currentSubScreen = CustomerSubScreen.QR_SCANNER },
                                onLogout = onLogout,
                            )
                        }
                    }
                }
            }
        }
    }
}
