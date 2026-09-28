package com.overdrive.app.ui.vehicle

import androidx.annotation.DrawableRes
import com.overdrive.app.R
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
        "sealion7", "sealu", "sealudmi", "atto3", "atto3evo", "atto2", "tang" -> R.drawable.vehicle_topdown_suv
        "dolphin", "seagull" -> R.drawable.vehicle_topdown_hatchback
        "han", "destroyer", "destroyer05" -> R.drawable.vehicle_topdown_sedan
        "m6" -> R.drawable.vehicle_topdown_mpv
        "shark" -> R.drawable.vehicle_topdown_pickup
        else -> R.drawable.vehicle_topdown_default
    }

    /**
     * Returns a human-friendly display name for [modelId].
     */
    fun displayNameFor(modelId: String?): String = when (normalize(modelId)) {
        "seal" -> "BYD Seal"
        "sealion7" -> "BYD Sealion 7"
        "sealu", "sealudmi" -> "BYD Seal U"
        "dolphin" -> "BYD Dolphin"
        "atto3" -> "BYD Atto 3"
        "atto3evo" -> "BYD Atto 3 Evo"
        "atto2" -> "BYD Atto 2"
        "han" -> "BYD Han"
        "tang" -> "BYD Tang"
        "m6" -> "BYD M6"
        "seagull" -> "BYD Seagull"
        "destroyer", "destroyer05" -> "BYD Destroyer 05"
        "shark" -> "BYD Shark"
        else -> modelId?.replaceFirstChar { it.uppercase() } ?: "BYD Seal"
    }

    private fun normalize(modelId: String?): String =
        modelId?.lowercase(Locale.US)?.filter(Char::isLetterOrDigit) ?: ""
}
