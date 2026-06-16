package com.lowderancorp.inioli.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetailItem
import com.lowderancorp.inioli.data.stockjourney.toQuantityProgress
import com.lowderancorp.inioli.ui.ReceiveStockDetailUiState
import com.lowderancorp.inioli.ui.ReceiveStockDetailViewModel
import com.lowderancorp.inioli.ui.components.CenteredLoadingState
import com.lowderancorp.inioli.ui.components.CenteredMessageState
import com.lowderancorp.inioli.ui.components.RetryErrorBanner
import com.lowderancorp.inioli.ui.components.ScreenTopAppBar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val BARCODE_SCANNER_TAG = "ReceiveStockScanner"
private const val BARCODE_CLEAR_DEBOUNCE_MS = 450L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockDetailScreen(
    viewModel: ReceiveStockDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = uiState.detail?.let { detail ->
        "Movement #${detail.id}"
    } ?: "Receive Stock Detail"

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = title,
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

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            BarcodeScannerCard(
                scannedBarcode = uiState.scannedBarcode,
                matchedItem = uiState.matchedItem,
                isScannerArmed = uiState.isScannerArmed,
                totalScannedQuantity = uiState.totalScannedQuantity,
                onBarcodeScanned = onBarcodeScanned,
                onBarcodeCleared = onBarcodeCleared
            )
        }

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

@Composable
private fun BarcodeScannerCard(
    scannedBarcode: String?,
    matchedItem: StockJourneyDetailItem?,
    isScannerArmed: Boolean,
    totalScannedQuantity: Int,
    onBarcodeScanned: (String) -> Unit,
    onBarcodeCleared: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CameraPermissionScanner(
                onBarcodeScanned = onBarcodeScanned,
                onBarcodeCleared = onBarcodeCleared,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            ScanResultPanel(
                scannedBarcode = scannedBarcode,
                matchedItem = matchedItem
            )

            Text(
                text = if (isScannerArmed) {
                    "Ready to scan"
                } else {
                    "Waiting for next item"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$totalScannedQuantity scanned in this session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
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
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
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
                },
                mainExecutor
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@Composable
private fun ScanResultPanel(
    scannedBarcode: String?,
    matchedItem: StockJourneyDetailItem?
) {
    val hasScan = !scannedBarcode.isNullOrBlank()
    val message = when {
        !hasScan -> "No barcode scanned yet."
        matchedItem != null -> "Matched ${matchedItem.barcode.orEmpty()}"
        else -> "No product matched $scannedBarcode"
    }
    val containerColor = if (hasScan && matchedItem != null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (hasScan && matchedItem != null) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodySmall
        )
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
