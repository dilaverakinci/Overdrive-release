package com.overdrive.app.ui.vehicle

import android.content.Context
import androidx.annotation.DrawableRes
import com.overdrive.app.R
import com.overdrive.app.config.UnifiedConfigManager
import java.util.Locale

/**
 * Maps a selected vehicle model ID to its top-down plan-view visual.
 * Used for live camera hotspot navigation, overhead vehicle status, and overhead alignment.
 */
object VehicleTopDownArt {

    /**
     * Returns the appropriate top-down overhead visual drawable for [modelId].
     * Falls back to the standard BYD Seal top-down visual if unknown.
     */
    @DrawableRes
    fun drawableFor(modelId: String?): Int = when (normalize(modelId)) {
        "seal" -> R.drawable.vehicle_topdown_default
        "sealion7", "sealu", "sealudmi", "sealion6", "atto3", "atto3evo", "atto2", "atto1", "tang", "song", "qin" -> R.drawable.vehicle_topdown_suv
        "dolphin", "seagull" -> R.drawable.vehicle_topdown_hatchback
        "han", "destroyer", "destroyer05" -> R.drawable.vehicle_topdown_sedan
        "m6" -> R.drawable.vehicle_topdown_mpv
        "shark" -> R.drawable.vehicle_topdown_pickup
        else -> R.drawable.vehicle_topdown_default
    }

    /**
     * Returns a human-friendly display name for [modelId].
     */
    fun displayNameFor(modelId: String?, context: Context? = null): String = when (normalize(modelId)) {
        "seal" -> "BYD Seal"
        "sealion7" -> "BYD Sealion 7"
        "sealion6" -> "BYD Sealion 6"
        "sealu" -> "BYD Seal U"
        "sealudmi" -> "BYD Seal U DM-i"
        "dolphin" -> "BYD Dolphin"
        "atto3" -> "BYD Atto 3"
        "atto3evo" -> "BYD Atto 3 Evo"
        "atto2" -> "BYD Atto 2"
        "atto1" -> "BYD Atto 1"
        "han" -> "BYD Han"
        "tang" -> "BYD Tang"
        "song" -> "BYD Song"
        "qin" -> "BYD Qin"
        "m6" -> "BYD M6"
        "seagull" -> context?.getString(R.string.vehicle_model_seagull) ?: "BYD Seagull"
        "destroyer", "destroyer05" -> "BYD Destroyer 05"
        "shark" -> "BYD Shark"
        else -> modelId?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() } ?: "BYD Seal"
    }

    /**
     * Resolves the currently selected model ID from persistent preferences or unified config.
     */
    fun getSelectedModelId(context: Context?): String? {
        try {
            val prefs = context?.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
            val pId = prefs?.getString("selected_model_id", null)
            if (!pId.isNullOrEmpty()) return pId
        } catch (_: Throwable) {}
        try {
            val v = UnifiedConfigManager.getVehicle()
            val m = v.optString("modelId", "")
            val src = v.optString("modelSource", "")
            if (m.isNotEmpty() && src != "unset") return m
        } catch (_: Throwable) {}
        return null
    }

    /**
     * Returns the human-friendly display name for the currently selected vehicle model.
     */
    fun getSelectedModelDisplayName(context: Context?): String {
        val modelId = getSelectedModelId(context)
        return if (!modelId.isNullOrEmpty()) {
            displayNameFor(modelId, context)
        } else {
            "BYD Seal"
        }
    }

    private fun normalize(modelId: String?): String =
        modelId?.lowercase(Locale.US)?.filter(Char::isLetterOrDigit) ?: ""
}
