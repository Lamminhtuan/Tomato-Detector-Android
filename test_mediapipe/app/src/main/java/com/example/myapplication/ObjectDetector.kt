package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.proto.BaseOptionsProto
import com.google.mediapipe.tasks.core.proto.BaseOptionsProto.BaseOptionsOrBuilder
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectorHelper(
    val thresHold: Float = 0.5f,
    val numThread: Int = 3,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {

    private var objectDetector: ObjectDetector? = null
    private var imageRotation = 0
    private lateinit var imageProcessingOptions: ImageProcessingOptions
    init {
        setup()
    }
    private fun setup(){
        val baseOptionsBuilder = BaseOptions.builder()
        baseOptionsBuilder.setDelegate(Delegate.CPU)
        val modelName = "model.tflite"
        baseOptionsBuilder.setModelAssetPath(modelName)
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setScoreThreshold(thresHold).setRunningMode(RunningMode.LIVE_STREAM)
        imageProcessingOptions = ImageProcessingOptions.builder().setRotationDegrees(imageRotation).build()
        optionsBuilder.setRunningMode(RunningMode.LIVE_STREAM).setResultListener(this::returnLiveStreamResult)
        val options = optionsBuilder.build()
        objectDetector = ObjectDetector.createFromOptions(context, options)

    }
    fun clear(){
        objectDetector?.close()
        objectDetector = null
    }
    fun detectLiveStream(imageProxy: ImageProxy){
            val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)}
        imageProxy.close()

        val frameTime = SystemClock.uptimeMillis()
        if (imageRotation != imageProxy.imageInfo.rotationDegrees) {
            imageRotation = imageProxy.imageInfo.rotationDegrees
            clear()
            setup()
            return
        }
        val mpImage = BitmapImageBuilder(bitmapBuffer).build()
        detectAsync(mpImage, frameTime)
    }
    fun detectAsync(mpImage: MPImage, frameTime: Long){
        objectDetector?.detectAsync(mpImage, imageProcessingOptions, frameTime)
    }
    private fun returnLiveStreamResult(result: ObjectDetectorResult, input:MPImage){

        objectDetectorListener?.onResults(
            ResultBundle(listOf(result), input.height, input.width, imageRotation)
                )
    }
    data class ResultBundle(
        val results: List<ObjectDetectorResult>,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val inputImageRotation: Int,
    )
    interface DetectorListener {
        fun onResults(resultBundle: ResultBundle
        )
    }
}