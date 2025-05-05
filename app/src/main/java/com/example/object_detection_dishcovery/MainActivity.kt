package com.example.object_detection_dishcovery

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.object_detection_dishcovery.Constants.LABELS_PATH
import com.example.object_detection_dishcovery.Constants.MODEL_PATH
import com.example.object_detection_dishcovery.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.object_detection_dishcovery.RecipeManager

private val recipeManager = RecipeManager()

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    // Add detection storage
    private val detectionStorage = DetectionStorage()

    private lateinit var cameraExecutor: ExecutorService

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

        // Setup toggle scan button
        setupScanButton()

        // Setup data management buttons
        setupDataManagementButtons()
    }

    private fun setupScanButton() {
        binding.btnToggleScan.setOnClickListener {
            val isScanning = detector.toggleScanning()
        }

        // Auto-start scanning when app opens
        if (!detector.isScanning()) {
            detector.startScanning()
        }
    }

    private fun setupDataManagementButtons() {
        binding.btnClearData.setOnClickListener {
            detectionStorage.clearDetections()
            Toast.makeText(this, "Ingredient data cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnShowData.setOnClickListener {
            showIngredientsDialog()
        }

        binding.btnHome.setOnClickListener {
            detectionStorage.clearDetections()
            Toast.makeText(this, "Ingredient data cleared", Toast.LENGTH_SHORT).show()
        }

    }

    private fun showIngredientsDialog() {
        // Get all detections and filter by confidence level
        val allDetections = detectionStorage.getAllDetections()

        // First filter by confidence level
        val highConfidenceDetections = allDetections.filter { it.boundingBox.cnf >= 0.60f }

        if (highConfidenceDetections.isEmpty()) {
            Toast.makeText(this, "No ingredients detected yet", Toast.LENGTH_SHORT).show()
            return
        }

        // Then create a map to hold the highest confidence detection for each class name
        val uniqueDetectionsByClass = mutableMapOf<String, DetectionData>()

        // For each detection, only keep the one with highest confidence per class
        highConfidenceDetections.forEach { detection ->
            val className = detection.boundingBox.clsName
            val existingDetection = uniqueDetectionsByClass[className]

            // If this class hasn't been seen yet or this detection has higher confidence
            if (existingDetection == null || detection.boundingBox.cnf > existingDetection.boundingBox.cnf) {
                uniqueDetectionsByClass[className] = detection
            }
        }

        // Get the list of unique detections
        val uniqueDetections = uniqueDetectionsByClass.values.toList()

        if (uniqueDetections.isEmpty()) {
            Toast.makeText(this, "No ingredients detected yet", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert to ingredient data
        val ingredients = uniqueDetections.map { detection ->
            IngredientData(
                name = detection.boundingBox.clsName,
                detectionData = detection
            )
        }

        // Create and show the dialog
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ingredients)

        // Set up the close button
        val closeButton = dialog.findViewById<ImageButton>(R.id.btnCloseDialog)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set up ingredients list
        val container = dialog.findViewById<LinearLayout>(R.id.ingredientsContainer)
        container.removeAllViews()

        // Add ingredients to the container
        val inflater = LayoutInflater.from(this)
        for (ingredient in ingredients) {
            val itemView = inflater.inflate(R.layout.item_ingredient, container, false)
            val nameText = itemView.findViewById<TextView>(R.id.ingredientNameText)
            nameText.text = ingredient.name
            container.addView(itemView)
        }

        // Set up the action button
        val actionButton = dialog.findViewById<Button>(R.id.btnRecommendRecipe)
        actionButton.setOnClickListener {
            Toast.makeText(this, "Finding recipes for your ingredients...", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            // Add this - call the new method to show recipes
            showRecipesDialog(ingredients.map { it.name })
        }
        // Show the dialog
        dialog.show()

        // Also log the data for debugging
        Log.d(TAG, "Unique high confidence ingredients: ${ingredients.size}")
        ingredients.forEach { ingredient ->
            Log.d(TAG, "Ingredient: ${ingredient.name}, " +
                    "Confidence: ${ingredient.detectionData.boundingBox.cnf}")
        }
    }

    private fun showRecipesDialog(detectedIngredients: List<String>) {
        // Create and show the dialog
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_recipes)

        // Set up the close button
        val closeButton = dialog.findViewById<ImageButton>(R.id.btnCloseRecipesDialog)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show which ingredients we're using
        val ingredientsText = dialog.findViewById<TextView>(R.id.recipeIngredientsUsed)
        ingredientsText.text = "Ingredients used: ${detectedIngredients.joinToString(", ")}"

        // Find recipes that match our ingredients
        val recipeMatches = recipeManager.findRecipesWithIngredients(detectedIngredients)

        if (recipeMatches.isEmpty()) {
            Toast.makeText(this, "No recipes found with these ingredients", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        // Set up RecyclerView
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recipesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up adapter with click listener for recipes
        val adapter = RecipeAdapter(recipeMatches) { recipe ->
            showRecipeDetailDialog(recipe)
        }
        recyclerView.adapter = adapter

        // Show the dialog
        dialog.show()

        // Log for debugging
        Log.d(TAG, "Found ${recipeMatches.size} recipes matching ingredients: ${detectedIngredients.joinToString(", ")}")
    }

    // Add this method to show recipe details
    private fun showRecipeDetailDialog(recipe: RecipeData) {
        // Create and show the dialog
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_recipe_detail)

        // Set up the close button
        val closeButton = dialog.findViewById<ImageButton>(R.id.btnCloseRecipeDetailDialog)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set recipe title
        val titleText = dialog.findViewById<TextView>(R.id.recipeDetailTitle)
        titleText.text = recipe.name

        // Set ingredients
        val ingredientsText = dialog.findViewById<TextView>(R.id.recipeDetailIngredients)
        val formattedIngredients = recipe.ingredients.joinToString("\n") { "• $it" }
        ingredientsText.text = formattedIngredients

        // Set instructions
        val instructionsText = dialog.findViewById<TextView>(R.id.recipeDetailInstructions)
        instructionsText.text = recipe.instructions

        // Set prep time
        val prepTimeText = dialog.findViewById<TextView>(R.id.recipeDetailPrepTime)
        prepTimeText.text = "Preparation time: ${recipe.prepTime} minutes"

        // Show the dialog
        dialog.show()
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

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
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
        binding.overlay.invalidate()
    }


    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, frameWidth: Int, frameHeight: Int) {
        // Store detection data
        val currentTime = System.currentTimeMillis()
        boundingBoxes.forEach { box ->
            val detectionData = DetectionData(
                boundingBox = box,
                timestamp = currentTime,
                frameWidth = frameWidth,
                frameHeight = frameHeight
            )
            detectionStorage.addDetection(detectionData)
        }

        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    override fun onScanningStatusChanged(isScanning: Boolean) {
        runOnUiThread {
            if (isScanning) {
                binding.btnToggleScan.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                binding.btnToggleScan.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }
    }
}

