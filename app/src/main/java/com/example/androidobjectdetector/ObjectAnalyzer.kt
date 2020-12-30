package com.example.androidobjectdetector

import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

//typealias DetectListener = (detectedObjects: List<DetectedObject>) -> Unit

class ObjectAnalyzer(private val listener: (imgSize: Size, detectedObjects: List<DetectedObject>) -> Unit) : ImageAnalysis.Analyzer {
    private val objectDetector = createMlObjectDetector()

    private fun createMlObjectDetector(): ObjectDetector {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        return ObjectDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            objectDetector.process(image)
                    .addOnSuccessListener{ detectedObjects ->
                        listener(Size(image.width, image.height), detectedObjects)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ObjectAnalyzer", e.toString())
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
        }
    }
}
