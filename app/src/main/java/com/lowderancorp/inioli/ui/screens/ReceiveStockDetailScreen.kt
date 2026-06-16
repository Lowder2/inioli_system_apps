package com.lowderancorp.inioli.ui.screens

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.Camera
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lowderancorp.inioli.data.stockjourney.CloseStockJourneyPreview
import com.lowderancorp.inioli.data.stockjourney.CloseStockJourneyPreviewItem
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetailItem
import com.lowderancorp.inioli.data.stockjourney.toQuantityProgress
import com.lowderancorp.inioli.ui.ReceiveStockDetailUiState
import com.lowderancorp.inioli.ui.ReceiveStockDetailViewModel
import com.lowderancorp.inioli.ui.components.CenteredLoadingState
import com.lowderancorp.inioli.ui.components.CenteredMessageState
import com.lowderancorp.inioli.ui.components.RetryErrorBanner
import com.lowderancorp.inioli.ui.components.ScreenTopAppBar
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val BARCODE_SCANNER_TAG = "ReceiveStockScanner"
private const val BARCODE_CLEAR_DEBOUNCE_MS = 450L
private val ScannerViewportShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockDetailScreen(
    viewModel: ReceiveStockDetailViewModel,
    onBackClick: () -> Unit,
    onCloseSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = uiState.detail?.let { detail ->
        "Movement #${detail.id}"
    } ?: "Receive Stock Detail"
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator.release()
        }
    }

    LaunchedEffect(uiState.closeSuccessToken) {
        if (uiState.closeSuccessToken > 0) {
            onCloseSuccess()
        }
    }

    LaunchedEffect(uiState.scanAcceptedToneToken) {
        if (uiState.scanAcceptedToneToken > 0) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        }
    }

    LaunchedEffect(uiState.overscanToneToken) {
        if (uiState.overscanToneToken > 0) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        }
    }

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = title,
                onBackClick = onBackClick,
                actions = {
                    TextButton(
                        onClick = { showResetDialog = true },
                        enabled = uiState.canReset
                    ) {
                        Text("Reset")
                    }
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isLoading && !uiState.isClosing
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.detail != null) {
                SubmitBottomBar(
                    canSubmit = uiState.canClose,
                    isSubmitting = uiState.isClosing,
                    onSubmitClick = { showSubmitDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading || uiState.isClosing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading && uiState.detail == null -> {
                    CenteredLoadingState(
                        message = "Loading stock movement detail...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                !uiState.errorMessage.isNullOrBlank() && uiState.detail == null -> {
                    CenteredMessageState(
                        title = "Unable to load movement detail",
                        message = uiState.errorMessage,
                        actionLabel = "Try Again",
                        onActionClick = viewModel::refresh,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                else -> {
                    ReceiveStockDetailContent(
                        uiState = uiState,
                        onBarcodeScanned = viewModel::onBarcodeScanned,
                        onBarcodeCleared = viewModel::onBarcodeCleared,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    val closePreview = uiState.closePreview
    if (showSubmitDialog && closePreview != null) {
        CloseMovementConfirmationDialog(
            closePreview = closePreview,
            closeNotes = uiState.closeNotes,
            closeErrorMessage = uiState.closeErrorMessage,
            isClosing = uiState.isClosing,
            onNotesChange = viewModel::onCloseNotesChange,
            onDismissRequest = {
                if (!uiState.isClosing) {
                    showSubmitDialog = false
                }
            },
            onConfirmClick = viewModel::closeStockJourney
        )
    }

    if (showResetDialog) {
        ResetMovementConfirmationDialog(
            onDismissRequest = { showResetDialog = false },
            onConfirmClick = {
                showResetDialog = false
                viewModel.resetMovementDraft()
            }
        )
    }

    uiState.pendingOverscanItem?.let { pendingOverscanItem ->
        OverscanWarningDialog(
            item = pendingOverscanItem,
            onDismissRequest = {
                viewModel.dismissOverscanWarning()
            },
            onConfirmClick = { muteForProductInSession ->
                viewModel.confirmOverscanWarning(
                    muteForProductInSession = muteForProductInSession
                )
            }
        )
    }
}

@Composable
private fun ReceiveStockDetailContent(
    uiState: ReceiveStockDetailUiState,
    onBarcodeScanned: (String) -> Unit,
    onBarcodeCleared: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = uiState.detail ?: return
    val errorMessage = uiState.errorMessage

    Column(modifier = modifier) {
        BarcodeScannerCard(
            onBarcodeScanned = onBarcodeScanned,
            onBarcodeCleared = onBarcodeCleared,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!errorMessage.isNullOrBlank()) {
                item {
                    RetryErrorBanner(
                        message = errorMessage,
                        onRetryClick = onRetryClick
                    )
                }
            }

            item {
                Text(
                    text = "${detail.items.size} product${if (detail.items.size == 1) "" else "s"} in this movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = detail.items,
                key = { item -> item.id }
            ) { item ->
                DetailItemCard(
                    item = item,
                    isMatched = item.id == uiState.matchedItemId,
                    scannedQuantity = uiState.scannedQuantityByItemId[item.id] ?: 0
                )
            }
        }
    }
}

@Composable
private fun BarcodeScannerCard(
    onBarcodeScanned: (String) -> Unit,
    onBarcodeCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        CameraPermissionScanner(
            onBarcodeScanned = onBarcodeScanned,
            onBarcodeCleared = onBarcodeCleared,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SubmitBottomBar(
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onSubmitClick: () -> Unit
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSubmitClick,
                enabled = canSubmit && !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit")
            }

            if (!canSubmit) {
                Text(
                    text = "Scan at least one matching product to enable submission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InlineErrorMessage(
    message: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CloseMovementConfirmationDialog(
    closePreview: CloseStockJourneyPreview,
    closeNotes: String,
    closeErrorMessage: String?,
    isClosing: Boolean,
    onNotesChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Submit Movement")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${closePreview.items.size} product${if (closePreview.items.size == 1) "" else "s"} will be submitted with ${closePreview.totalSubmittedQuantityDisplay} total quantity.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (closePreview.hasOverscannedQuantities) {
                    Text(
                        text = "Some items exceed the required quantity and will be submitted as overscanned quantities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = closeNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isClosing,
                    minLines = 3,
                    label = { Text("Close Notes") },
                    placeholder = { Text("Optional close notes") }
                )

                if (!closeErrorMessage.isNullOrBlank()) {
                    InlineErrorMessage(message = closeErrorMessage)
                }

                closePreview.items.forEach { item ->
                    CloseMovementConfirmationItem(item = item)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                enabled = !isClosing
            ) {
                Text(if (isClosing) "Submitting..." else "Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isClosing
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ResetMovementConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Reset movement progress?")
        },
        text = {
            Text(
                text = "This will clear all scanned quantities, close notes, and overscan warning preferences for this movement."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OverscanWarningDialog(
    item: StockJourneyDetailItem,
    onDismissRequest: () -> Unit,
    onConfirmClick: (Boolean) -> Unit
) {
    var muteForProductInSession by rememberSaveable(item.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Product already fully scanned")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${item.barcode ?: item.productCode} has already reached the required scan quantity for this movement."
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = muteForProductInSession,
                        onCheckedChange = { isChecked ->
                            muteForProductInSession = isChecked
                        }
                    )
                    Text(
                        text = "Do not show this warning again for this product in this session.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmClick(muteForProductInSession)
                }
            ) {
                Text("Count Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CloseMovementConfirmationItem(
    item: CloseStockJourneyPreviewItem
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = item.productCode,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = item.productName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (item.isOverscanned) {
                val requiredText = item.requiredQtyDisplay?.let { requiredQty ->
                    " (required $requiredQty)"
                }.orEmpty()
                "Submit ${item.receivedQtyDisplay}$requiredText."
            } else {
                "Submit ${item.receivedQtyDisplay}."
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CameraPermissionScanner(
    onBarcodeScanned: (String) -> Unit,
    onBarcodeCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    if (hasCameraPermission) {
        BarcodeScannerPreview(
            onBarcodeScanned = onBarcodeScanned,
            onBarcodeCleared = onBarcodeCleared,
            modifier = modifier
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Camera access is needed to scan barcodes for this movement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("Enable Camera")
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeScannerPreview(
    onBarcodeScanned: (String) -> Unit,
    onBarcodeCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcodeScanned = rememberUpdatedState(onBarcodeScanned)
    val currentOnBarcodeCleared = rememberUpdatedState(onBarcodeCleared)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isTorchEnabled by rememberSaveable { mutableStateOf(false) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    LaunchedEffect(camera, isTorchEnabled) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            cameraExecutor,
                            StockBarcodeAnalyzer(
                                barcodeScanner = barcodeScanner,
                                onBarcodeScanned = { barcode ->
                                    currentOnBarcodeScanned.value(barcode)
                                },
                                onBarcodeCleared = {
                                    currentOnBarcodeCleared.value()
                                }
                            )
                        )
                    }

                try {
                    cameraProvider.unbindAll()
                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    camera = boundCamera
                    hasFlashUnit = boundCamera.cameraInfo.hasFlashUnit()
                } catch (exception: Exception) {
                    Log.e(BARCODE_SCANNER_TAG, "Unable to bind barcode scanner camera.", exception)
                }
            },
            mainExecutor
        )

        onDispose {
            cameraProviderFuture.addListener(
                {
                    runCatching {
                        cameraProviderFuture.get().unbindAll()
                    }
                    camera = null
                    hasFlashUnit = false
                },
                mainExecutor
            )
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(camera) {
                detectTapGestures { tapOffset ->
                    val activeCamera = camera ?: return@detectTapGestures
                    val focusPoint = previewView.meteringPointFactory.createPoint(
                        tapOffset.x,
                        tapOffset.y
                    )
                    activeCamera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            focusPoint,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                        )
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                    )
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .height(108.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                )
        )

        if (hasFlashUnit) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.52f),
                contentColor = Color.White
            ) {
                IconButton(
                    onClick = {
                        isTorchEnabled = !isTorchEnabled
                    }
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) {
                            Icons.Filled.FlashOn
                        } else {
                            Icons.Filled.FlashOff
                        },
                        contentDescription = if (isTorchEnabled) {
                            "Turn flashlight off"
                        } else {
                            "Turn flashlight on"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanResultPanel(
    scannedBarcode: String?,
    matchedItem: StockJourneyDetailItem?
) {
    val hasScan = !scannedBarcode.isNullOrBlank()
    val title = when {
        !hasScan -> "Scanner ready"
        matchedItem != null -> "Barcode matched"
        else -> "Barcode not found"
    }
    val detail = when {
        !hasScan -> "Aim the camera at a barcode from this movement to start scanning."
        matchedItem != null -> {
            val itemCode = matchedItem.barcode ?: matchedItem.productCode
            "$itemCode • ${matchedItem.productName}"
        }
        else -> "$scannedBarcode is not part of this movement."
    }
    val containerColor = if (hasScan && matchedItem != null) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (hasScan) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (hasScan && matchedItem != null) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (hasScan) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ScannerGuideOverlay(
    isScannerArmed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(14.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.TopStart),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            contentColor = Color.White
        ) {
            Text(
                text = if (isScannerArmed) "Ready" else "Hold",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.76f)
                .height(88.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.48f),
            contentColor = Color.White
        ) {
            Text(
                text = if (isScannerArmed) {
                    "Place barcode inside the frame"
                } else {
                    "Move the barcode away to scan again"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScannerSessionSummary(
    isScannerArmed: Boolean,
    totalScannedQuantity: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScannerSummaryChip(
            label = "Status",
            value = if (isScannerArmed) "Ready" else "Waiting",
            modifier = Modifier.weight(1f)
        )
        ScannerSummaryChip(
            label = "Session",
            value = "$totalScannedQuantity scanned",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScannerSummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailItemCard(
    item: StockJourneyDetailItem,
    isMatched: Boolean,
    scannedQuantity: Int
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMatched) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.barcode ?: item.productCode,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium
            )

            QuantityProgressPanel(
                item = item,
                scannedQuantity = scannedQuantity
            )
        }
    }
}

@Composable
private fun QuantityProgressPanel(
    item: StockJourneyDetailItem,
    scannedQuantity: Int
) {
    val progress = item.toQuantityProgress(scannedQuantity = scannedQuantity)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuantityValue(
                    label = "Need",
                    value = progress.requiredDisplay,
                    modifier = Modifier.weight(1f)
                )
                QuantityValue(
                    label = "Scanned",
                    value = progress.scannedDisplay,
                    modifier = Modifier.weight(1f)
                )
                QuantityValue(
                    label = "Remaining",
                    value = progress.remainingDisplay,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = progress.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (progress.isOverScanned) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun QuantityValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private class StockBarcodeAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val onBarcodeScanned: (String) -> Unit,
    private val onBarcodeCleared: () -> Unit
) : ImageAnalysis.Analyzer {
    private val isProcessing = AtomicBoolean(false)
    private var barcodeClearStartedAt: Long? = null
    private var hasReportedClear = true

    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull { candidate ->
                    !candidate.rawValue.isNullOrBlank()
                }?.rawValue
                if (!barcode.isNullOrBlank()) {
                    barcodeClearStartedAt = null
                    hasReportedClear = false
                    onBarcodeScanned(barcode)
                } else {
                    handleBarcodeMissing()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(BARCODE_SCANNER_TAG, "Unable to scan barcode.", exception)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun handleBarcodeMissing() {
        val now = SystemClock.elapsedRealtime()
        val clearStartedAt = barcodeClearStartedAt ?: now.also {
            barcodeClearStartedAt = it
        }

        if (!hasReportedClear && now - clearStartedAt >= BARCODE_CLEAR_DEBOUNCE_MS) {
            hasReportedClear = true
            onBarcodeCleared()
        }
    }
}
