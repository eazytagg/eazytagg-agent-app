package com.eazytagg.agent.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.eazytagg.agent.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScanner(modifier: Modifier = Modifier, onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                Text("Camera permission is needed to scan QR codes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            }
        }
        return
    }

    val current by rememberUpdatedState(onCode)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39).build()
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            try { scanner.close() } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var front by remember { mutableStateOf(false) }
    var frontAvailable by remember { mutableStateOf(false) }
    var backAvailable by remember { mutableStateOf(true) }
    var torch by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraError by remember { mutableStateOf("") }
    DisposableEffect(previewView, front, lifecycleOwner) {
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var boundAnalysis: ImageAnalysis? = null
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            if (!disposed) try {
                val provider = future.get()
                boundProvider = provider
                frontAvailable = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                backAvailable = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                boundPreview = preview; boundAnalysis = analysis
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || disposed) proxy.close()
                    else scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes -> if (!disposed) codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { current(it) } }
                        .addOnCompleteListener { proxy.close() }
                }
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                flashAvailable = camera?.cameraInfo?.hasFlashUnit() == true
                torch = false; cameraError = ""
            } catch (_: Exception) { cameraError = "Camera unavailable. Use search or PIN, or select another camera." }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            disposed = true
            camera?.cameraControl?.enableTorch(false)
            boundAnalysis?.clearAnalyzer()
            val uses = listOfNotNull(boundPreview, boundAnalysis).toTypedArray()
            if (uses.isNotEmpty()) boundProvider?.unbind(*uses)
            camera = null
        }
    }
    Box(modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(bottom = 132.dp)) {
            val side = size.minDimension * 0.62f
            val left = (size.width - side) / 2; val top = (size.height - side) / 2
            val length = side * 0.16f
            for (x in listOf(left, left + side)) for (y in listOf(top, top + side)) {
                drawLine(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.geometry.Offset(x,y), androidx.compose.ui.geometry.Offset(x + if(x == left) length else -length,y), 4.dp.toPx())
                drawLine(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.geometry.Offset(x,y), androidx.compose.ui.geometry.Offset(x,y + if(y == top) length else -length), 4.dp.toPx())
            }
            drawLine(androidx.compose.ui.graphics.Color(0xFF00B7FF), androidx.compose.ui.geometry.Offset(left,top+side/2), androidx.compose.ui.geometry.Offset(left+side,top+side/2), 2.dp.toPx())
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xE611151C)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(cameraError.ifBlank { "Point the camera at the guest's QR code" }, color = androidx.compose.ui.graphics.Color.White)
            CameraControls(front, frontAvailable, backAvailable, torch, flashAvailable,
                onFront = { front = it }, onTorch = { enabled ->
                    camera?.cameraControl?.enableTorch(enabled)?.addListener({
                        torch = camera?.cameraInfo?.torchState?.value == androidx.camera.core.TorchState.ON
                    }, ContextCompat.getMainExecutor(ctx))
                })
        }
    }
}

@Composable
internal fun CameraControls(front: Boolean, frontAvailable: Boolean, backAvailable: Boolean, torch: Boolean, flash: Boolean, onFront: (Boolean) -> Unit, onTorch: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.FilterChip(selected = !front, enabled = backAvailable, onClick = { onFront(false) }, label = { Text("Back") }, leadingIcon = { androidx.compose.material3.Icon(painterResource(R.drawable.ic_camera), null, Modifier.size(20.dp)) })
        androidx.compose.material3.FilterChip(selected = front, enabled = frontAvailable, onClick = { onFront(true) }, label = { Text("Front") }, leadingIcon = { androidx.compose.material3.Icon(painterResource(R.drawable.ic_front), null, Modifier.size(20.dp)) })
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.Icon(painterResource(R.drawable.ic_torch), null, Modifier.size(24.dp), tint = androidx.compose.ui.graphics.Color.White)
        Text(if (flash) "Torch" else "Torch unavailable", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = torch && flash, enabled = flash, onCheckedChange = onTorch)
    }
}
