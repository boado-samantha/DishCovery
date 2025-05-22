package com.example.object_detection_dishcovery

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DishcoveryDatabase.db"
        private const val DATABASE_VERSION = 4 // Increased version for enhanced UI tables
        private const val TAG = "DatabaseHelper"

        // Detection Data table (stores BoundingBox data from object detection)
        private const val DETECTIONS_TABLE = "detection_data"
        private const val COLUMN_DETECTION_ID = "id"
        private const val COLUMN_DETECTION_CLASS_NAME = "class_name"
        private const val COLUMN_DETECTION_CONFIDENCE = "confidence"
        private const val COLUMN_DETECTION_X1 = "x1"
        private const val COLUMN_DETECTION_Y1 = "y1"
        private const val COLUMN_DETECTION_X2 = "x2"
        private const val COLUMN_DETECTION_Y2 = "y2"
        private const val COLUMN_DETECTION_CX = "cx"
        private const val COLUMN_DETECTION_CY = "cy"
        private const val COLUMN_DETECTION_WIDTH = "width"
        private const val COLUMN_DETECTION_HEIGHT = "height"
        private const val COLUMN_DETECTION_CLS = "cls"
        private const val COLUMN_DETECTION_TIMESTAMP = "timestamp"
        private const val COLUMN_DETECTION_FRAME_WIDTH = "frame_width"
        private const val COLUMN_DETECTION_FRAME_HEIGHT = "frame_height"

        // Ingredients table (processed unique ingredients)
        private const val INGREDIENTS_TABLE = "ingredients"
        private const val COLUMN_INGREDIENT_ID = "id"
        private const val COLUMN_INGREDIENT_NAME = "name"
        private const val COLUMN_INGREDIENT_CATEGORY = "category"
        private const val COLUMN_INGREDIENT_FIRST_DETECTED = "first_detected"
        private const val COLUMN_INGREDIENT_LAST_DETECTED = "last_detected"
        private const val COLUMN_INGREDIENT_DETECTION_COUNT = "detection_count"
        private const val COLUMN_INGREDIENT_HIGHEST_CONFIDENCE = "highest_confidence"

        // Recipes table (stores recipe data)
        private const val RECIPES_TABLE = "recipes"
        private const val COLUMN_RECIPE_ID = "id"
        private const val COLUMN_RECIPE_NAME = "name"
        private const val COLUMN_RECIPE_INSTRUCTIONS = "instructions"
        private const val COLUMN_RECIPE_PREP_TIME = "prep_time"
        private const val COLUMN_RECIPE_SERVINGS = "servings"
        private const val COLUMN_RECIPE_DIFFICULTY = "difficulty"
        private const val COLUMN_RECIPE_CREATED_AT = "created_at"

        // Recipe Ingredients junction table
        private const val RECIPE_INGREDIENTS_TABLE = "recipe_ingredients"
        private const val COLUMN_RI_ID = "id"
        private const val COLUMN_RI_RECIPE_ID = "recipe_id"
        private const val COLUMN_RI_INGREDIENT_NAME = "ingredient_name"
        private const val COLUMN_RI_QUANTITY = "quantity"
        private const val COLUMN_RI_UNIT = "unit"

        // Scan Sessions table (groups detections by scanning session)
        private const val SCAN_SESSIONS_TABLE = "scan_sessions"
        private const val COLUMN_SESSION_ID = "id"
        private const val COLUMN_SESSION_START_TIME = "start_time"
        private const val COLUMN_SESSION_END_TIME = "end_time"
        private const val COLUMN_SESSION_TOTAL_DETECTIONS = "total_detections"
        private const val COLUMN_SESSION_UNIQUE_INGREDIENTS = "unique_ingredients"

        // Recipe Matches table (stores recipe recommendation results)
        private const val RECIPE_MATCHES_TABLE = "recipe_matches"
        private const val COLUMN_MATCH_ID = "id"
        private const val COLUMN_MATCH_SESSION_ID = "session_id"
        private const val COLUMN_MATCH_RECIPE_ID = "recipe_id"
        private const val COLUMN_MATCH_PERCENTAGE = "match_percentage"
        private const val COLUMN_MATCH_MATCHED_INGREDIENTS = "matched_ingredients"
        private const val COLUMN_MATCH_CREATED_AT = "created_at"

        // UI Settings table (stores dynamic UI configurations)
        private const val UI_SETTINGS_TABLE = "ui_settings"
        private const val COLUMN_UI_ID = "id"
        private const val COLUMN_UI_KEY = "setting_key"
        private const val COLUMN_UI_VALUE = "setting_value"
        private const val COLUMN_UI_TYPE = "value_type"
        private const val COLUMN_UI_CATEGORY = "category"
        private const val COLUMN_UI_UPDATED_AT = "updated_at"

        // Navigation Items table (stores bottom navigation configuration)
        private const val NAV_ITEMS_TABLE = "navigation_items"
        private const val COLUMN_NAV_ID = "id"
        private const val COLUMN_NAV_TITLE = "title"
        private const val COLUMN_NAV_ICON = "icon_name"
        private const val COLUMN_NAV_POSITION = "position"
        private const val COLUMN_NAV_IS_ACTIVE = "is_active"
        private const val COLUMN_NAV_ACTION_TYPE = "action_type"
        private const val COLUMN_NAV_TARGET = "target"

        // Button Configurations table (stores button settings and states)
        private const val BUTTON_CONFIGS_TABLE = "button_configurations"
        private const val COLUMN_BTN_ID = "id"
        private const val COLUMN_BTN_IDENTIFIER = "button_identifier"
        private const val COLUMN_BTN_TITLE = "title"
        private const val COLUMN_BTN_ICON = "icon_name"
        private const val COLUMN_BTN_BACKGROUND = "background_drawable"
        private const val COLUMN_BTN_IS_ENABLED = "is_enabled"
        private const val COLUMN_BTN_VISIBILITY = "visibility"
        private const val COLUMN_BTN_ACTION = "action"
        private const val COLUMN_BTN_CATEGORY = "category"

        // App Texts table (stores all text content)
        private const val APP_TEXTS_TABLE = "app_texts"
        private const val COLUMN_TEXT_ID = "id"
        private const val COLUMN_TEXT_KEY = "text_key"
        private const val COLUMN_TEXT_VALUE = "text_value"
        private const val COLUMN_TEXT_SCREEN = "screen_name"
        private const val COLUMN_TEXT_ELEMENT = "element_name"
        private const val COLUMN_TEXT_LANGUAGE = "language_code"

        // Icons table (stores all icon references)
        private const val ICONS_TABLE = "icons"
        private const val COLUMN_ICON_ID = "id"
        private const val COLUMN_ICON_KEY = "icon_key"
        private const val COLUMN_ICON_RESOURCE_NAME = "resource_name"
        private const val COLUMN_ICON_CATEGORY = "category"
        private const val COLUMN_ICON_DESCRIPTION = "description"

        // Colors table (stores all color configurations)
        private const val COLORS_TABLE = "colors"
        private const val COLUMN_COLOR_ID = "id"
        private const val COLUMN_COLOR_KEY = "color_key"
        private const val COLUMN_COLOR_VALUE = "color_value"
        private const val COLUMN_COLOR_CATEGORY = "category"
        private const val COLUMN_COLOR_DESCRIPTION = "description"

        // Drawables table (stores background and drawable references)
        private const val DRAWABLES_TABLE = "drawables"
        private const val COLUMN_DRAWABLE_ID = "id"
        private const val COLUMN_DRAWABLE_KEY = "drawable_key"
        private const val COLUMN_DRAWABLE_RESOURCE_NAME = "resource_name"
        private const val COLUMN_DRAWABLE_TYPE = "drawable_type"
        private const val COLUMN_DRAWABLE_CATEGORY = "category"

        // Layout Elements table (stores dynamic layout configurations)
        private const val LAYOUT_ELEMENTS_TABLE = "layout_elements"
        private const val COLUMN_LAYOUT_ID = "id"
        private const val COLUMN_LAYOUT_SCREEN = "screen_name"
        private const val COLUMN_LAYOUT_ELEMENT_ID = "element_id"
        private const val COLUMN_LAYOUT_ELEMENT_TYPE = "element_type"
        private const val COLUMN_LAYOUT_TEXT_KEY = "text_key"
        private const val COLUMN_LAYOUT_ICON_KEY = "icon_key"
        private const val COLUMN_LAYOUT_COLOR_KEY = "color_key"
        private const val COLUMN_LAYOUT_DRAWABLE_KEY = "drawable_key"
        private const val COLUMN_LAYOUT_IS_VISIBLE = "is_visible"
        private const val COLUMN_LAYOUT_PROPERTIES = "properties_json"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create existing tables...
        createDetectionDataTable(db)
        createIngredientsTable(db)
        createRecipesTable(db)
        createRecipeIngredientsTable(db)
        createScanSessionsTable(db)
        createRecipeMatchesTable(db)

        // Create enhanced dynamic UI tables
        createUISettingsTable(db)
        createNavigationItemsTable(db)
        createButtonConfigurationsTable(db)
        createAppTextsTable(db)

        // Create new dynamic resource tables
        createIconsTable(db)
        createColorsTable(db)
        createDrawablesTable(db)
        createLayoutElementsTable(db)

        // Insert default data
        insertDefaultRecipes(db)
        insertDefaultUISettings(db)
        insertDefaultNavigationItems(db)
        insertDefaultButtonConfigurations(db)
        insertDefaultAppTexts(db)
        insertDefaultIcons(db)
        insertDefaultColors(db)
        insertDefaultDrawables(db)
        insertDefaultLayoutElements(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Drop all tables
        db?.execSQL("DROP TABLE IF EXISTS $DETECTIONS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $INGREDIENTS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $RECIPES_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $RECIPE_INGREDIENTS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $SCAN_SESSIONS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $RECIPE_MATCHES_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $UI_SETTINGS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $NAV_ITEMS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $BUTTON_CONFIGS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $APP_TEXTS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $ICONS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $COLORS_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $DRAWABLES_TABLE")
        db?.execSQL("DROP TABLE IF EXISTS $LAYOUT_ELEMENTS_TABLE")
        onCreate(db)
    }

    // Table creation methods
    private fun createDetectionDataTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $DETECTIONS_TABLE (
                $COLUMN_DETECTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DETECTION_CLASS_NAME TEXT NOT NULL,
                $COLUMN_DETECTION_CONFIDENCE REAL NOT NULL,
                $COLUMN_DETECTION_X1 REAL NOT NULL,
                $COLUMN_DETECTION_Y1 REAL NOT NULL,
                $COLUMN_DETECTION_X2 REAL NOT NULL,
                $COLUMN_DETECTION_Y2 REAL NOT NULL,
                $COLUMN_DETECTION_CX REAL NOT NULL,
                $COLUMN_DETECTION_CY REAL NOT NULL,
                $COLUMN_DETECTION_WIDTH REAL NOT NULL,
                $COLUMN_DETECTION_HEIGHT REAL NOT NULL,
                $COLUMN_DETECTION_CLS INTEGER NOT NULL,
                $COLUMN_DETECTION_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_DETECTION_FRAME_WIDTH INTEGER NOT NULL,
                $COLUMN_DETECTION_FRAME_HEIGHT INTEGER NOT NULL
            )
        """.trimIndent())
    }

    private fun createIngredientsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $INGREDIENTS_TABLE (
                $COLUMN_INGREDIENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_INGREDIENT_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_INGREDIENT_CATEGORY TEXT,
                $COLUMN_INGREDIENT_FIRST_DETECTED INTEGER NOT NULL,
                $COLUMN_INGREDIENT_LAST_DETECTED INTEGER NOT NULL,
                $COLUMN_INGREDIENT_DETECTION_COUNT INTEGER DEFAULT 1,
                $COLUMN_INGREDIENT_HIGHEST_CONFIDENCE REAL NOT NULL
            )
        """.trimIndent())
    }

    private fun createRecipesTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $RECIPES_TABLE (
                $COLUMN_RECIPE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECIPE_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_RECIPE_INSTRUCTIONS TEXT NOT NULL,
                $COLUMN_RECIPE_PREP_TIME INTEGER NOT NULL,
                $COLUMN_RECIPE_SERVINGS INTEGER DEFAULT 1,
                $COLUMN_RECIPE_DIFFICULTY TEXT DEFAULT 'easy',
                $COLUMN_RECIPE_CREATED_AT INTEGER DEFAULT (strftime('%s','now'))
            )
        """.trimIndent())
    }

    private fun createRecipeIngredientsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $RECIPE_INGREDIENTS_TABLE (
                $COLUMN_RI_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RI_RECIPE_ID INTEGER NOT NULL,
                $COLUMN_RI_INGREDIENT_NAME TEXT NOT NULL,
                $COLUMN_RI_QUANTITY TEXT,
                $COLUMN_RI_UNIT TEXT,
                FOREIGN KEY($COLUMN_RI_RECIPE_ID) REFERENCES $RECIPES_TABLE($COLUMN_RECIPE_ID),
                UNIQUE($COLUMN_RI_RECIPE_ID, $COLUMN_RI_INGREDIENT_NAME)
            )
        """.trimIndent())
    }

    private fun createScanSessionsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $SCAN_SESSIONS_TABLE (
                $COLUMN_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SESSION_START_TIME INTEGER NOT NULL,
                $COLUMN_SESSION_END_TIME INTEGER,
                $COLUMN_SESSION_TOTAL_DETECTIONS INTEGER DEFAULT 0,
                $COLUMN_SESSION_UNIQUE_INGREDIENTS INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    private fun createRecipeMatchesTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $RECIPE_MATCHES_TABLE (
                $COLUMN_MATCH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MATCH_SESSION_ID INTEGER,
                $COLUMN_MATCH_RECIPE_ID INTEGER NOT NULL,
                $COLUMN_MATCH_PERCENTAGE REAL NOT NULL,
                $COLUMN_MATCH_MATCHED_INGREDIENTS TEXT NOT NULL,
                $COLUMN_MATCH_CREATED_AT INTEGER DEFAULT (strftime('%s','now')),
                FOREIGN KEY($COLUMN_MATCH_SESSION_ID) REFERENCES $SCAN_SESSIONS_TABLE($COLUMN_SESSION_ID),
                FOREIGN KEY($COLUMN_MATCH_RECIPE_ID) REFERENCES $RECIPES_TABLE($COLUMN_RECIPE_ID)
            )
        """.trimIndent())
    }

    private fun createUISettingsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $UI_SETTINGS_TABLE (
                $COLUMN_UI_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_UI_KEY TEXT UNIQUE NOT NULL,
                $COLUMN_UI_VALUE TEXT NOT NULL,
                $COLUMN_UI_TYPE TEXT NOT NULL,
                $COLUMN_UI_CATEGORY TEXT,
                $COLUMN_UI_UPDATED_AT INTEGER DEFAULT (strftime('%s','now'))
            )
        """.trimIndent())
    }

    private fun createNavigationItemsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $NAV_ITEMS_TABLE (
                $COLUMN_NAV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAV_TITLE TEXT NOT NULL,
                $COLUMN_NAV_ICON TEXT NOT NULL,
                $COLUMN_NAV_POSITION INTEGER NOT NULL,
                $COLUMN_NAV_IS_ACTIVE BOOLEAN DEFAULT 1,
                $COLUMN_NAV_ACTION_TYPE TEXT NOT NULL,
                $COLUMN_NAV_TARGET TEXT
            )
        """.trimIndent())
    }

    private fun createButtonConfigurationsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $BUTTON_CONFIGS_TABLE (
                $COLUMN_BTN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BTN_IDENTIFIER TEXT UNIQUE NOT NULL,
                $COLUMN_BTN_TITLE TEXT,
                $COLUMN_BTN_ICON TEXT,
                $COLUMN_BTN_BACKGROUND TEXT,
                $COLUMN_BTN_IS_ENABLED BOOLEAN DEFAULT 1,
                $COLUMN_BTN_VISIBILITY INTEGER DEFAULT 0,
                $COLUMN_BTN_ACTION TEXT,
                $COLUMN_BTN_CATEGORY TEXT
            )
        """.trimIndent())
    }

    private fun createAppTextsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $APP_TEXTS_TABLE (
                $COLUMN_TEXT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT_KEY TEXT NOT NULL,
                $COLUMN_TEXT_VALUE TEXT NOT NULL,
                $COLUMN_TEXT_SCREEN TEXT NOT NULL,
                $COLUMN_TEXT_ELEMENT TEXT,
                $COLUMN_TEXT_LANGUAGE TEXT DEFAULT 'en',
                UNIQUE($COLUMN_TEXT_KEY, $COLUMN_TEXT_SCREEN, $COLUMN_TEXT_LANGUAGE)
            )
        """.trimIndent())
    }

    private fun createIconsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $ICONS_TABLE (
                $COLUMN_ICON_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ICON_KEY TEXT UNIQUE NOT NULL,
                $COLUMN_ICON_RESOURCE_NAME TEXT NOT NULL,
                $COLUMN_ICON_CATEGORY TEXT,
                $COLUMN_ICON_DESCRIPTION TEXT
            )
        """.trimIndent())
    }

    private fun createColorsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $COLORS_TABLE (
                $COLUMN_COLOR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COLOR_KEY TEXT UNIQUE NOT NULL,
                $COLUMN_COLOR_VALUE TEXT NOT NULL,
                $COLUMN_COLOR_CATEGORY TEXT,
                $COLUMN_COLOR_DESCRIPTION TEXT
            )
        """.trimIndent())
    }

    private fun createDrawablesTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $DRAWABLES_TABLE (
                $COLUMN_DRAWABLE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DRAWABLE_KEY TEXT UNIQUE NOT NULL,
                $COLUMN_DRAWABLE_RESOURCE_NAME TEXT NOT NULL,
                $COLUMN_DRAWABLE_TYPE TEXT,
                $COLUMN_DRAWABLE_CATEGORY TEXT
            )
        """.trimIndent())
    }

    private fun createLayoutElementsTable(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE $LAYOUT_ELEMENTS_TABLE (
                $COLUMN_LAYOUT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LAYOUT_SCREEN TEXT NOT NULL,
                $COLUMN_LAYOUT_ELEMENT_ID TEXT NOT NULL,
                $COLUMN_LAYOUT_ELEMENT_TYPE TEXT NOT NULL,
                $COLUMN_LAYOUT_TEXT_KEY TEXT,
                $COLUMN_LAYOUT_ICON_KEY TEXT,
                $COLUMN_LAYOUT_COLOR_KEY TEXT,
                $COLUMN_LAYOUT_DRAWABLE_KEY TEXT,
                $COLUMN_LAYOUT_IS_VISIBLE BOOLEAN DEFAULT 1,
                $COLUMN_LAYOUT_PROPERTIES TEXT,
                UNIQUE($COLUMN_LAYOUT_SCREEN, $COLUMN_LAYOUT_ELEMENT_ID)
            )
        """.trimIndent())
    }

    // Data classes for dynamic UI
    data class UISettingData(
        val key: String,
        val value: String,
        val type: String,
        val category: String
    )

    data class NavigationItemData(
        val title: String,
        val icon: String,
        val position: Int,
        val isActive: Boolean,
        val actionType: String,
        val target: String?
    )

    data class ButtonConfigData(
        val identifier: String,
        val title: String,
        val icon: String,
        val background: String,
        val isEnabled: Boolean,
        val visibility: Int, // 0 = VISIBLE, 4 = INVISIBLE, 8 = GONE
        val action: String,
        val category: String
    )

    data class AppTextData(
        val key: String,
        val value: String,
        val screen: String,
        val element: String
    )

    data class IconData(
        val key: String,
        val resourceName: String,
        val category: String,
        val description: String
    )

    data class ColorData(
        val key: String,
        val value: String,
        val category: String,
        val description: String
    )

    data class DrawableData(
        val key: String,
        val resourceName: String,
        val type: String,
        val category: String
    )

    data class LayoutElementData(
        val screen: String,
        val elementId: String,
        val elementType: String,
        val textKey: String?,
        val iconKey: String?,
        val colorKey: String?,
        val drawableKey: String?,
        val isVisible: Boolean,
        val properties: String
    )

    // Insert default data methods
    private fun insertDefaultUISettings(db: SQLiteDatabase?) {
        val settings = listOf(
            UISettingData("app_primary_color", "#2E6700", "color", "theme"),
            UISettingData("app_secondary_color", "#326500", "color", "theme"),
            UISettingData("app_light_green", "#CCFFCC", "color", "theme"),
            UISettingData("app_white", "#FFFFFF", "color", "theme"),
            UISettingData("app_black", "#000000", "color", "theme"),
            UISettingData("camera_preview_enabled", "true", "boolean", "camera"),
            UISettingData("detection_overlay_enabled", "true", "boolean", "detection"),
            UISettingData("inference_time_visible", "false", "boolean", "debug"),
            UISettingData("detection_count_visible", "false", "boolean", "debug"),
            UISettingData("scan_button_size", "86", "integer", "ui"),
            UISettingData("corner_button_size", "55", "integer", "ui"),
            UISettingData("help_button_size", "45", "integer", "ui")
        )

        settings.forEach { setting ->
            val values = ContentValues().apply {
                put(COLUMN_UI_KEY, setting.key)
                put(COLUMN_UI_VALUE, setting.value)
                put(COLUMN_UI_TYPE, setting.type)
                put(COLUMN_UI_CATEGORY, setting.category)
            }
            db?.insert(UI_SETTINGS_TABLE, null, values)
        }
    }

    private fun insertDefaultNavigationItems(db: SQLiteDatabase?) {
        val navItems = listOf(
            NavigationItemData("Recipe", "nav_recipe", 0, true, "activity", "recipe_activity"),
            NavigationItemData("Camera", "nav_camera", 1, true, "activity", "camera_activity"),
            NavigationItemData("Favorites", "nav_favorite", 2, true, "activity", "favorites_activity"),
            NavigationItemData("Profile", "nav_person", 3, true, "activity", "profile_activity")
        )

        navItems.forEach { item ->
            val values = ContentValues().apply {
                put(COLUMN_NAV_TITLE, item.title)
                put(COLUMN_NAV_ICON, item.icon)
                put(COLUMN_NAV_POSITION, item.position)
                put(COLUMN_NAV_IS_ACTIVE, item.isActive)
                put(COLUMN_NAV_ACTION_TYPE, item.actionType)
                put(COLUMN_NAV_TARGET, item.target)
            }
            db?.insert(NAV_ITEMS_TABLE, null, values)
        }
    }

    private fun insertDefaultButtonConfigurations(db: SQLiteDatabase?) {
        val buttons = listOf(
            ButtonConfigData("btn_close_ingredients", "Close", "icon_close", "bg_transparent", true, 0, "close_dialog", "dialog"),
            ButtonConfigData("btn_close_recipe_detail", "Close", "icon_close_white", "bg_transparent", true, 0, "close_dialog", "dialog"),
            ButtonConfigData("btn_close_recipes", "Close", "icon_close_white", "bg_transparent", true, 0, "close_dialog", "dialog"),
            ButtonConfigData("btn_recommend_recipe", "Recommend Recipes", "", "bg_green_button", true, 0, "recommend_recipes", "dialog"),
            ButtonConfigData("btn_ingredient_edit", "Edit", "icon_edit", "bg_transparent", true, 0, "edit_ingredient", "ingredient"),
            ButtonConfigData("btn_ingredient_delete", "Delete", "icon_delete", "bg_transparent", true, 0, "delete_ingredient", "ingredient"),
            ButtonConfigData("btn_help", "Help", "ic_help", "circle_background", true, 0, "show_help", "header"),
            ButtonConfigData("btn_home", "Home", "clear_x", "circle_white_background", true, 0, "go_home", "bottom"),
            ButtonConfigData("btn_scan", "Scan", "ic_scan", "circle_green_background", true, 0, "toggle_scan", "bottom"),
            ButtonConfigData("btn_show_data", "Show Data", "ic_showdata", "circle_gray_background", true, 0, "show_data", "bottom"),
            ButtonConfigData("btn_clear_data", "Clear Data", "ic_menu_delete", "", true, 8, "clear_data", "floating")
        )

        buttons.forEach { button ->
            val values = ContentValues().apply {
                put(COLUMN_BTN_IDENTIFIER, button.identifier)
                put(COLUMN_BTN_TITLE, button.title)
                put(COLUMN_BTN_ICON, button.icon)
                put(COLUMN_BTN_BACKGROUND, button.background)
                put(COLUMN_BTN_IS_ENABLED, button.isEnabled)
                put(COLUMN_BTN_VISIBILITY, button.visibility)
                put(COLUMN_BTN_ACTION, button.action)
                put(COLUMN_BTN_CATEGORY, button.category)
            }
            db?.insert(BUTTON_CONFIGS_TABLE, null, values)
        }
    }

    private fun insertDefaultAppTexts(db: SQLiteDatabase?) {
        val texts = listOf(
            // Dialog Ingredients Screen
            AppTextData("title_scanned_ingredients", "Scanned Ingredients", "dialog_ingredients", "title"),
            AppTextData("btn_close", "Close", "dialog_ingredients", "button"),
            AppTextData("btn_recommend_recipes", "Recommend Recipes", "dialog_ingredients", "button"),

            // Dialog Recipe Detail Screen
            AppTextData("title_recipe_name", "Recipe Name", "dialog_recipe_detail", "title"),
            AppTextData("label_ingredients", "Ingredients:", "dialog_recipe_detail", "label"),
            AppTextData("label_instructions", "Instructions:", "dialog_recipe_detail", "label"),
            AppTextData("label_prep_time", "Preparation time: %d minutes", "dialog_recipe_detail", "label"),
            AppTextData("sample_ingredients", "- Ingredient 1\n- Ingredient 2\n- Ingredient 3", "dialog_recipe_detail", "content"),
            AppTextData("sample_instructions", "1. Step one\n2. Step two\n3. Step three", "dialog_recipe_detail", "content"),

            // Dialog Recipes Screen
            AppTextData("title_recipe_suggestions", "Recipe Suggestions", "dialog_recipes", "title"),
            AppTextData("label_ingredients_used", "Ingredients used: %s", "dialog_recipes", "label"),

            // Main Activity
            AppTextData("app_title", "Ingredient Scanner", "main_activity", "title"),
            AppTextData("detection_count_label", "Detections: %d", "main_activity", "label"),
            AppTextData("inference_time_label", "%dms", "main_activity", "label"),

            // Item Detection
            AppTextData("label_confidence", "Confidence: %.2f", "item_detection", "label"),
            AppTextData("label_position", "Position: (%.1f, %.1f)", "item_detection", "label"),
            AppTextData("label_timestamp", "Time: %s", "item_detection", "label"),

            // Item Recipe
            AppTextData("label_recipe_match", "Ingredients match: %.0f%%", "item_recipe", "label"),
            AppTextData("label_prep_time_item", "Prep time: %d minutes", "item_recipe", "label"),

            // Common
            AppTextData("sample_recipe_name", "Recipe Name", "all", "placeholder"),
            AppTextData("sample_person", "Person", "all", "placeholder"),
            AppTextData("close_button", "Close", "all", "button")
        )

        texts.forEach { text ->
            val values = ContentValues().apply {
                put(COLUMN_TEXT_KEY, text.key)
                put(COLUMN_TEXT_VALUE, text.value)
                put(COLUMN_TEXT_SCREEN, text.screen)
                put(COLUMN_TEXT_ELEMENT, text.element)
                put(COLUMN_TEXT_LANGUAGE, "en")
            }
            db?.insert(APP_TEXTS_TABLE, null, values)
        }
    }

    private fun insertDefaultIcons(db: SQLiteDatabase?) {
        val icons = listOf(
            IconData("icon_close", "ic_close", "dialog", "Close dialog icon"),
            IconData("icon_close_white", "ic_menu_close_clear_cancel", "dialog", "Close dialog icon (white)"),
            IconData("icon_edit", "ic_edit", "ingredient", "Edit ingredient icon"),
            IconData("icon_delete", "ic_delete", "ingredient", "Delete ingredient icon"),
            IconData("nav_recipe", "ic_recipe", "navigation", "Recipe navigation icon"),
            IconData("nav_camera", "ic_camera", "navigation", "Camera navigation icon"),
            IconData("nav_favorite", "ic_favorite", "navigation", "Favorites navigation icon"),
            IconData("nav_person", "ic_person", "navigation", "Profile navigation icon"),
            IconData("ic_help", "ic_help", "header", "Help button icon"),
            IconData("clear_x", "ic_clear", "button", "Clear/close icon"),
            IconData("ic_scan", "ic_scan", "button", "Scan button icon"),
            IconData("ic_showdata", "ic_showdata", "button", "Show data icon"),
            IconData("ic_menu_delete", "ic_menu_delete", "button", "Delete menu icon")
        )

        icons.forEach { icon ->
            val values = ContentValues().apply {
                put(COLUMN_ICON_KEY, icon.key)
                put(COLUMN_ICON_RESOURCE_NAME, icon.resourceName)
                put(COLUMN_ICON_CATEGORY, icon.category)
                put(COLUMN_ICON_DESCRIPTION, icon.description)
            }
            db?.insert(ICONS_TABLE, null, values)
        }
    }

    private fun insertDefaultColors(db: SQLiteDatabase?) {
        val colors = listOf(
            ColorData("color_primary", "#2E6700", "theme", "Primary green color"),
            ColorData("color_secondary", "#326500", "theme", "Secondary green color"),
            ColorData("color_light_green", "#CCFFCC", "theme", "Light green background"),
            ColorData("color_white", "#FFFFFF", "theme", "White color"),
            ColorData("color_black", "#000000", "theme", "Black color"),
            ColorData("color_green_dishcovery", "#2E6700", "theme", "Dishcovery brand green"),
            ColorData("color_lightgreen_dishcovery", "#CCFFCC", "theme", "Dishcovery light green"),
            ColorData("color_text_dark", "#2A5D00", "text", "Dark green text"),
            ColorData("color_text_gray", "#4A4E48", "text", "Gray text color")
        )

        colors.forEach { color ->
            val values = ContentValues().apply {
                put(COLUMN_COLOR_KEY, color.key)
                put(COLUMN_COLOR_VALUE, color.value)
                put(COLUMN_COLOR_CATEGORY, color.category)
                put(COLUMN_COLOR_DESCRIPTION, color.description)
            }
            db?.insert(COLORS_TABLE, null, values)
        }
    }

    private fun insertDefaultDrawables(db: SQLiteDatabase?) {
        val drawables = listOf(
            DrawableData("bg_ingredient", "ingredient_background", "background", "ingredient"),
            DrawableData("bg_green_button", "green_button_background", "background", "button"),
            DrawableData("bg_nav_item", "nav_item_background", "background", "navigation"),
            DrawableData("bg_transparent", "transparent_background", "background", "common"),
            DrawableData("bg_card", "card_background", "background", "card"),
            DrawableData("bg_dialog", "dialog_background", "background", "dialog"),
            DrawableData("circle_background", "circle_background", "background", "button"),
            DrawableData("circle_white_background", "circle_white_background", "background", "button"),
            DrawableData("circle_green_background", "circle_green_background", "background", "button"),
            DrawableData("circle_gray_background", "circle_gray_background", "background", "button")
        )

        drawables.forEach { drawable ->
            val values = ContentValues().apply {
                put(COLUMN_DRAWABLE_KEY, drawable.key)
                put(COLUMN_DRAWABLE_RESOURCE_NAME, drawable.resourceName)
                put(COLUMN_DRAWABLE_TYPE, drawable.type)
                put(COLUMN_DRAWABLE_CATEGORY, drawable.category)
            }
            db?.insert(DRAWABLES_TABLE, null, values)
        }
    }

    private fun insertDefaultLayoutElements(db: SQLiteDatabase?) {
        val layoutElements = listOf(
            // Dialog Ingredients Screen Elements
            LayoutElementData("dialog_ingredients", "title", "TextView", "title_scanned_ingredients", null, "color_secondary", null, true, ""),
            LayoutElementData("dialog_ingredients", "btnCloseDialog", "ImageButton", "btn_close", "icon_close", null, "bg_transparent", true, ""),
            LayoutElementData("dialog_ingredients", "btnRecommendRecipe", "Button", "btn_recommend_recipes", null, "color_white", "bg_green_button", true, ""),
            LayoutElementData("dialog_ingredients", "ingredientEdit", "ImageView", null, "icon_edit", null, null, true, ""),
            LayoutElementData("dialog_ingredients", "ingredientDelete", "ImageView", null, "icon_delete", null, null, true, ""),

            // Dialog Recipe Detail Screen Elements
            LayoutElementData("dialog_recipe_detail", "recipeDetailTitle", "TextView", "title_recipe_name", null, "color_white", null, true, ""),
            LayoutElementData("dialog_recipe_detail", "btnCloseRecipeDetailDialog", "ImageButton", null, "icon_close_white", "color_white", "bg_transparent", true, ""),
            LayoutElementData("dialog_recipe_detail", "labelIngredients", "TextView", "label_ingredients", null, "color_green_dishcovery", null, true, ""),
            LayoutElementData("dialog_recipe_detail", "recipeDetailIngredients", "TextView", "sample_ingredients", null, "color_black", null, true, ""),
            LayoutElementData("dialog_recipe_detail", "labelInstructions", "TextView", "label_instructions", null, "color_green_dishcovery", null, true, ""),
            LayoutElementData("dialog_recipe_detail", "recipeDetailInstructions", "TextView", "sample_instructions", null, "color_black", null, true, ""),
            LayoutElementData("dialog_recipe_detail", "recipeDetailPrepTime", "TextView", "label_prep_time", null, "color_text_gray", null, true, ""),

            // Dialog Recipes Screen Elements
            LayoutElementData("dialog_recipes", "titleRecipeSuggestions", "TextView", "title_recipe_suggestions", null, "color_white", null, true, ""),
            LayoutElementData("dialog_recipes", "btnCloseRecipesDialog", "ImageButton", null, "icon_close_white", "color_white", "bg_transparent", true, ""),
            LayoutElementData("dialog_recipes", "recipeIngredientsUsed", "TextView", "label_ingredients_used", null, "color_text_gray", null, true, ""),

            // Item Detection Elements
            LayoutElementData("item_detection", "objectNameText", "TextView", "sample_person", null, "color_black", null, true, ""),
            LayoutElementData("item_detection", "confidenceText", "TextView", "label_confidence", null, "color_black", null, true, ""),
            LayoutElementData("item_detection", "positionText", "TextView", "label_position", null, "color_black", null, true, ""),
            LayoutElementData("item_detection", "timestampText", "TextView", "label_timestamp", null, "color_black", null, true, ""),

            // Item Recipe Elements
            LayoutElementData("item_recipe", "recipeName", "TextView", "sample_recipe_name", null, "color_text_dark", null, true, ""),
            LayoutElementData("item_recipe", "recipeMatch", "TextView", "label_recipe_match", null, "color_black", null, true, ""),
            LayoutElementData("item_recipe", "recipePrepTime", "TextView", "label_prep_time_item", null, "color_black", null, true, ""),

            // Navigation Elements
            LayoutElementData("navigation", "navRecipe", "TextView", null, "nav_recipe", "color_primary", "bg_nav_item", true, ""),
            LayoutElementData("navigation", "navCamera", "ImageView", null, "nav_camera", null, "bg_nav_item", true, ""),
            LayoutElementData("navigation", "navFavorite", "ImageView", null, "nav_favorite", null, "bg_nav_item", true, ""),
            LayoutElementData("navigation", "navPerson", "ImageView", null, "nav_person", null, "bg_nav_item", true, "")
        )

        layoutElements.forEach { element ->
            val values = ContentValues().apply {
                put(COLUMN_LAYOUT_SCREEN, element.screen)
                put(COLUMN_LAYOUT_ELEMENT_ID, element.elementId)
                put(COLUMN_LAYOUT_ELEMENT_TYPE, element.elementType)
                put(COLUMN_LAYOUT_TEXT_KEY, element.textKey)
                put(COLUMN_LAYOUT_ICON_KEY, element.iconKey)
                put(COLUMN_LAYOUT_COLOR_KEY, element.colorKey)
                put(COLUMN_LAYOUT_DRAWABLE_KEY, element.drawableKey)
                put(COLUMN_LAYOUT_IS_VISIBLE, element.isVisible)
                put(COLUMN_LAYOUT_PROPERTIES, element.properties)
            }
            db?.insert(LAYOUT_ELEMENTS_TABLE, null, values)
        }
    }

    private fun insertDefaultRecipes(db: SQLiteDatabase?) {
        val recipes = listOf(
            Triple("Banana Apple Smoothie", "1. Peel and chop the apple and banana\n2. Blend with ice and a splash of water\n3. Serve cold", 5),
            Triple("Carrot Apple Salad", "1. Grate the apple and carrot\n2. Mix together in a bowl\n3. Add a drizzle of honey (optional)\n4. Serve fresh", 10),
            Triple("Scrambled Eggs with Tomato", "1. Beat the eggs in a bowl\n2. Dice the tomato\n3. Heat a pan and cook the eggs\n4. Add tomatoes when eggs are halfway done\n5. Season with salt and pepper", 8),
            Triple("Carrot Tomato Soup", "1. Chop carrots and tomatoes\n2. Boil in water until soft\n3. Blend until smooth\n4. Season with salt and herbs", 20),
            Triple("Banana Egg Pancakes", "1. Mash the banana\n2. Beat the eggs\n3. Mix banana and eggs together\n4. Pour small amounts into a hot pan\n5. Flip when bubbles appear\n6. Serve warm", 15),
            Triple("Veggie Breakfast Scramble", "1. Dice tomato and grate carrot\n2. Beat eggs in a bowl\n3. Cook vegetables in a pan until soft\n4. Add eggs and scramble together\n5. Season to taste", 12),
            Triple("Fruity Carrot Smoothie", "1. Peel and chop apple and banana\n2. Grate or chop carrot\n3. Blend all ingredients with ice\n4. Add a little water if needed\n5. Serve immediately", 7)
        )

        val recipeIngredients = mapOf(
            "Banana Apple Smoothie" to listOf("Apple", "Banana"),
            "Carrot Apple Salad" to listOf("Apple", "Carrot"),
            "Scrambled Eggs with Tomato" to listOf("Egg", "Tomato"),
            "Carrot Tomato Soup" to listOf("Carrot", "Tomato"),
            "Banana Egg Pancakes" to listOf("Banana", "Egg"),
            "Veggie Breakfast Scramble" to listOf("Egg", "Tomato", "Carrot"),
            "Fruity Carrot Smoothie" to listOf("Apple", "Banana", "Carrot")
        )

        recipes.forEach { (name, instructions, prepTime) ->
            val values = ContentValues().apply {
                put(COLUMN_RECIPE_NAME, name)
                put(COLUMN_RECIPE_INSTRUCTIONS, instructions)
                put(COLUMN_RECIPE_PREP_TIME, prepTime)
                put(COLUMN_RECIPE_SERVINGS, 2)
                put(COLUMN_RECIPE_DIFFICULTY, "easy")
            }
            val recipeId = db?.insert(RECIPES_TABLE, null, values)

            // Insert recipe ingredients
            recipeIngredients[name]?.forEach { ingredient ->
                val ingredientValues = ContentValues().apply {
                    put(COLUMN_RI_RECIPE_ID, recipeId)
                    put(COLUMN_RI_INGREDIENT_NAME, ingredient)
                    put(COLUMN_RI_QUANTITY, "1")
                    put(COLUMN_RI_UNIT, "piece")
                }
                db?.insert(RECIPE_INGREDIENTS_TABLE, null, ingredientValues)
            }
        }
    }

    // EXISTING METHODS (Detection Data, Ingredients, Recipes, etc.) - Enhanced with Dynamic UI

    fun saveDetectionData(detectionData: DetectionData): Long {
        val values = ContentValues().apply {
            put(COLUMN_DETECTION_CLASS_NAME, detectionData.boundingBox.clsName)
            put(COLUMN_DETECTION_CONFIDENCE, detectionData.boundingBox.cnf)
            put(COLUMN_DETECTION_X1, detectionData.boundingBox.x1)
            put(COLUMN_DETECTION_Y1, detectionData.boundingBox.y1)
            put(COLUMN_DETECTION_X2, detectionData.boundingBox.x2)
            put(COLUMN_DETECTION_Y2, detectionData.boundingBox.y2)
            put(COLUMN_DETECTION_CX, detectionData.boundingBox.cx)
            put(COLUMN_DETECTION_CY, detectionData.boundingBox.cy)
            put(COLUMN_DETECTION_WIDTH, detectionData.boundingBox.w)
            put(COLUMN_DETECTION_HEIGHT, detectionData.boundingBox.h)
            put(COLUMN_DETECTION_CLS, detectionData.boundingBox.cls)
            put(COLUMN_DETECTION_TIMESTAMP, detectionData.timestamp)
            put(COLUMN_DETECTION_FRAME_WIDTH, detectionData.frameWidth)
            put(COLUMN_DETECTION_FRAME_HEIGHT, detectionData.frameHeight)
        }

        val result = writableDatabase.insert(DETECTIONS_TABLE, null, values)

        // Also update or insert ingredient data
        updateIngredientData(detectionData.boundingBox.clsName, detectionData.boundingBox.cnf, detectionData.timestamp)

        return result
    }

    fun getAllDetections(): List<DetectionData> {
        val detections = mutableListOf<DetectionData>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $DETECTIONS_TABLE ORDER BY $COLUMN_DETECTION_TIMESTAMP DESC", null)

        cursor.use {
            while (it.moveToNext()) {
                val boundingBox = BoundingBox(
                    x1 = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_X1)),
                    y1 = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_Y1)),
                    x2 = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_X2)),
                    y2 = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_Y2)),
                    cx = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_CX)),
                    cy = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_CY)),
                    w = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_WIDTH)),
                    h = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_HEIGHT)),
                    cnf = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DETECTION_CONFIDENCE)),
                    cls = it.getInt(it.getColumnIndexOrThrow(COLUMN_DETECTION_CLS)),
                    clsName = it.getString(it.getColumnIndexOrThrow(COLUMN_DETECTION_CLASS_NAME))
                )

                val detectionData = DetectionData(
                    boundingBox = boundingBox,
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_DETECTION_TIMESTAMP)),
                    frameWidth = it.getInt(it.getColumnIndexOrThrow(COLUMN_DETECTION_FRAME_WIDTH)),
                    frameHeight = it.getInt(it.getColumnIndexOrThrow(COLUMN_DETECTION_FRAME_HEIGHT))
                )
                detections.add(detectionData)
            }
        }
        return detections
    }

    fun clearAllDetections(): Int {
        val detectionCount = writableDatabase.delete(DETECTIONS_TABLE, null, null)
        val ingredientCount = writableDatabase.delete(INGREDIENTS_TABLE, null, null)
        Log.d(TAG, "Cleared $detectionCount detections and $ingredientCount ingredients")
        return detectionCount
    }

    fun getDetectionCount(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $DETECTIONS_TABLE", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // INGREDIENT METHODS

    private fun updateIngredientData(ingredientName: String, confidence: Float, timestamp: Long) {
        val cursor = readableDatabase.query(
            INGREDIENTS_TABLE,
            arrayOf(COLUMN_INGREDIENT_ID, COLUMN_INGREDIENT_DETECTION_COUNT, COLUMN_INGREDIENT_HIGHEST_CONFIDENCE),
            "$COLUMN_INGREDIENT_NAME = ?",
            arrayOf(ingredientName),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                // Update existing ingredient
                val currentCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_DETECTION_COUNT))
                val currentHighest = it.getFloat(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_HIGHEST_CONFIDENCE))

                val values = ContentValues().apply {
                    put(COLUMN_INGREDIENT_LAST_DETECTED, timestamp)
                    put(COLUMN_INGREDIENT_DETECTION_COUNT, currentCount + 1)
                    put(COLUMN_INGREDIENT_HIGHEST_CONFIDENCE, maxOf(currentHighest, confidence))
                }

                writableDatabase.update(
                    INGREDIENTS_TABLE, values,
                    "$COLUMN_INGREDIENT_NAME = ?", arrayOf(ingredientName)
                )
            } else {
                // Insert new ingredient
                val values = ContentValues().apply {
                    put(COLUMN_INGREDIENT_NAME, ingredientName)
                    put(COLUMN_INGREDIENT_CATEGORY, getIngredientCategory(ingredientName))
                    put(COLUMN_INGREDIENT_FIRST_DETECTED, timestamp)
                    put(COLUMN_INGREDIENT_LAST_DETECTED, timestamp)
                    put(COLUMN_INGREDIENT_DETECTION_COUNT, 1)
                    put(COLUMN_INGREDIENT_HIGHEST_CONFIDENCE, confidence)
                }
                writableDatabase.insert(INGREDIENTS_TABLE, null, values)
            }
        }
    }

    fun getUniqueIngredientsWithHighConfidence(confidenceThreshold: Float = 0.6f): List<IngredientData> {
        val ingredients = mutableListOf<IngredientData>()
        val cursor = readableDatabase.query(
            INGREDIENTS_TABLE,
            null,
            "$COLUMN_INGREDIENT_HIGHEST_CONFIDENCE >= ?",
            arrayOf(confidenceThreshold.toString()),
            null, null,
            "$COLUMN_INGREDIENT_HIGHEST_CONFIDENCE DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                // Create a dummy DetectionData for compatibility
                val dummyBoundingBox = BoundingBox(
                    x1 = 0f, y1 = 0f, x2 = 1f, y2 = 1f,
                    cx = 0.5f, cy = 0.5f, w = 1f, h = 1f,
                    cnf = it.getFloat(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_HIGHEST_CONFIDENCE)),
                    cls = 0,
                    clsName = it.getString(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_NAME))
                )

                val dummyDetectionData = DetectionData(
                    boundingBox = dummyBoundingBox,
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_LAST_DETECTED)),
                    frameWidth = 640,
                    frameHeight = 480
                )

                val ingredientData = IngredientData(
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_INGREDIENT_NAME)),
                    detectionData = dummyDetectionData
                )
                ingredients.add(ingredientData)
            }
        }
        return ingredients
    }

    private fun getIngredientCategory(ingredientName: String): String {
        return when (ingredientName.lowercase()) {
            "apple", "banana" -> "fruit"
            "carrot", "tomato" -> "vegetable"
            "egg" -> "protein"
            else -> "other"
        }
    }

    // RECIPE METHODS

    fun getAllRecipes(): List<RecipeData> {
        val recipes = mutableListOf<RecipeData>()
        val cursor = readableDatabase.rawQuery("""
            SELECT * FROM $RECIPES_TABLE ORDER BY $COLUMN_RECIPE_NAME
        """.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()) {
                val recipeId = it.getLong(it.getColumnIndexOrThrow(COLUMN_RECIPE_ID))
                val ingredients = getRecipeIngredients(recipeId)

                val recipe = RecipeData(
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_RECIPE_NAME)),
                    ingredients = ingredients,
                    instructions = it.getString(it.getColumnIndexOrThrow(COLUMN_RECIPE_INSTRUCTIONS)),
                    prepTime = it.getInt(it.getColumnIndexOrThrow(COLUMN_RECIPE_PREP_TIME))
                )
                recipes.add(recipe)
            }
        }
        return recipes
    }

    private fun getRecipeIngredients(recipeId: Long): List<String> {
        val ingredients = mutableListOf<String>()
        val cursor = readableDatabase.query(
            RECIPE_INGREDIENTS_TABLE,
            arrayOf(COLUMN_RI_INGREDIENT_NAME),
            "$COLUMN_RI_RECIPE_ID = ?",
            arrayOf(recipeId.toString()),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                ingredients.add(it.getString(it.getColumnIndexOrThrow(COLUMN_RI_INGREDIENT_NAME)))
            }
        }
        return ingredients
    }

    fun findRecipesWithIngredients(detectedIngredients: List<String>): List<RecipeMatch> {
        val recipeMatches = mutableListOf<RecipeMatch>()
        val allRecipes = getAllRecipes()

        for (recipe in allRecipes) {
            val matchedIngredients = recipe.ingredients.filter { ingredient ->
                detectedIngredients.contains(ingredient)
            }

            if (matchedIngredients.isNotEmpty()) {
                val matchPercentage = matchedIngredients.size.toFloat() / recipe.ingredients.size

                val recipeMatch = RecipeMatch(
                    recipe = recipe,
                    matchPercentage = matchPercentage,
                    matchedIngredients = matchedIngredients
                )
                recipeMatches.add(recipeMatch)
            }
        }

        return recipeMatches.sortedByDescending { it.matchPercentage }
    }

    // SCAN SESSION METHODS

    fun startScanSession(): Long {
        val values = ContentValues().apply {
            put(COLUMN_SESSION_START_TIME, System.currentTimeMillis())
        }
        return writableDatabase.insert(SCAN_SESSIONS_TABLE, null, values)
    }

    fun endScanSession(sessionId: Long) {
        val detectionCount = getDetectionCount()
        val uniqueIngredientCount = getUniqueIngredientsWithHighConfidence().size

        val values = ContentValues().apply {
            put(COLUMN_SESSION_END_TIME, System.currentTimeMillis())
            put(COLUMN_SESSION_TOTAL_DETECTIONS, detectionCount)
            put(COLUMN_SESSION_UNIQUE_INGREDIENTS, uniqueIngredientCount)
        }

        writableDatabase.update(
            SCAN_SESSIONS_TABLE, values,
            "$COLUMN_SESSION_ID = ?", arrayOf(sessionId.toString())
        )
    }

    fun saveRecipeMatches(sessionId: Long, recipeMatches: List<RecipeMatch>) {
        recipeMatches.forEach { match ->
            val values = ContentValues().apply {
                put(COLUMN_MATCH_SESSION_ID, sessionId)
                put(COLUMN_MATCH_RECIPE_ID, getRecipeIdByName(match.recipe.name))
                put(COLUMN_MATCH_PERCENTAGE, match.matchPercentage)
                put(COLUMN_MATCH_MATCHED_INGREDIENTS, match.matchedIngredients.joinToString(","))
            }
            writableDatabase.insert(RECIPE_MATCHES_TABLE, null, values)
        }
    }

    private fun getRecipeIdByName(recipeName: String): Long {
        val cursor = readableDatabase.query(
            RECIPES_TABLE,
            arrayOf(COLUMN_RECIPE_ID),
            "$COLUMN_RECIPE_NAME = ?",
            arrayOf(recipeName),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(COLUMN_RECIPE_ID))
            } else -1
        }
    }

    // EXISTING UI METHODS (Enhanced)

    fun getUISetting(key: String): String? {
        val cursor = readableDatabase.query(
            UI_SETTINGS_TABLE,
            arrayOf(COLUMN_UI_VALUE),
            "$COLUMN_UI_KEY = ?",
            arrayOf(key),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(COLUMN_UI_VALUE))
            } else null
        }
    }

    fun updateUISetting(key: String, value: String): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_UI_VALUE, value)
            put(COLUMN_UI_UPDATED_AT, System.currentTimeMillis())
        }

        val rowsAffected = writableDatabase.update(
            UI_SETTINGS_TABLE, values,
            "$COLUMN_UI_KEY = ?", arrayOf(key)
        )
        return rowsAffected > 0
    }

    fun getNavigationItems(): List<NavigationItemData> {
        val items = mutableListOf<NavigationItemData>()
        val cursor = readableDatabase.query(
            NAV_ITEMS_TABLE, null,
            "$COLUMN_NAV_IS_ACTIVE = ?", arrayOf("1"),
            null, null, "$COLUMN_NAV_POSITION ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val item = NavigationItemData(
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_NAV_TITLE)),
                    icon = it.getString(it.getColumnIndexOrThrow(COLUMN_NAV_ICON)),
                    position = it.getInt(it.getColumnIndexOrThrow(COLUMN_NAV_POSITION)),
                    isActive = it.getInt(it.getColumnIndexOrThrow(COLUMN_NAV_IS_ACTIVE)) == 1,
                    actionType = it.getString(it.getColumnIndexOrThrow(COLUMN_NAV_ACTION_TYPE)),
                    target = it.getString(it.getColumnIndexOrThrow(COLUMN_NAV_TARGET))
                )
                items.add(item)
            }
        }
        return items
    }

    fun getButtonConfig(identifier: String): ButtonConfigData? {
        val cursor = readableDatabase.query(
            BUTTON_CONFIGS_TABLE, null,
            "$COLUMN_BTN_IDENTIFIER = ?", arrayOf(identifier),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                ButtonConfigData(
                    identifier = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_IDENTIFIER)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_TITLE)) ?: "",
                    icon = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_ICON)) ?: "",
                    background = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_BACKGROUND)) ?: "",
                    isEnabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_BTN_IS_ENABLED)) == 1,
                    visibility = it.getInt(it.getColumnIndexOrThrow(COLUMN_BTN_VISIBILITY)),
                    action = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_ACTION)) ?: "",
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_CATEGORY)) ?: ""
                )
            } else null
        }
    }

    fun updateButtonConfig(identifier: String, isEnabled: Boolean? = null, visibility: Int? = null): Boolean {
        val values = ContentValues()
        isEnabled?.let { values.put(COLUMN_BTN_IS_ENABLED, it) }
        visibility?.let { values.put(COLUMN_BTN_VISIBILITY, it) }

        if (values.size() == 0) return false

        val rowsAffected = writableDatabase.update(
            BUTTON_CONFIGS_TABLE, values,
            "$COLUMN_BTN_IDENTIFIER = ?", arrayOf(identifier)
        )
        return rowsAffected > 0
    }

    fun getAppText(key: String, screen: String, language: String = "en"): String? {
        val cursor = readableDatabase.query(
            APP_TEXTS_TABLE,
            arrayOf(COLUMN_TEXT_VALUE),
            "$COLUMN_TEXT_KEY = ? AND $COLUMN_TEXT_SCREEN = ? AND $COLUMN_TEXT_LANGUAGE = ?",
            arrayOf(key, screen, language),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT_VALUE))
            } else {
                // Fallback to 'all' screen if specific screen not found
                getAppTextFallback(key, language)
            }
        }
    }

    private fun getAppTextFallback(key: String, language: String): String? {
        val cursor = readableDatabase.query(
            APP_TEXTS_TABLE,
            arrayOf(COLUMN_TEXT_VALUE),
            "$COLUMN_TEXT_KEY = ? AND $COLUMN_TEXT_SCREEN = ? AND $COLUMN_TEXT_LANGUAGE = ?",
            arrayOf(key, "all", language),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT_VALUE))
            } else null
        }
    }

    fun updateAppText(key: String, screen: String, newValue: String, language: String = "en"): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_TEXT_VALUE, newValue)
        }

        val rowsAffected = writableDatabase.update(
            APP_TEXTS_TABLE, values,
            "$COLUMN_TEXT_KEY = ? AND $COLUMN_TEXT_SCREEN = ? AND $COLUMN_TEXT_LANGUAGE = ?",
            arrayOf(key, screen, language)
        )
        return rowsAffected > 0
    }

    fun getAllAppTextsForScreen(screen: String, language: String = "en"): Map<String, String> {
        val texts = mutableMapOf<String, String>()
        val cursor = readableDatabase.query(
            APP_TEXTS_TABLE,
            arrayOf(COLUMN_TEXT_KEY, COLUMN_TEXT_VALUE),
            "$COLUMN_TEXT_SCREEN = ? AND $COLUMN_TEXT_LANGUAGE = ?",
            arrayOf(screen, language),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                texts[it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT_KEY))] =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT_VALUE))
            }
        }
        return texts
    }

    fun getButtonsByCategory(category: String): List<ButtonConfigData> {
        val buttons = mutableListOf<ButtonConfigData>()
        val cursor = readableDatabase.query(
            BUTTON_CONFIGS_TABLE, null,
            "$COLUMN_BTN_CATEGORY = ?", arrayOf(category),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val button = ButtonConfigData(
                    identifier = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_IDENTIFIER)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_TITLE)) ?: "",
                    icon = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_ICON)) ?: "",
                    background = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_BACKGROUND)) ?: "",
                    isEnabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_BTN_IS_ENABLED)) == 1,
                    visibility = it.getInt(it.getColumnIndexOrThrow(COLUMN_BTN_VISIBILITY)),
                    action = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_ACTION)) ?: "",
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_BTN_CATEGORY)) ?: ""
                )
                buttons.add(button)
            }
        }
        return buttons
    }

    // NEW RESOURCE MANAGEMENT METHODS

    fun getIcon(key: String): IconData? {
        val cursor = readableDatabase.query(
            ICONS_TABLE, null,
            "$COLUMN_ICON_KEY = ?", arrayOf(key),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                IconData(
                    key = it.getString(it.getColumnIndexOrThrow(COLUMN_ICON_KEY)),
                    resourceName = it.getString(it.getColumnIndexOrThrow(COLUMN_ICON_RESOURCE_NAME)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_ICON_CATEGORY)) ?: "",
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_ICON_DESCRIPTION)) ?: ""
                )
            } else null
        }
    }

    fun getColor(key: String): ColorData? {
        val cursor = readableDatabase.query(
            COLORS_TABLE, null,
            "$COLUMN_COLOR_KEY = ?", arrayOf(key),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                ColorData(
                    key = it.getString(it.getColumnIndexOrThrow(COLUMN_COLOR_KEY)),
                    value = it.getString(it.getColumnIndexOrThrow(COLUMN_COLOR_VALUE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_COLOR_CATEGORY)) ?: "",
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_COLOR_DESCRIPTION)) ?: ""
                )
            } else null
        }
    }

    fun getDrawable(key: String): DrawableData? {
        val cursor = readableDatabase.query(
            DRAWABLES_TABLE, null,
            "$COLUMN_DRAWABLE_KEY = ?", arrayOf(key),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                DrawableData(
                    key = it.getString(it.getColumnIndexOrThrow(COLUMN_DRAWABLE_KEY)),
                    resourceName = it.getString(it.getColumnIndexOrThrow(COLUMN_DRAWABLE_RESOURCE_NAME)),
                    type = it.getString(it.getColumnIndexOrThrow(COLUMN_DRAWABLE_TYPE)) ?: "",
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_DRAWABLE_CATEGORY)) ?: ""
                )
            } else null
        }
    }

    fun getLayoutElementsForScreen(screen: String): List<LayoutElementData> {
        val elements = mutableListOf<LayoutElementData>()
        val cursor = readableDatabase.query(
            LAYOUT_ELEMENTS_TABLE, null,
            "$COLUMN_LAYOUT_SCREEN = ? AND $COLUMN_LAYOUT_IS_VISIBLE = ?",
            arrayOf(screen, "1"),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val element = LayoutElementData(
                    screen = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_SCREEN)),
                    elementId = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_ELEMENT_ID)),
                    elementType = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_ELEMENT_TYPE)),
                    textKey = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_TEXT_KEY)),
                    iconKey = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_ICON_KEY)),
                    colorKey = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_COLOR_KEY)),
                    drawableKey = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_DRAWABLE_KEY)),
                    isVisible = it.getInt(it.getColumnIndexOrThrow(COLUMN_LAYOUT_IS_VISIBLE)) == 1,
                    properties = it.getString(it.getColumnIndexOrThrow(COLUMN_LAYOUT_PROPERTIES)) ?: ""
                )
                elements.add(element)
            }
        }
        return elements
    }

    fun updateLayoutElementVisibility(screen: String, elementId: String, isVisible: Boolean): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_LAYOUT_IS_VISIBLE, isVisible)
        }

        val rowsAffected = writableDatabase.update(
            LAYOUT_ELEMENTS_TABLE, values,
            "$COLUMN_LAYOUT_SCREEN = ? AND $COLUMN_LAYOUT_ELEMENT_ID = ?",
            arrayOf(screen, elementId)
        )
        return rowsAffected > 0
    }

    // Helper methods for UI color management
    fun getUIColors(): Map<String, String> {
        val colors = mutableMapOf<String, String>()
        val cursor = readableDatabase.query(
            UI_SETTINGS_TABLE,
            arrayOf(COLUMN_UI_KEY, COLUMN_UI_VALUE),
            "$COLUMN_UI_TYPE = ? AND $COLUMN_UI_CATEGORY = ?",
            arrayOf("color", "theme"),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                colors[it.getString(it.getColumnIndexOrThrow(COLUMN_UI_KEY))] =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_UI_VALUE))
            }
        }
        return colors
    }

    fun getUIBooleanSetting(key: String, defaultValue: Boolean = false): Boolean {
        val value = getUISetting(key)
        return value?.toBoolean() ?: defaultValue
    }

    fun getUIIntegerSetting(key: String, defaultValue: Int = 0): Int {
        val value = getUISetting(key)
        return value?.toIntOrNull() ?: defaultValue
    }

    // Method to get formatted text with parameters
    fun getFormattedAppText(key: String, screen: String, vararg params: Any): String {
        val template = getAppText(key, screen) ?: return key
        return try {
            String.format(template, *params)
        } catch (e: Exception) {
            template
        }
    }

    // Method to add new button configuration
    fun addButtonConfig(buttonConfig: ButtonConfigData): Long {
        val values = ContentValues().apply {
            put(COLUMN_BTN_IDENTIFIER, buttonConfig.identifier)
            put(COLUMN_BTN_TITLE, buttonConfig.title)
            put(COLUMN_BTN_ICON, buttonConfig.icon)
            put(COLUMN_BTN_BACKGROUND, buttonConfig.background)
            put(COLUMN_BTN_IS_ENABLED, buttonConfig.isEnabled)
            put(COLUMN_BTN_VISIBILITY, buttonConfig.visibility)
            put(COLUMN_BTN_ACTION, buttonConfig.action)
            put(COLUMN_BTN_CATEGORY, buttonConfig.category)
        }
        return writableDatabase.insert(BUTTON_CONFIGS_TABLE, null, values)
    }

    // Method to add new navigation item
    fun addNavigationItem(navItem: NavigationItemData): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAV_TITLE, navItem.title)
            put(COLUMN_NAV_ICON, navItem.icon)
            put(COLUMN_NAV_POSITION, navItem.position)
            put(COLUMN_NAV_IS_ACTIVE, navItem.isActive)
            put(COLUMN_NAV_ACTION_TYPE, navItem.actionType)
            put(COLUMN_NAV_TARGET, navItem.target)
        }
        return writableDatabase.insert(NAV_ITEMS_TABLE, null, values)
    }

    // Method to toggle navigation item active state
    fun toggleNavigationItem(title: String, isActive: Boolean): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_NAV_IS_ACTIVE, isActive)
        }

        val rowsAffected = writableDatabase.update(
            NAV_ITEMS_TABLE, values,
            "$COLUMN_NAV_TITLE = ?", arrayOf(title)
        )
        return rowsAffected > 0
    }

    // Method to get detection display settings
    fun getDetectionDisplaySettings(): Map<String, Boolean> {
        return mapOf(
            "inference_time_visible" to getUIBooleanSetting("inference_time_visible"),
            "detection_count_visible" to getUIBooleanSetting("detection_count_visible"),
            "camera_preview_enabled" to getUIBooleanSetting("camera_preview_enabled"),
            "detection_overlay_enabled" to getUIBooleanSetting("detection_overlay_enabled")
        )
    }

    // Method to get button sizes
    fun getButtonSizes(): Map<String, Int> {
        return mapOf(
            "scan_button_size" to getUIIntegerSetting("scan_button_size", 86),
            "corner_button_size" to getUIIntegerSetting("corner_button_size", 55),
            "help_button_size" to getUIIntegerSetting("help_button_size", 45)
        )
    }

    // Method to add new UI setting
    fun addUISetting(key: String, value: String, type: String, category: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_UI_KEY, key)
            put(COLUMN_UI_VALUE, value)
            put(COLUMN_UI_TYPE, type)
            put(COLUMN_UI_CATEGORY, category)
        }
        return writableDatabase.insert(UI_SETTINGS_TABLE, null, values)
    }

    // Method to add new app text
    fun addAppText(key: String, value: String, screen: String, element: String, language: String = "en"): Long {
        val values = ContentValues().apply {
            put(COLUMN_TEXT_KEY, key)
            put(COLUMN_TEXT_VALUE, value)
            put(COLUMN_TEXT_SCREEN, screen)
            put(COLUMN_TEXT_ELEMENT, element)
            put(COLUMN_TEXT_LANGUAGE, language)
        }
        return writableDatabase.insert(APP_TEXTS_TABLE, null, values)
    }

    // Method to add new icon
    fun addIcon(iconData: IconData): Long {
        val values = ContentValues().apply {
            put(COLUMN_ICON_KEY, iconData.key)
            put(COLUMN_ICON_RESOURCE_NAME, iconData.resourceName)
            put(COLUMN_ICON_CATEGORY, iconData.category)
            put(COLUMN_ICON_DESCRIPTION, iconData.description)
        }
        return writableDatabase.insert(ICONS_TABLE, null, values)
    }

    // Method to add new color
    fun addColor(colorData: ColorData): Long {
        val values = ContentValues().apply {
            put(COLUMN_COLOR_KEY, colorData.key)
            put(COLUMN_COLOR_VALUE, colorData.value)
            put(COLUMN_COLOR_CATEGORY, colorData.category)
            put(COLUMN_COLOR_DESCRIPTION, colorData.description)
        }
        return writableDatabase.insert(COLORS_TABLE, null, values)
    }

    // Method to add new drawable
    fun addDrawable(drawableData: DrawableData): Long {
        val values = ContentValues().apply {
            put(COLUMN_DRAWABLE_KEY, drawableData.key)
            put(COLUMN_DRAWABLE_RESOURCE_NAME, drawableData.resourceName)
            put(COLUMN_DRAWABLE_TYPE, drawableData.type)
            put(COLUMN_DRAWABLE_CATEGORY, drawableData.category)
        }
        return writableDatabase.insert(DRAWABLES_TABLE, null, values)
    }

    // Method to add new layout element
    fun addLayoutElement(layoutElement: LayoutElementData): Long {
        val values = ContentValues().apply {
            put(COLUMN_LAYOUT_SCREEN, layoutElement.screen)
            put(COLUMN_LAYOUT_ELEMENT_ID, layoutElement.elementId)
            put(COLUMN_LAYOUT_ELEMENT_TYPE, layoutElement.elementType)
            put(COLUMN_LAYOUT_TEXT_KEY, layoutElement.textKey)
            put(COLUMN_LAYOUT_ICON_KEY, layoutElement.iconKey)
            put(COLUMN_LAYOUT_COLOR_KEY, layoutElement.colorKey)
            put(COLUMN_LAYOUT_DRAWABLE_KEY, layoutElement.drawableKey)
            put(COLUMN_LAYOUT_IS_VISIBLE, layoutElement.isVisible)
            put(COLUMN_LAYOUT_PROPERTIES, layoutElement.properties)
        }
        return writableDatabase.insert(LAYOUT_ELEMENTS_TABLE, null, values)
    }

    // UTILITY METHODS

    fun getDatabaseStats(): Map<String, Int> {
        return mapOf(
            "total_detections" to getTableCount(DETECTIONS_TABLE),
            "unique_ingredients" to getTableCount(INGREDIENTS_TABLE),
            "total_recipes" to getTableCount(RECIPES_TABLE),
            "scan_sessions" to getTableCount(SCAN_SESSIONS_TABLE),
            "recipe_matches" to getTableCount(RECIPE_MATCHES_TABLE),
            "ui_settings" to getTableCount(UI_SETTINGS_TABLE),
            "navigation_items" to getTableCount(NAV_ITEMS_TABLE),
            "button_configs" to getTableCount(BUTTON_CONFIGS_TABLE),
            "app_texts" to getTableCount(APP_TEXTS_TABLE),
            "icons" to getTableCount(ICONS_TABLE),
            "colors" to getTableCount(COLORS_TABLE),
            "drawables" to getTableCount(DRAWABLES_TABLE),
            "layout_elements" to getTableCount(LAYOUT_ELEMENTS_TABLE)
        )
    }

    private fun getTableCount(tableName: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $tableName", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
}