package com.example.androidobjectdetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//typealias LumaListener = (luma: Double) -> Unit


class MainActivity : AppCompatActivity() {
    private val TAG = "com.example.androidobjectdetector.MainActivity"

    //    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        start_tracking_button.setOnClickListener {
            startTracking()
            start_tracking_button.isEnabled = false
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private lateinit var overlay: Bitmap

    private fun convertX(srcWidth: Int, trgWidth: Int, x: Int): Float {
        return x.toFloat() / srcWidth.toFloat() * trgWidth.toFloat()
    }

    private fun convertY(srcHeight: Int, trgHeight: Int, y: Int): Float {
        return y.toFloat() / srcHeight.toFloat() * trgHeight.toFloat()
    }

    private fun convertBox(srcBox: Rect, srcSize: Size, trgSize: Size): Rect {
        val left = convertX(srcSize.width, trgSize.width, srcBox.left)
        val right = convertX(srcSize.width, trgSize.width, srcBox.right)
        val top = convertY(srcSize.height, trgSize.height, srcBox.top)
        val bottom = convertY(srcSize.height, trgSize.height, srcBox.bottom)
        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    private fun startTracking() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(viewFinder.width, viewFinder.height))
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ObjectAnalyzer { imgSize, detectedObjects ->
                            val bitmap = viewFinder.bitmap ?: return@ObjectAnalyzer
                            overlay = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                            val rectPaint = Paint()
                            rectPaint.color = Color.RED
                            rectPaint.style = Paint.Style.STROKE
                            rectPaint.strokeWidth = 5f

                            val textPaint = Paint()
                            textPaint.color = Color.BLACK
                            textPaint.textSize = 30f

                            val canvas = Canvas(overlay)

                            for (detectedObject in detectedObjects) {

                                val boundingBox = convertBox(detectedObject.boundingBox, imgSize, Size(bitmap.height, bitmap.width))
                                val id = detectedObject.trackingId ?: 0
                                canvas.drawRect(boundingBox, rectPaint)
                                for (label in detectedObject.labels) {
                                    val text = id.toString() + " " + label.text
                                    canvas.drawText(text, boundingBox.left.toFloat() + 7f,
                                            boundingBox.top.toFloat() + 42f, textPaint)
                                }
                            }

                            runOnUiThread {
                                imageView.setImageBitmap(overlay)
                            }
                        })
                    }


            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

//    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
//
//        private fun ByteBuffer.toByteArray(): ByteArray {
//            rewind()    // Rewind the buffer to zero
//            val data = ByteArray(remaining())
//            get(data)   // Copy the buffer into a byte array
//            return data // Return the byte array
//        }
//
//        override fun analyze(image: ImageProxy) {
//
//            val buffer = image.planes[0].buffer
//            val data = buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//            val luma = pixels.average()
//
//            listener(luma)
//
//            image.close()
//        }
//    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
