package com.overdrive.app.ui.seatpositions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.R

class AmbientColourAdapter(
    private val context: Context,
    private val colours: List<String>,
    private var selectedIndex: Int, // 1-based index
    private val onColourSelected: (Int) -> Unit
) : RecyclerView.Adapter<AmbientColourAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewSwatch: View = view.findViewById(R.id.viewSwatch)
        val ivCheck: ImageView = view.findViewById(R.id.ivSwatchCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_colour_swatch, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = colours.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val colourIndex = position + 1 // 1-based
        val hex = colours[position]
        try {
            val color = Color.parseColor(hex)
            holder.viewSwatch.backgroundTintList = ColorStateList.valueOf(color)
        } catch (_: Exception) {}

        val isSelected = (colourIndex == selectedIndex)
        holder.ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val oldPos = selectedIndex - 1
            selectedIndex = colourIndex
            if (oldPos in colours.indices) notifyItemChanged(oldPos)
            notifyItemChanged(position)
            onColourSelected(colourIndex)
        }
    }
}
