package com.overdrive.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.overdrive.app.R

data class NavigationRailOption(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val categoryRes: Int? = null,
)

/**
 * Stable user-facing navigation keys. Resource and destination ids are not
 * persisted because they can change between builds.
 */
object NavigationRailCatalog {
    const val ASSISTANT = "assistant"
    const val LIVE = "live"
    const val RECORDINGS = "recordings"
    const val PARKING = "parking"
    const val VEHICLE = "vehicle"
    const val SEAT_POSITIONS = "seat_positions"
    const val PROJECTION = "projection"
    const val CHARGING = "charging"
    const val TRIPS = "trips"
    const val ROAD_SENSE = "road_sense"
    const val MAP = "map"
    const val AUTOMATIONS = "automations"
    const val KEY_MAPPING = "key_mapping"
    const val INTEGRATIONS = "integrations"
    const val NETWORK = "network"
    const val DIAGNOSTICS = "diagnostics"

    val customizableOptions: List<NavigationRailOption> = listOf(
        NavigationRailOption(ASSISTANT, R.string.rail_assistant, R.drawable.ic_smart_toy),
        // Kameralar
        NavigationRailOption(LIVE, R.string.rail_live, R.drawable.ic_live, R.string.rail_section_cameras),
        NavigationRailOption(RECORDINGS, R.string.rail_recordings, R.drawable.ic_recording, R.string.rail_section_cameras),
        NavigationRailOption(PARKING, R.string.rail_parking, R.drawable.ic_parking, R.string.rail_section_cameras),
        // Kontroller
        NavigationRailOption(VEHICLE, R.string.rail_vehicle, R.drawable.ic_vehicle_control, R.string.rail_section_controls),
        NavigationRailOption(SEAT_POSITIONS, R.string.rail_seat_positions, R.drawable.ic_seat_positions, R.string.rail_section_controls),
        NavigationRailOption(PROJECTION, R.string.rail_projection, R.drawable.ic_projection, R.string.rail_section_controls),
        NavigationRailOption(CHARGING, R.string.rail_charging, R.drawable.ic_charging, R.string.rail_section_controls),
        // Sürüş
        NavigationRailOption(TRIPS, R.string.rail_trips, R.drawable.ic_trips, R.string.rail_section_driving),
        NavigationRailOption(ROAD_SENSE, R.string.rail_roadsense, R.drawable.ic_roadsense, R.string.rail_section_driving),
        NavigationRailOption(MAP, R.string.rail_hazard_map, R.drawable.ic_roadsense_map, R.string.rail_section_driving),
        // Otomasyon
        NavigationRailOption(AUTOMATIONS, R.string.rail_automations, R.drawable.ic_automations, R.string.rail_section_automation),
        NavigationRailOption(KEY_MAPPING, R.string.rail_key_mapping, R.drawable.ic_key_mapping, R.string.rail_section_automation),
        NavigationRailOption(INTEGRATIONS, R.string.rail_integrations, R.drawable.ic_integrations, R.string.rail_section_automation),
        // Sistem
        NavigationRailOption(NETWORK, R.string.rail_network, R.drawable.ic_hotspot, R.string.rail_section_system),
        NavigationRailOption(DIAGNOSTICS, R.string.rail_diagnostics, R.drawable.ic_diagnostics, R.string.rail_section_system),
    )

    val customizableKeys: Set<String> = customizableOptions.mapTo(linkedSetOf()) { it.key }
}
