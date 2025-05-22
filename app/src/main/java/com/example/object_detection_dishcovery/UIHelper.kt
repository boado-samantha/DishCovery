package com.example.object_detection_dishcovery

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Simplified UIHelper class that works with the comprehensive DatabaseHelper
 * Focuses on the most commonly used UI configuration methods
 */
class UIHelper(private val context: Context) {

    private val databaseHelper = DatabaseHelper(context)

    /**
     * Configure a TextView with database settings
     */
    fun configureTextView(textView: TextView, textKey: String, screen: String, colorKey: String? = null) {
        // Set text content
        val text = databaseHelper.getAppText(textKey, screen)
        text?.let { textView.text = it }

        // Set text color if specified
        colorKey?.let { key ->
            val colorData = databaseHelper.getColor(key)
            colorData?.let {
                try {
                    textView.setTextColor(Color.parseColor(it.value))
                } catch (e: IllegalArgumentException) {
                    // Fallback to resource color if hex parsing fails
                    val colorRes = context.resources.getIdentifier(it.value, "color", context.packageName)
                    if (colorRes != 0) {
                        textView.setTextColor(ContextCompat.getColor(context, colorRes))
                    }
                }
            }
        }
    }

    /**
     * Configure a Button with database settings
     */
    fun configureButton(button: Button, buttonIdentifier: String) {
        val config = databaseHelper.getButtonConfig(buttonIdentifier)
        config?.let {
            // Set button text
            if (it.title.isNotEmpty()) {
                button.text = it.title
            }

            // Set background if available
            if (it.background.isNotEmpty()) {
                val drawableRes = context.resources.getIdentifier(it.background, "drawable", context.packageName)
                if (drawableRes != 0) {
                    button.setBackgroundResource(drawableRes)
                }
            }

            // Set enabled state
            button.isEnabled = it.isEnabled

            // Set visibility
            button.visibility = it.visibility
        }
    }

    /**
     * Configure an ImageButton with database settings
     */
    fun configureImageButton(imageButton: ImageButton, buttonIdentifier: String) {
        val config = databaseHelper.getButtonConfig(buttonIdentifier)
        config?.let {
            // Set icon if available
            if (it.icon.isNotEmpty()) {
                val iconRes = context.resources.getIdentifier(it.icon, "drawable", context.packageName)
                if (iconRes != 0) {
                    imageButton.setImageResource(iconRes)
                }
            }

            // Set background if available
            if (it.background.isNotEmpty()) {
                val drawableRes = context.resources.getIdentifier(it.background, "drawable", context.packageName)
                if (drawableRes != 0) {
                    imageButton.setBackgroundResource(drawableRes)
                }
            }

            // Set enabled state
            imageButton.isEnabled = it.isEnabled

            // Set visibility
            imageButton.visibility = it.visibility
        }
    }

    /**
     * Configure an ImageView with icon and color
     */
    fun configureImageView(imageView: ImageView, iconKey: String, colorKey: String? = null) {
        // Set icon
        val iconData = databaseHelper.getIcon(iconKey)
        iconData?.let {
            val iconRes = context.resources.getIdentifier(it.resourceName, "drawable", context.packageName)
            if (iconRes != 0) {
                imageView.setImageResource(iconRes)
            }
        }

        // Set color tint if specified
        colorKey?.let { key ->
            val colorData = databaseHelper.getColor(key)
            colorData?.let {
                try {
                    imageView.setColorFilter(Color.parseColor(it.value))
                } catch (e: IllegalArgumentException) {
                    val colorRes = context.resources.getIdentifier(it.value, "color", context.packageName)
                    if (colorRes != 0) {
                        imageView.setColorFilter(ContextCompat.getColor(context, colorRes))
                    }
                }
            }
        }
    }

    /**
     * Get formatted text with parameters
     */
    fun getFormattedText(textKey: String, screen: String, vararg params: Any): String {
        return databaseHelper.getFormattedAppText(textKey, screen, *params)
    }

    /**
     * Get navigation items for bottom navigation
     */
    fun getNavigationItems(): List<DatabaseHelper.NavigationItemData> {
        return databaseHelper.getNavigationItems()
    }

    /**
     * Get UI color value by key
     */
    fun getColorValue(colorKey: String): String? {
        return databaseHelper.getColor(colorKey)?.value
    }

    /**
     * Get icon resource name by key
     */
    fun getIconResource(iconKey: String): String? {
        return databaseHelper.getIcon(iconKey)?.resourceName
    }

    /**
     * Get drawable resource name by key
     */
    fun getDrawableResource(drawableKey: String): String? {
        return databaseHelper.getDrawable(drawableKey)?.resourceName
    }

    /**
     * Update button state dynamically
     */
    fun updateButtonState(buttonIdentifier: String, isEnabled: Boolean? = null, visibility: Int? = null): Boolean {
        return databaseHelper.updateButtonConfig(buttonIdentifier, isEnabled, visibility)
    }

    /**
     * Get UI setting values for common configurations
     */
    fun getUISettings(): UISettings {
        return UISettings(
            scanButtonSize = databaseHelper.getUIIntegerSetting("scan_button_size", 86),
            cornerButtonSize = databaseHelper.getUIIntegerSetting("corner_button_size", 55),
            helpButtonSize = databaseHelper.getUIIntegerSetting("help_button_size", 45),
            showInferenceTime = databaseHelper.getUIBooleanSetting("inference_time_visible", false),
            showDetectionCount = databaseHelper.getUIBooleanSetting("detection_count_visible", false),
            cameraPreviewEnabled = databaseHelper.getUIBooleanSetting("camera_preview_enabled", true),
            detectionOverlayEnabled = databaseHelper.getUIBooleanSetting("detection_overlay_enabled", true)
        )
    }

    /**
     * Data class for common UI settings
     */
    data class UISettings(
        val scanButtonSize: Int,
        val cornerButtonSize: Int,
        val helpButtonSize: Int,
        val showInferenceTime: Boolean,
        val showDetectionCount: Boolean,
        val cameraPreviewEnabled: Boolean,
        val detectionOverlayEnabled: Boolean
    )

    companion object {
        // Screen names for different layouts
        const val SCREEN_DIALOG_INGREDIENTS = "dialog_ingredients"
        const val SCREEN_DIALOG_RECIPE_DETAIL = "dialog_recipe_detail"
        const val SCREEN_DIALOG_RECIPES = "dialog_recipes"
        const val SCREEN_MAIN_ACTIVITY = "main_activity"
        const val SCREEN_ITEM_DETECTION = "item_detection"
        const val SCREEN_ITEM_RECIPE = "item_recipe"
        const val SCREEN_NAVIGATION = "navigation"

        // Common button identifiers
        const val BTN_CLOSE_INGREDIENTS = "btn_close_ingredients"
        const val BTN_CLOSE_RECIPE_DETAIL = "btn_close_recipe_detail"
        const val BTN_CLOSE_RECIPES = "btn_close_recipes"
        const val BTN_RECOMMEND_RECIPE = "btn_recommend_recipe"
        const val BTN_HELP = "btn_help"
        const val BTN_HOME = "btn_home"
        const val BTN_SCAN = "btn_scan"
        const val BTN_SHOW_DATA = "btn_show_data"
        const val BTN_CLEAR_DATA = "btn_clear_data"
    }
}