package com.example.myapplication

import android.os.Bundle
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas

import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import androidx.compose.ui.tooling.preview.Preview
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        viewBinding.viewFinder.scaleType = PreviewView.ScaleType.FILL_START
        setContentView(viewBinding.root)
        viewBinding.overlay.setBackgroundColor(resources.getColor(android.R.color.transparent))
        objectDetectorHelper = ObjectDetectorHelper(context = this, objectDetectorListener = this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (PermissionGranted()) {
            StartCamera()

            Toast.makeText(this, "Thank you! Permission granted!", Toast.LENGTH_SHORT).show()
        }
        else{
            RequestPermission()
        }
    }
    private fun StartCamera(){

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(viewBinding.viewFinder.display.rotation).build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(viewBinding.viewFinder.display.rotation).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat((OUTPUT_IMAGE_FORMAT_RGBA_8888)).build()
                .also{
                it.setAnalyzer(cameraExecutor, objectDetectorHelper::detectLiveStream)

            }
            val camaraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            try{
                cameraProvider.bindToLifecycle(this, camaraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {

                Log.d("Object", "Failed", exc)
            }
        }, ContextCompat.getMainExecutor((this)))
    }
    private fun RequestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100 )

    }
    private fun PermissionGranted() =
        (ContextCompat.checkSelfPermission(baseContext, REQUIRED_PERMISSIONS) == PackageManager.PERMISSION_GRANTED)

    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {

        val detectionResult = resultBundle.results[0]
        viewBinding.overlay.setResults(detectionResult, resultBundle.inputImageHeight, resultBundle.inputImageWidth, resultBundle.inputImageRotation)


        viewBinding.overlay.invalidate()


    }
    companion object {
        private val REQUIRED_PERMISSIONS =
            android.Manifest.permission.CAMERA
    }

}


