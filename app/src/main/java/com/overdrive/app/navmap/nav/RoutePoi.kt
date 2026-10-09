package com.overdrive.app.navmap.nav

/**
 * Plain, immutable data model for a point of interest (POI) shown on the
 * RoadSense navigation map — sourced from free OpenStreetMap data via the
 * public Overpass API (see [OverpassPoiClient]).
 *
 * <p>Framework-free (no Android imports) so it can be unit tested on the JVM,
 * matching the rest of [com.overdrive.app.navmap.nav]. Coordinates are WGS-84
 * decimal degrees.
 *
 * @property kind what category of POI this is ([PoiKind.CHARGING] EV charger
 *   or [PoiKind.FUEL] petrol/diesel station)
 * @property name the place name or brand
 * @property lat latitude in decimal degrees (north positive)
 * @property lng longitude in decimal degrees (east positive)
 * @property operator charging network or station operator name (e.g. "Trugo", "ZES", "Ionity")
 * @property powerKw maximum charging power in kilowatts (e.g. 180.0, 300.0)
 * @property socketCount number of physical charging plugs / sockets available
 * @property acPrice price per kWh for AC charging in local currency
 * @property dcPrice price per kWh for DC fast charging in local currency
 * @property chargingType charging category ("DC", "AC", "DC/AC")
 * @property address textual address or district/city description
 */
data class RoutePoi(
    val kind: PoiKind,
    val name: String,
    val lat: Double,
    val lng: Double,
    val operator: String = "",
    val powerKw: Double = 0.0,
    val socketCount: Int = 0,
    val acPrice: Double = 0.0,
    val dcPrice: Double = 0.0,
    val chargingType: String = "",
    val address: String = ""
)

/**
 * The categories of POI this client can query.
 *
 * <p>Each maps 1:1 to an OSM `amenity` tag value:
 * - [CHARGING] -> `amenity=charging_station`
 * - [FUEL] -> `amenity=fuel`
 */
enum class PoiKind {
    /** EV charging station (`amenity=charging_station`). */
    CHARGING,

    /** Fuel / petrol / diesel station (`amenity=fuel`). */
    FUEL
}
