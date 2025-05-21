package com.example.object_detection_dishcovery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying ingredients in a dialog
 */
class IngredientAdapter(private val ingredients: List<IngredientData>) :
    RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientNameText: TextView = itemView.findViewById(R.id.ingredientNameText)
        val ingredientCheckmark: ImageView = itemView.findViewById(R.id.ingredientEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun getItemCount(): Int = ingredients.size

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]

        holder.ingredientNameText.text = ingredient.name
        // All ingredients are checked by default
        holder.ingredientCheckmark.visibility = View.VISIBLE
    }
}

/**
 * Data class for storing ingredient information
 */
data class IngredientData(
    val name: String,
    val detectionData: DetectionData
)