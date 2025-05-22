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

    // Database and storage (ADDED - not changed)
    private lateinit var databaseHelper: DatabaseHelper
    private val detectionStorage = DetectionStorage()

    // Session tracking (ADDED - not changed)
    private var currentSessionId: Long = -1

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database (ADDED)
        databaseHelper = DatabaseHelper(this)

        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup toggle scan button (keeping your original logic)
        setupScanButton()

        // Setup data management buttons (keeping your original logic)
        setupDataManagementButtons()

        // ADDED: Make other icons functional
        setupAdditionalIcons()

        // ADDED: Start session tracking
        startNewSession()
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
            // ENHANCED: Show confirmation dialog instead of direct clear
            showClearDataConfirmationDialog()
        }

        binding.btnShowData.setOnClickListener {
            showIngredientsDialog()
        }

        binding.btnHome.setOnClickListener {
            // ENHANCED: Show confirmation dialog instead of direct clear
            showClearDataConfirmationDialog()
        }
    }

    // ADDED: Make previously non-functional icons work
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

    // ADDED: Session management
    private fun startNewSession() {
        currentSessionId = databaseHelper.startScanSession()
        Log.d(TAG, "Started new scan session: $currentSessionId")
    }

    private fun endCurrentSession() {
        if (currentSessionId != -1L) {
            databaseHelper.endScanSession(currentSessionId)
            Log.d(TAG, "Ended scan session: $currentSessionId")
        }
    }

    // ADDED: Dialog functions
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
                • Scanner works best with: Apple, Banana, Carrot, Egg, Tomato
                • Higher confidence detections are more accurate
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
        // Clear database (ENHANCED)
        databaseHelper.clearAllDetections()

        // Keep your original memory clearing
        detectionStorage.clearDetections()
        Toast.makeText(this, "Ingredient data cleared", Toast.LENGTH_SHORT).show()

        // ADDED: Session management
        endCurrentSession()
        startNewSession()
    }

    private fun showIngredientsDialog() {
        // ENHANCED: Get from database instead of just memory
        val allDetections = detectionStorage.getAllDetections()
        val dbIngredients = databaseHelper.getUniqueIngredientsWithHighConfidence(0.60f)

        // Use database data if available, fallback to memory storage
        val detections = if (dbIngredients.isNotEmpty()) {
            dbIngredients.map { it.detectionData }
        } else {
            allDetections
        }

        // First filter by confidence level
        val highConfidenceDetections = detections.filter { it.boundingBox.cnf >= 0.60f }

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
        }.toMutableList()

        // Create and show the dialog (keeping your original dialog)
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
            val editIcon = itemView.findViewById<ImageView>(R.id.ingredientEdit)
            val deleteIcon = itemView.findViewById<ImageView>(R.id.deleteIcon1)

            nameText.text = ingredient.name

            // Set up edit functionality with null check
            editIcon?.setOnClickListener {
                showEditIngredientDialog(ingredient.name) { newName ->
                    if (newName.isNotEmpty() && newName != ingredient.name) {
                        // Update the ingredient name in database
                        val success = updateIngredientName(ingredient.name, newName)
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

            // Set up delete functionality with null check
            deleteIcon?.setOnClickListener {
                showDeleteConfirmationDialog(ingredient.name) {
                    // Delete from database
                    val success = deleteIngredient(ingredient.name)
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

            // ENHANCED: Use database for recipe matching
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

    // ADDED: Helper methods for edit/delete functionality
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

    private fun updateIngredientName(oldName: String, newName: String): Boolean {
        return try {
            // Get the ingredient data
            val ingredients = databaseHelper.getUniqueIngredientsWithHighConfidence(0.0f)
            val targetIngredient = ingredients.find { it.name == oldName }

            if (targetIngredient != null) {
                // Create a new detection with the new name
                val updatedBoundingBox = targetIngredient.detectionData.boundingBox.copy(clsName = newName)
                val updatedDetectionData = targetIngredient.detectionData.copy(
                    boundingBox = updatedBoundingBox,
                    timestamp = System.currentTimeMillis()
                )

                // Save the new detection data
                databaseHelper.saveDetectionData(updatedDetectionData)

                // Delete the old ingredient
                deleteIngredient(oldName)

                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ingredient name: ${e.message}")
            false
        }
    }

    private fun deleteIngredient(ingredientName: String): Boolean {
        return try {
            // Get all current ingredients except the one we want to delete
            val allIngredients = databaseHelper.getUniqueIngredientsWithHighConfidence(0.0f)
            val ingredientsToKeep = allIngredients.filter { it.name != ingredientName }

            if (allIngredients.size == ingredientsToKeep.size) {
                // Ingredient not found
                return false
            }

            // Clear all data
            databaseHelper.clearAllDetections()

            // Re-add the ingredients we want to keep
            ingredientsToKeep.forEach { ingredient ->
                databaseHelper.saveDetectionData(ingredient.detectionData)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ingredient: ${e.message}")
            false
        }
    }

    private fun showRecipesDialog(detectedIngredients: List<String>) {
        // ENHANCED: Use database for recipe matching
        val recipeMatches = databaseHelper.findRecipesWithIngredients(detectedIngredients)

        if (recipeMatches.isEmpty()) {
            Toast.makeText(this, "No recipes found with these ingredients", Toast.LENGTH_SHORT).show()
            return
        }

        // ADDED: Save recipe matches to database
        databaseHelper.saveRecipeMatches(currentSessionId, recipeMatches)

        // Create and show the dialog (keeping your original dialog)
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

        // Set up RecyclerView (keeping your original setup)
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

    // Add this method to show recipe details (keeping your original approach)
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

    // Keep all your original camera methods exactly the same
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
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
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        endCurrentSession() // ADDED
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
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        binding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, frameWidth: Int, frameHeight: Int) {
        // Store detection data (ENHANCED: now saves to database too)
        val currentTime = System.currentTimeMillis()
        boundingBoxes.forEach { box ->
            val detectionData = DetectionData(
                boundingBox = box,
                timestamp = currentTime,
                frameWidth = frameWidth,
                frameHeight = frameHeight
            )

            // Keep your original memory storage
            detectionStorage.addDetection(detectionData)

            // ADDED: Also save to database
            databaseHelper.saveDetectionData(detectionData)
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

    // ADDED: Data class for ingredient data
    data class IngredientData(
        var name: String,
        val detectionData: DetectionData
    )
}