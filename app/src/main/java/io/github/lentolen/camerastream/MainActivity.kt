package io.github.lentolen.camerastream

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import io.github.lentolen.camerastream.ui.theme.CameraStreamTheme
import java.util.concurrent.atomic.AtomicLong

class CameraViewModel : ViewModel() {
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
}

class MainActivity : ComponentActivity() {
    private val cameraViewModel by viewModels<CameraViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        setContent {
            CameraStreamTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Screen(cameraViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Screen(cameraViewModel: CameraViewModel) {
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context)
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier) {
            CameraPreview(controller = controller, modifier = Modifier.fillMaxSize(), cameraViewModel = cameraViewModel)
        }
    } else {
        if (cameraPermissionState.status.shouldShowRationale) {
            Column (
                Modifier
                    .fillMaxSize()
                    .padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.camera_permission_prompt), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text(stringResource(R.string.request_permission_btn))
                }
            }
        } else {
            LaunchedEffect(cameraPermissionState) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
        if (!cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale) {
            Column (
                Modifier
                    .fillMaxSize()
                    .padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.camera_access_required))
            }

        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier, cameraViewModel: CameraViewModel) {
    val lastTriggerTime = remember { AtomicLong(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
                controller.cameraSelector = cameraViewModel.cameraSelector
            }
        },
        modifier = modifier.pointerInput(Unit) {
            detectVerticalDragGestures { change, dragAmount ->
                change.consume()

                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastTriggerTime.get()

                if (elapsedTime > 1000 && (dragAmount < -20 || dragAmount > 20)) {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    cameraViewModel.cameraSelector = controller.cameraSelector
                    lastTriggerTime.set(currentTime)
                }
            }
        }
    )
}