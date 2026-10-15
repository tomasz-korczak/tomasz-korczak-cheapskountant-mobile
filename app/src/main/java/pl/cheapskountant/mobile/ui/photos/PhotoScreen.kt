package pl.cheapskountant.mobile.ui.photos

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.AppContainer
import pl.cheapskountant.mobile.data.local.ReceiptPhotoStore
import pl.cheapskountant.mobile.data.repository.IncomingPhoto
import pl.cheapskountant.mobile.domain.OutboxTransitions
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenReview: (String, String) -> Unit,
) {
    val viewModel: PhotoViewModel = viewModel(factory = PhotoViewModel.factory(container.receipts))
    val waiting by viewModel.waiting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var camera by remember { mutableStateOf(false) }
    val noPhotos = R.string.no_photos
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (viewModel.selected.size >= 5) {
            viewModel.add(IncomingPhoto("", "", byteArrayOf()), rejected = false, overLimit = true, limitMessage = R.string.photo_limit, ok = false)
            return@rememberLauncherForActivityResult
        }
        val type = context.contentResolver.getType(uri) ?: ""
        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "gallery.jpg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val allowed = type == "image/jpeg" || type == "image/png" || type == "image/webp" || type == "image/jpg"
        val fits = bytes.size <= ReceiptPhotoStore.MAX_BYTES || BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null
        viewModel.add(
            IncomingPhoto(name, if (type.isBlank()) "image/jpeg" else type, bytes),
            rejected = !allowed,
            overLimit = false,
            limitMessage = R.string.photo_limit,
            ok = allowed && fits,
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera = true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt_photo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.same_receipt))
            Text(stringResource(R.string.photo_count, viewModel.selected.size))
            TextButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.take_photo))
            }
            TextButton(onClick = {
                if (viewModel.selected.size >= 5) {
                    viewModel.add(IncomingPhoto("", "", byteArrayOf()), false, true, R.string.photo_limit, false)
                } else {
                    gallery.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }) { Text(stringResource(R.string.choose_gallery)) }
            viewModel.selected.forEachIndexed { index, photo ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val bitmap = remember(photo.bytes) { BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size) }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(photo.displayName)
                        Text("${photo.bytes.size} B")
                    }
                    TextButton(onClick = { viewModel.remove(index) }) { Text(stringResource(R.string.remove_photo)) }
                }
            }
            viewModel.message?.let { Text(stringResource(it)) }
            if (viewModel.reading) Text(stringResource(R.string.reading_receipt))
            Button(
                onClick = { viewModel.save({ id -> onOpenReview(id, "direct") }, noPhotos, R.string.photo_limit) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
            if (waiting.isNotEmpty()) {
                Text(stringResource(R.string.waiting_receipts), modifier = Modifier.padding(top = 16.dp))
                waiting.forEach { row ->
                    Text(stringResource(receiptStatus(row.status)))
                    if (row.status == OutboxTransitions.READY) {
                        TextButton(onClick = { onOpenReview(row.localId, "queued") }) { Text(stringResource(R.string.edit)) }
                    }
                    if (row.status != OutboxTransitions.SENDING) {
                        TextButton(onClick = { viewModel.delete(row.localId) }) { Text(stringResource(R.string.delete)) }
                    }
                    if (row.status == OutboxTransitions.ERROR) {
                        TextButton(onClick = { viewModel.resubmit(row.localId) }) { Text(stringResource(R.string.resubmit)) }
                    }
                }
            }
        }
    }
    if (camera) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { camera = false }) {
            CameraCapture(onDismiss = { camera = false }) { bytes ->
            camera = false
            if (viewModel.selected.size >= 5) {
                viewModel.add(IncomingPhoto("", "", byteArrayOf()), false, true, R.string.photo_limit, false)
            } else {
                val name = "receipt-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".jpg"
                viewModel.add(IncomingPhoto(name, "image/jpeg", bytes), false, false, R.string.photo_limit, bytes.size <= ReceiptPhotoStore.MAX_BYTES)
            }
        }
        }
    }
}

@Composable
private fun CameraCapture(onDismiss: () -> Unit, onCaptured: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val capture = remember { ImageCapture.Builder().build() }
    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).also { previewView ->
                    val future = ProcessCameraProvider.getInstance(viewContext)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    }, ContextCompat.getMainExecutor(viewContext))
                }
            },
            modifier = Modifier.fillMaxWidth().height(360.dp),
        )
        Button(onClick = {
            val file = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onCaptured(file.readBytes())
                        file.delete()
                    }
                    override fun onError(exception: ImageCaptureException) {
                        onDismiss()
                    }
                },
            )
        }) { Text(stringResource(R.string.take_photo)) }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) }
    }
}

private fun receiptStatus(status: String): Int = when (status) {
    OutboxTransitions.READY -> R.string.ready_to_edit
    OutboxTransitions.ERROR -> R.string.error
    else -> R.string.waiting_to_send
}
