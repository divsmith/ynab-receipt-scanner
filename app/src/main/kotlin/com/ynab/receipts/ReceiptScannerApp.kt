package com.ynab.receipts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ynab.receipts.feature.auth.AuthScreen
import com.ynab.receipts.feature.history.HistoryScreen
import com.ynab.receipts.feature.review.ReviewScreen
import com.ynab.receipts.feature.scan.ScanScreen

private enum class Destination(val route: String, val label: String) {
    Scan("scan", "Scan"),
    Review("review", "Review"),
    Auth("auth", "Auth"),
    History("history", "History")
}

@Composable
fun ReceiptScannerApp(viewModel: ReceiptScannerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val tabs = listOf(Destination.Scan, Destination.Review, Destination.Auth, Destination.History)
                tabs.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination.isTopLevelDestination(destination.route),
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(destination.label) },
                        icon = { Text(destination.label.take(1)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Scan.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Destination.Scan.route) {
                ScanScreen(
                    onCaptureImage = { imageBytes ->
                        viewModel.captureReceipt(imageBytes)
                        navController.navigate(Destination.Review.route)
                    }
                )
            }
            composable(Destination.Review.route) {
                val state = viewModel.uiState
                ReviewScreen(
                    payeeInput = state.reviewPayeeInput,
                    amountInput = state.reviewAmountInput,
                    dateInput = state.reviewDateInput,
                    currency = state.currency,
                    confidence = state.confidence,
                    ocrContextLines = state.ocrContextLines,
                    onPayeeChange = viewModel::updateReviewPayee,
                    onAmountChange = viewModel::updateReviewAmount,
                    onDateChange = viewModel::updateReviewDate,
                    onConfirm = { viewModel.confirmAndQueue() }
                )
            }
            composable(Destination.Auth.route) {
                AuthScreen(
                    isConnected = viewModel.uiState.isConnected,
                    onConnect = { viewModel.connectYnab() },
                    onDisconnect = { viewModel.disconnectYnab() }
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    queuedCount = viewModel.uiState.queuedCount,
                    failedCount = viewModel.uiState.failedCount
                )
            }
        }
    }
}

private fun NavDestination?.isTopLevelDestination(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}
