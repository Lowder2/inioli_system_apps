package com.lowderancorp.inioli.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowderancorp.inioli.data.stockjourney.MovementType
import com.lowderancorp.inioli.data.stockjourney.StockJourneyItem
import com.lowderancorp.inioli.data.stockjourney.formatQuantityOrDefault
import com.lowderancorp.inioli.ui.ReceiveStockUiState
import com.lowderancorp.inioli.ui.ReceiveStockViewModel
import com.lowderancorp.inioli.ui.components.CenteredLoadingState
import com.lowderancorp.inioli.ui.components.CenteredMessageState
import com.lowderancorp.inioli.ui.components.LoadingPanel
import com.lowderancorp.inioli.ui.components.RetryErrorBanner
import com.lowderancorp.inioli.ui.components.ScreenTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockScreen(
    viewModel: ReceiveStockViewModel,
    onStockJourneyClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = "Receive Stock",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading && uiState.movementTypes.isEmpty() -> {
                    CenteredLoadingState(
                        message = "Loading stock movements...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                !uiState.errorMessage.isNullOrBlank() && uiState.movementTypes.isEmpty() -> {
                    CenteredMessageState(
                        title = "Unable to load movement types",
                        message = uiState.errorMessage,
                        actionLabel = "Try Again",
                        onActionClick = viewModel::refresh,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                else -> {
                    ReceiveStockList(
                        uiState = uiState,
                        onStockJourneyClick = onStockJourneyClick,
                        onMovementTypeSelected = viewModel::onMovementTypeSelected,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiveStockList(
    uiState: ReceiveStockUiState,
    onStockJourneyClick: (Int) -> Unit,
    onMovementTypeSelected: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMovementType = uiState.selectedMovementType

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Movement Type",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            MovementTypeSelector(
                movementTypes = uiState.movementTypes,
                selectedMovementTypeCode = uiState.selectedMovementTypeCode,
                isLoading = uiState.isLoading,
                onMovementTypeSelected = onMovementTypeSelected
            )
        }

        item {
            Text(
                text = buildMovementSummary(
                    selectedMovementType = selectedMovementType,
                    itemCount = uiState.items.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val errorMessage = uiState.errorMessage
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                item {
                    LoadingPanel(
                        message = "Loading stock movements for the selected type..."
                    )
                }
            }

            !errorMessage.isNullOrBlank() && uiState.items.isEmpty() -> {
                item {
                    RetryErrorBanner(
                        message = errorMessage,
                        onRetryClick = onRetryClick
                    )
                }
            }

            uiState.items.isEmpty() -> {
                item {
                    EmptyMovementPanel(
                        movementType = selectedMovementType,
                        onRefreshClick = onRetryClick
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank() && uiState.items.isNotEmpty()) {
            item {
                RetryErrorBanner(
                    message = errorMessage,
                    onRetryClick = onRetryClick
                )
            }
        }

        if (uiState.items.isNotEmpty()) {
            items(
                items = uiState.items,
                key = { item -> item.id }
            ) { item ->
                StockJourneyCard(
                    item = item,
                    onClick = { onStockJourneyClick(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MovementTypeSelector(
    movementTypes: List<MovementType>,
    selectedMovementTypeCode: String,
    isLoading: Boolean,
    onMovementTypeSelected: (String) -> Unit
) {
    val selectedMovementType = movementTypes.firstOrNull { movementType ->
        movementType.code == selectedMovementTypeCode
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (!isLoading && movementTypes.isNotEmpty()) {
                expanded = shouldExpand
            }
        }
    ) {
        OutlinedTextField(
            value = selectedMovementType?.code.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = !isLoading && movementTypes.isNotEmpty()
                ),
            readOnly = true,
            enabled = !isLoading && movementTypes.isNotEmpty(),
            label = { Text("Movement Type") },
            placeholder = { Text("Select movement type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            movementTypes.forEach { movementType ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = movementType.code)
                            Text(
                                text = movementType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onMovementTypeSelected(movementType.code)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockJourneyCard(
    item: StockJourneyItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Movement #${item.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.createdAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = item.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${item.sourceLocationCode} -> ${item.destinationLocationCode}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${item.sourceLocationName} to ${item.destinationLocationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val notes = item.notes
            if (!notes.isNullOrBlank()) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(label = "Qty ${item.totalQty.formatQuantityOrDefault()}")
                SummaryChip(label = "${item.itemCount} item${if (item.itemCount == 1) "" else "s"}")
                item.totalReceivedQty?.let { receivedQty ->
                    SummaryChip(label = "Received ${receivedQty.formatQuantityOrDefault()}")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SummaryChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun EmptyMovementPanel(
    movementType: MovementType?,
    onRefreshClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No stock movements found",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = movementType?.let {
                    "There are no movements available for ${it.code.lowercase().replace('_', ' ')} right now."
                } ?: "There are no movements available right now.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRefreshClick) {
                Text("Refresh")
            }
        }
    }
}

private fun buildMovementSummary(
    selectedMovementType: MovementType?,
    itemCount: Int
): String {
    val movementLabel = selectedMovementType?.code ?: "the selected type"
    val movementWord = if (itemCount == 1) "movement" else "movements"
    return "$itemCount $movementWord loaded for $movementLabel."
}
