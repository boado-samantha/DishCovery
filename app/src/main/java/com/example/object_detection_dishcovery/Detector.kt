package com.example.object_detection_dishcovery

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener
) {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    // Controls whether scanning is active
    private val isScanning = AtomicBoolean(false)

    // Track setup status
    private var isSetupComplete = false

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun setup() {
        try {
            Log.d(TAG, "Starting detector setup...")

            val model = FileUtil.loadMappedFile(context, modelPath)
            val options = Interpreter.Options()
            options.numThreads = 4
            interpreter = Interpreter(model, options)

            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()

            if (inputShape == null || outputShape == null) {
                Log.e(TAG, "Failed to get tensor shapes")
                return
            }

            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]
            numChannel = outputShape[1]
            numElements = outputShape[2]

            Log.d(TAG, "Model input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Model output shape: ${outputShape.contentToString()}")
            Log.d(TAG, "Tensor dimensions - Width: $tensorWidth, Height: $tensorHeight")
            Log.d(TAG, "Output dimensions - Channels: $numChannel, Elements: $numElements")

            loadLabels()

            isSetupComplete = true
            Log.d(TAG, "Detector setup completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during setup: ${e.message}", e)
            isSetupComplete = false
        }
    }

    private fun loadLabels() {
        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null) {
                if (line.trim().isNotEmpty()) {
                    labels.add(line.trim())
                }
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()

            Log.d(TAG, "Loaded ${labels.size} labels:")
            labels.forEachIndexed { index, label ->
                Log.d(TAG, "Label $index: $label")
            }

            if (labels.isEmpty()) {
                Log.w(TAG, "Warning: No labels loaded from $labelPath")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Error loading labels: ${e.message}", e)
        }
    }

    fun clear() {
        interpreter?.close()
        interpreter = null
        isSetupComplete = false
        Log.d(TAG, "Detector cleared")
    }

    /**
     * Start scanning for objects
     */
    fun startScanning() {
        if (!isSetupComplete) {
            Log.w(TAG, "Cannot start scanning - setup not complete")
            return
        }

        isScanning.set(true)
        detectorListener.onScanningStatusChanged(true)
        Log.d(TAG, "Scanning started")
    }

    /**
     * Stop scanning for objects
     */
    fun stopScanning() {
        isScanning.set(false)
        detectorListener.onScanningStatusChanged(false)
        Log.d(TAG, "Scanning stopped")
    }

    /**
     * Check if scanning is currently active
     */
    fun isScanning(): Boolean {
        return isScanning.get()
    }

    /**
     * Toggle scanning status
     */
    fun toggleScanning(): Boolean {
        val newStatus = !isScanning.get()
        if (newStatus) {
            startScanning()
        } else {
            stopScanning()
        }
        return newStatus
    }

    fun detect(frame: Bitmap) {
        // Check if setup is complete
        if (!isSetupComplete) {
            Log.w(TAG, "Detection skipped - setup not complete")
            return
        }

        // Skip detection if scanning is disabled
        if (!isScanning.get()) {
            Log.v(TAG, "Detection skipped - scanning disabled")
            return
        }

        val interpreter = this.interpreter
        if (interpreter == null) {
            Log.w(TAG, "Detection skipped - interpreter is null")
            return
        }

        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            Log.w(TAG, "Detection skipped - invalid tensor dimensions")
            return
        }

        try {
            var inferenceTime = SystemClock.uptimeMillis()

            val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resizedBitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val imageBuffer = processedImage.buffer

            val output = TensorBuffer.createFixedSize(
                intArrayOf(1, numChannel, numElements),
                OUTPUT_IMAGE_TYPE
            )
            interpreter.run(imageBuffer, output.buffer)

            val bestBoxes = bestBox(output.floatArray)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            if (bestBoxes == null || bestBoxes.isEmpty()) {
                Log.v(TAG, "No objects detected")
                detectorListener.onEmptyDetect()
                return
            }

            Log.d(TAG, "Detected ${bestBoxes.size} objects with inference time: ${inferenceTime}ms")
            bestBoxes.forEach { box ->
                Log.d(TAG, "Detection: ${box.clsName} (${String.format("%.2f", box.cnf)})")
            }

            detectorListener.onDetect(bestBoxes, inferenceTime, frame.width, frame.height)

        } catch (e: Exception) {
            Log.e(TAG, "Error during detection: ${e.message}", e)
            detectorListener.onEmptyDetect()
        }
    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        // Add safety checks
        if (array.isEmpty()) {
            Log.w(TAG, "Model output array is empty")
            return null
        }

        if (numElements <= 0 || numChannel <= 4) {
            Log.w(TAG, "Invalid model dimensions - numElements: $numElements, numChannel: $numChannel")
            return null
        }

        // Check if array size matches expected dimensions
        val expectedSize = numElements * numChannel
        if (array.size != expectedSize) {
            Log.w(TAG, "Array size mismatch. Expected: $expectedSize, Actual: ${array.size}")
            return null
        }

        val boundingBoxes = mutableListOf<BoundingBox>()
        var detectionCount = 0

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j

            while (j < numChannel) {
                // Add bounds check for array access
                if (arrayIdx >= array.size) {
                    Log.w(TAG, "Array index out of bounds. Index: $arrayIdx, Array size: ${array.size}")
                    break
                }

                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            // Log detection attempts for debugging
            if (maxConf > 0.05f) { // Log low confidence detections for debugging
                val className = if (maxIdx >= 0 && maxIdx < labels.size) labels[maxIdx] else "unknown"
                Log.v(TAG, "Detection candidate $c: $className (confidence: ${String.format("%.3f", maxConf)})")
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                // Validate maxIdx before using it
                if (maxIdx < 0 || maxIdx >= labels.size) {
                    Log.w(TAG, "Invalid class index: $maxIdx, Labels size: ${labels.size}")
                    continue
                }

                val clsName = labels[maxIdx]

                // Add bounds checking for coordinate access
                val cxIdx = c
                val cyIdx = c + numElements
                val wIdx = c + numElements * 2
                val hIdx = c + numElements * 3

                if (cxIdx >= array.size || cyIdx >= array.size || wIdx >= array.size || hIdx >= array.size) {
                    Log.w(TAG, "Coordinate index out of bounds. Indices: [$cxIdx, $cyIdx, $wIdx, $hIdx], Array size: ${array.size}")
                    continue
                }

                val cx = array[cxIdx]
                val cy = array[cyIdx]
                val w = array[wIdx]
                val h = array[hIdx]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)

                // More lenient bounds checking for coordinates
                if (x1 < -0.1F || x1 > 1.1F) continue
                if (y1 < -0.1F || y1 > 1.1F) continue
                if (x2 < -0.1F || x2 > 1.1F) continue
                if (y2 < -0.1F || y2 > 1.1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )

                detectionCount++
                Log.d(TAG, "Valid detection: $clsName (${String.format("%.3f", maxConf)}) at [$x1, $y1, $x2, $y2]")
            }
        }

        Log.d(TAG, "Found $detectionCount valid detections before NMS")

        if (boundingBoxes.isEmpty()) {
            Log.d(TAG, "No detections above confidence threshold (${CONFIDENCE_THRESHOLD})")
            return null
        }

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        Log.d(TAG, "Applying NMS to ${sortedBoxes.size} boxes")

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    Log.v(TAG, "Removing overlapping box: ${nextBox.clsName} (IoU: ${String.format("%.3f", iou)})")
                    iterator.remove()
                }
            }
        }

        Log.d(TAG, "NMS result: ${selectedBoxes.size} boxes selected")
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    /**
     * Get current confidence threshold
     */
    fun getConfidenceThreshold(): Float = CONFIDENCE_THRESHOLD

    /**
     * Get loaded labels
     */
    fun getLabels(): List<String> = labels.toList()

    /**
     * Check if detector is properly set up
     */
    fun isSetupComplete(): Boolean = isSetupComplete

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, frameWidth: Int, frameHeight: Int)
        fun onScanningStatusChanged(isScanning: Boolean)
    }

    companion object {
        private const val TAG = "Detector"
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32

        // Reduced confidence threshold for better ingredient detection
        private const val CONFIDENCE_THRESHOLD = 0.15F // Lowered from 0.3F
        private const val IOU_THRESHOLD = 0.5F
    }
}