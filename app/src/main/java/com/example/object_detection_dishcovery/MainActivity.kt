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
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    // Database - now independent
    private lateinit var databaseHelper: DatabaseHelper

    // Session tracking
    private var currentSessionId: Long = -1

    private lateinit var cameraExecutor: ExecutorService

    // Simple data class for ingredients compatible with your UI
    data class IngredientData(
        var name: String,
        val confidence: Float,
        val timestamp: Long
    )

    // Add missing data classes for recipes
    data class SimpleRecipeMatch(
        val recipe: SimpleRecipeData,
        val matchPercentage: Float,
        val matchedIngredients: List<String>
    )

    data class SimpleRecipeData(
        val id: Long,
        val name: String,
        val ingredients: List<String>,
        val instructions: String,
        val prepTime: Int,
        val servings: Int = 2,
        val difficulty: String = "easy"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database
        databaseHelper = DatabaseHelper(this)

        // Initialize detector with improved setup
        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)

        // Set up detector with error handling
        try {
            detector.setup()
            if (!detector.isSetupComplete()) {
                Log.e(TAG, "Detector setup failed")
                Toast.makeText(this, "Failed to initialize ingredient detector", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "Detector setup successful")
                // Log loaded labels for debugging
                val labels = detector.getLabels()
                Log.d(TAG, "Loaded labels: ${labels.joinToString(", ")}")

                if (labels.isEmpty()) {
                    Toast.makeText(this, "Warning: No ingredient labels loaded", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up detector: ${e.message}", e)
            Toast.makeText(this, "Error initializing detector: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Initialize camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup UI components
        setupScanButton()
        setupDataManagementButtons()
        setupAdditionalIcons()

        // Start session tracking
        startNewSession()

        // Show detector status
        showDetectorStatus()
    }

    private fun showDetectorStatus() {
        runOnUiThread {
            val labels = detector.getLabels()
            val statusMessage = if (detector.isSetupComplete()) {
                "Detector ready! Loaded ${labels.size} ingredient types"
            } else {
                "Detector setup failed"
            }

            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Detector status: $statusMessage")

            // Log confidence threshold for debugging
            Log.d(TAG, "Confidence threshold: ${detector.getConfidenceThreshold()}")
        }
    }

    private fun setupScanButton() {
        binding.btnToggleScan.setOnClickListener {
            if (!detector.isSetupComplete()) {
                Toast.makeText(this, "Detector is not ready. Please restart the app.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isScanning = detector.toggleScanning()
            Log.d(TAG, "User toggled scanning: $isScanning")

            val message = if (isScanning) "Scanning started" else "Scanning stopped"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // AUTO-START scanning when app opens (only if detector is ready)
        if (detector.isSetupComplete() && !detector.isScanning()) {
            detector.startScanning()
            Log.d(TAG, "Auto-started scanning on app launch")
        }
    }

    private fun setupDataManagementButtons() {
        binding.btnClearData.setOnClickListener {
            showClearDataConfirmationDialog()
        }

        binding.btnShowData.setOnClickListener {
            showIngredientsDialog()
        }

        binding.btnHome.setOnClickListener {
            showClearDataConfirmationDialog()
        }
    }

    private fun setupAdditionalIcons() {
        // Close button functionality
        binding.iconClose.setOnClickListener {
            showExitConfirmationDialog()
        }

        // Help button functionality
        binding.helpButton.setOnClickListener {
            showHelpDialog()
        }
    }

    // Session management
    private fun startNewSession() {
        try {
            currentSessionId = databaseHelper.startScanSession()
            Log.d(TAG, "Started new scan session: $currentSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session: ${e.message}", e)
            currentSessionId = -1
        }
    }

    private fun endCurrentSession() {
        if (currentSessionId != -1L) {
            try {
                databaseHelper.endScanSession(currentSessionId)
                Log.d(TAG, "Ended scan session: $currentSessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Error ending session: ${e.message}", e)
            }
        }
    }

    // Dialog functions
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                endCurrentSession()
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showHelpDialog() {
        val labels = detector.getLabels()
        val detectedTypes = if (labels.isNotEmpty()) {
            labels.take(10).joinToString(", ") + if (labels.size > 10) "..." else ""
        } else {
            "No ingredient types loaded"
        }

        AlertDialog.Builder(this)
            .setTitle("How to Use Ingredient Scanner")
            .setMessage("""
                1. Point your camera at ingredients
                2. Tap the scan button to start/stop scanning
                3. Detected ingredients will appear with bounding boxes
                4. Tap the list icon to view detected ingredients
                5. Get recipe recommendations based on your ingredients
                
                Tips:
                • Keep ingredients well-lit and in focus
                • Scanner can detect: $detectedTypes
                • Higher confidence detections are more accurate
                • Current confidence threshold: ${String.format("%.1f%%", detector.getConfidenceThreshold() * 100)}
                
                Troubleshooting:
                • If nothing is detected, try better lighting
                • Move camera closer or farther from ingredients  
                • Ensure ingredients are clearly visible
            """.trimIndent())
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun showClearDataConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("This will clear all detected ingredients and detection history. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        try {
            // Clear database
            databaseHelper.clearAllDetections()
            Toast.makeText(this, "Ingredient data cleared", Toast.LENGTH_SHORT).show()

            // Session management
            endCurrentSession()
            startNewSession()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data: ${e.message}", e)
            Toast.makeText(this, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showIngredientsDialog() {
        try {
            // Get ingredients from database
            val dbIngredients = databaseHelper.getUniqueIngredientsWithHighConfidence(0.30f)

            if (dbIngredients.isEmpty()) {
                Toast.makeText(this, "No ingredients detected yet. Try scanning some ingredients!", Toast.LENGTH_LONG).show()
                return
            }

            // Convert to simple ingredient data for UI
            val ingredients = dbIngredients.map { dbIngredient ->
                IngredientData(
                    name = dbIngredient.name,
                    confidence = dbIngredient.detectionData.boundingBox.cnf,
                    timestamp = dbIngredient.detectionData.timestamp
                )
            }.toMutableList()

            // Create and show the dialog
            val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_ingredients)

            // Set up the close button
            val closeButton = dialog.findViewById<ImageButton>(R.id.btnCloseDialog)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Set up ingredients list with functional edit/delete icons
            val container = dialog.findViewById<LinearLayout>(R.id.ingredientsContainer)
            container.removeAllViews()

            // Add ingredients to the container
            val inflater = LayoutInflater.from(this)
            ingredients.forEachIndexed { index, ingredient ->
                val itemView = inflater.inflate(R.layout.item_ingredient, container, false)
                val nameText = itemView.findViewById<TextView>(R.id.ingredientNameText)
                val confidenceText = itemView.findViewById<TextView>(R.id.ingredientConfidence)
                val editIcon = itemView.findViewById<ImageView>(R.id.ingredientEdit)
                val deleteIcon = itemView.findViewById<ImageView>(R.id.deleteIcon1)

                nameText.text = ingredient.name
                confidenceText.text = "${String.format("%.1f", ingredient.confidence * 100)}%"

                // Set up edit functionality
                editIcon?.setOnClickListener {
                    showEditIngredientDialog(ingredient.name) { newName ->
                        if (newName.isNotEmpty() && newName != ingredient.name) {
                            // Update the ingredient name in database
                            val success = databaseHelper.updateIngredientName(ingredient.name, newName)
                            if (success) {
                                // Update the UI
                                nameText.text = newName
                                // Update the ingredient object
                                ingredient.name = newName
                                Toast.makeText(this, "Ingredient updated to: $newName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to update ingredient", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Set up delete functionality
                deleteIcon?.setOnClickListener {
                    showDeleteConfirmationDialog(ingredient.name) {
                        // Delete from database
                        val success = databaseHelper.deleteIngredient(ingredient.name)
                        if (success) {
                            // Remove from UI
                            container.removeView(itemView)
                            // Remove from list
                            ingredients.removeAt(index)
                            Toast.makeText(this, "Ingredient '${ingredient.name}' deleted", Toast.LENGTH_SHORT).show()

                            // If no ingredients left, close dialog
                            if (ingredients.isEmpty()) {
                                Toast.makeText(this, "No ingredients remaining", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this, "Failed to delete ingredient", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                container.addView(itemView)
            }

            // Set up the action button
            val actionButton = dialog.findViewById<Button>(R.id.btnRecommendRecipe)
            actionButton.setOnClickListener {
                Toast.makeText(this, "Finding recipes for your ingredients...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Show recipes dialog
                showRecipesDialog(ingredients.map { it.name })
            }

            // Show the dialog
            dialog.show()

            // Log for debugging
            Log.d(TAG, "Unique high confidence ingredients: ${ingredients.size}")
            ingredients.forEach { ingredient ->
                Log.d(TAG, "Ingredient: ${ingredient.name}, Confidence: ${String.format("%.3f", ingredient.confidence)}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing ingredients dialog: ${e.message}", e)
            Toast.makeText(this, "Error loading ingredients: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper methods for edit/delete functionality
    private fun showEditIngredientDialog(currentName: String, onUpdate: (String) -> Unit) {
        val editText = EditText(this).apply {
            setText(currentName)
            hint = "Enter new ingredient name"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Ingredient")
            .setMessage("Change '$currentName' to:")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    onUpdate(newName)
                } else {
                    Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(ingredientName: String, onDelete: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to delete '$ingredientName'?")
            .setPositiveButton("Delete") { _, _ ->
                onDelete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecipesDialog(detectedIngredients: List<String>) {
        try {
            // Get recipe matches from database - convert to SimpleRecipeMatch
            val enhancedRecipeMatches = databaseHelper.findRecipesWithIngredients(detectedIngredients)

            // Convert enhanced matches to simple matches for UI compatibility
            val recipeMatches = enhancedRecipeMatches.map { enhancedMatch ->
                SimpleRecipeMatch(
                    recipe = SimpleRecipeData(
                        id = enhancedMatch.recipe.id,
                        name = enhancedMatch.recipe.name,
                        ingredients = enhancedMatch.recipe.ingredients,
                        instructions = enhancedMatch.recipe.instructions,
                        prepTime = enhancedMatch.recipe.prepTime,
                        servings = enhancedMatch.recipe.servings,
                        difficulty = enhancedMatch.recipe.difficulty
                    ),
                    matchPercentage = enhancedMatch.matchPercentage,
                    matchedIngredients = enhancedMatch.matchedIngredients
                )
            }

            if (recipeMatches.isEmpty()) {
                Toast.makeText(this, "No recipes found with these ingredients", Toast.LENGTH_SHORT).show()
                return
            }

            // Save recipe matches to database - convert back to enhanced for saving
            databaseHelper.saveRecipeMatches(currentSessionId, enhancedRecipeMatches)

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
            ingredientsText?.text = "Ingredients used: ${detectedIngredients.joinToString(", ")}"

            // Set up RecyclerView
            val recyclerView = dialog.findViewById<RecyclerView>(R.id.recipesRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)

            // Create a simple adapter for recipes
            val adapter = SimpleRecipeAdapter(recipeMatches) { recipe ->
                showRecipeDetailDialog(recipe)
            }
            recyclerView.adapter = adapter

            // Show the dialog
            dialog.show()

            // Log for debugging
            Log.d(TAG, "Found ${recipeMatches.size} recipes matching ingredients: ${detectedIngredients.joinToString(", ")}")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing recipes dialog: ${e.message}", e)
            Toast.makeText(this, "Error loading recipes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Simple recipe adapter
    private inner class SimpleRecipeAdapter(
        private val recipes: List<SimpleRecipeMatch>,
        private val onRecipeClick: (SimpleRecipeData) -> Unit
    ) : RecyclerView.Adapter<SimpleRecipeAdapter.RecipeViewHolder>() {

        inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recipeName: TextView = itemView.findViewById(R.id.recipeName)
            val recipeMatch: TextView = itemView.findViewById(R.id.recipeMatch)
            val recipePrepTime: TextView = itemView.findViewById(R.id.recipePrepTime)

            init {
                itemView.setOnClickListener {
                    onRecipeClick(recipes[adapterPosition].recipe)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recipe, parent, false)
            return RecipeViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipeMatch = recipes[position]
            holder.recipeName.text = recipeMatch.recipe.name
            holder.recipeMatch.text = "Ingredients match: ${(recipeMatch.matchPercentage * 100).toInt()}%"
            holder.recipePrepTime.text = "Prep time: ${recipeMatch.recipe.prepTime} minutes"
        }

        override fun getItemCount(): Int = recipes.size
    }

    private fun showRecipeDetailDialog(recipe: SimpleRecipeData) {
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

    // Camera methods - improved with better error handling
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed: ${e.message}", e)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
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
            try {
                val bitmapBuffer =
                    Bitmap.createBitmap(
                        imageProxy.width,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )
                imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

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

                // Only detect if detector is properly set up
                if (detector.isSetupComplete()) {
                    detector.detect(rotatedBitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing camera frame: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
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
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for ingredient detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        endCurrentSession()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    // Detector listener methods - updated to work directly with DatabaseHelper
    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.invalidate()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, frameWidth: Int, frameHeight: Int) {
        try {
            // Store detection data directly in database
            val currentTime = System.currentTimeMillis()
            boundingBoxes.forEach { box ->
                // Create DetectionData and save to database
                val detectionData = DetectionData(
                    boundingBox = box,
                    timestamp = currentTime,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight
                )
                databaseHelper.saveDetectionData(detectionData)
            }

            runOnUiThread {
                binding.inferenceTime.text = "${inferenceTime}ms"
                binding.overlay.apply {
                    setResults(boundingBoxes)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving detection data: ${e.message}", e)
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