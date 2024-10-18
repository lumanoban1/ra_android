package com.surendramaran.yolov8tflite

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.isVisible
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraControl = camera?.cameraControl
    private var isZoomingIn = false
    private var currentZoomRatio = 1f
    private val zoomStep = 0.1f
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    open var buttons: btn1,

    private lateinit var cameraExecutor: ExecutorService


    /*val buttonMapping = mapOf(
        "T4-0330-WR-00708-C1E2A"  to binding.btn1,
        "T200-RH-003" to binding.btn2,
        "T210-RH-002" to binding.btn3,
        "V2010-V-101" to binding.btn4,
        "V2010-V-102" to binding.btn5,
        "V2010-V-110" to binding.btn6,
        "V2010-V-111" to binding.btn7,
        "V2010-V-120" to binding.btn8,
        "V2011-V-251" to binding.btn9,
        "V2011-V-255" to binding.btn10
    )*/

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()



    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            cameraControl = camera?.cameraControl
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            initZoom()
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initZoom() {
        binding.zoomButton.setOnTouchListener { _, event ->
            if (cameraControl != null) {
                val action = MotionEventCompat.getActionMasked(event)
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        isZoomingIn = true
                        zoom(currentZoomRatio + zoomStep)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isZoomingIn = false
                        true
                    }
                    else -> false
                }
            } else {
                false  // Return false if cameraControl is null
            }
        }
        binding.resetZoomButton.setOnClickListener {
            resetZoom()
        }
    }

    private fun resetZoom() {
        // Restablece el zoom al valor predeterminado (1.0x)
        zoom(1.0f)
    }

    private fun zoom(newZoomRatio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val cameraInfo = camera?.cameraInfo ?: return

        // Obtiene los límites de zoom actuales
        val minZoomRatio = cameraInfo.zoomState.value?.minZoomRatio ?: 1f
        val maxZoomRatio = cameraInfo.zoomState.value?.maxZoomRatio ?: 1f

        val clampedZoomRatio = newZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)

        // Establece el nuevo ratio de zoom
        cameraControl.setZoomRatio(clampedZoomRatio)

        // Actualiza el ratio de zoom actual
        currentZoomRatio = clampedZoomRatio
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        binding.overlay.clearResults()
        binding.overlay.invalidate()
    }


    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"

            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()

                for (box in boundingBoxes) {


                    val className = box.clsName

                    val buttons = listOf(
                        binding.btn1,
                        binding.btn2,
                        binding.btn3,
                        binding.btn4,
                        binding.btn5,
                        binding.btn6,
                        binding.btn7,
                        binding.btn8,
                        binding.btn9,
                        binding.btn10
                    )
                    val labels = listOf(
                        "T4-0330-WR-00708-C1E2A",
                        "T200-RH-003",
                        "T210-RH-002",
                        "V2010-V-101",
                        "V2010-V-102",
                        "V2010-V-110",
                        "V2010-V-111",
                        "V2010-V-120",
                        "V2011-V-251",
                        "V2011-V-255"
                    )


                    if (className in labels) {
                        for (button in buttons) {
                            button.isVisible = button.text == className
                        }
                    } else {
                        for (button in buttons) {
                            button.isVisible = false


                        }
                    }




                }

            }

        }

    }




    override fun onClearDetection(){
        binding.overlay.clearResults()

        binding.overlay.invalidate()
    }
}

