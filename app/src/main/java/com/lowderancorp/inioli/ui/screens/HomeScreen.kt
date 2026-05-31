package com.lowderancorp.inioli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String,
    onLogoutClick: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onSaleClick: () -> Unit,
    onReturnClick: () -> Unit,
    onStockAdjustmentClick: () -> Unit
) {
    val homeActions = listOf(
        HomeAction(
            title = "Receive Stock",
            icon = Icons.Filled.SyncAlt,
            onClick = onReceiveStockClick
        ),
        HomeAction(
            title = "Sale",
            icon = Icons.Filled.PointOfSale,
            onClick = onSaleClick
        ),
        HomeAction(
            title = "Return",
            icon = Icons.AutoMirrored.Filled.AssignmentReturn,
            onClick = onReturnClick
        ),
        HomeAction(
            title = "Stock Adjustment",
            icon = Icons.Filled.Tune,
            onClick = onStockAdjustmentClick
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome back, $username",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Choose a transaction to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardSpacing = 16.dp
                val cardSize = (maxWidth - cardSpacing) / 2

                Column(
                    verticalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    homeActions.chunked(2).forEach { rowActions ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                        ) {
                            rowActions.forEach { action ->
                                HomeActionCard(
                                    action = action,
                                    modifier = Modifier.size(cardSize)
                                )
                            }

                            if (rowActions.size < 2) {
                                Spacer(modifier = Modifier.width(cardSize))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    action: HomeAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = action.onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class HomeAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
