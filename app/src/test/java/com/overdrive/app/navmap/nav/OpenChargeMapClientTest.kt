package com.overdrive.app.navmap.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenChargeMapClientTest {

    @Test
    fun testParseOcmValid() {
        val json = """
        [
          {
            "AddressInfo": {
              "Title": "Ionity Station A8",
              "AddressLine1": "Autobahn A8 km 45",
              "Town": "Ulm",
              "Latitude": 48.456,
              "Longitude": 9.987
            },
            "OperatorInfo": {
              "Title": "Ionity"
            },
            "Connections": [
              {
                "PowerKW": 350.0,
                "Quantity": 4,
                "ConnectionType": { "Title": "CCS (Type 2)" },
                "Level": { "IsFastCharge": true }
              },
              {
                "PowerKW": 43.0,
                "Quantity": 2,
                "ConnectionType": { "Title": "Type 2" },
                "Level": { "IsFastCharge": false }
              }
            ]
          }
        ]
        """.trimIndent()

        val pois = OpenChargeMapClient.parseOcm(json)
        assertEquals(1, pois.size)

        val poi = pois[0]
        assertEquals(PoiKind.CHARGING, poi.kind)
        assertEquals("Ionity Station A8", poi.name)
        assertEquals("Ionity", poi.operator)
        assertEquals(48.456, poi.lat, 1e-4)
        assertEquals(9.987, poi.lng, 1e-4)
        assertEquals(350.0, poi.powerKw, 0.1)
        assertEquals(6, poi.socketCount)
        assertEquals("DC/AC", poi.chargingType)
        assertEquals("Autobahn A8 km 45, Ulm", poi.address)
    }

    @Test
    fun testParseOcmMalformedOrEmpty() {
        val emptyPois = OpenChargeMapClient.parseOcm("[]")
        assertTrue(emptyPois.isEmpty())

        val invalidPois = OpenChargeMapClient.parseOcm("not json")
        assertTrue(invalidPois.isEmpty())
    }

    @Test
    fun testParseOverpassWithEvTags() {
        val json = """
        {
          "elements": [
            {
              "type": "node",
              "id": 12345,
              "lat": 41.015,
              "lon": 28.978,
              "tags": {
                "amenity": "charging_station",
                "name": "ZES Zorlu Center",
                "operator": "ZES",
                "capacity": "4",
                "socket:type2_combo:output": "180kW",
                "addr:street": "Levazım Mah.",
                "addr:city": "İstanbul"
              }
            },
            {
              "type": "node",
              "id": 67890,
              "lat": 41.020,
              "lon": 28.980,
              "tags": {
                "amenity": "fuel",
                "name": "Shell Beşiktaş",
                "brand": "Shell"
              }
            }
          ]
        }
        """.trimIndent()

        val pois = OverpassPoiClient.parseOverpass(json)
        assertEquals(2, pois.size)

        val charging = pois[0]
        assertEquals(PoiKind.CHARGING, charging.kind)
        assertEquals("ZES Zorlu Center", charging.name)
        assertEquals("ZES", charging.operator)
        assertEquals(4, charging.socketCount)
        assertEquals(180.0, charging.powerKw, 0.1)
        assertEquals("DC", charging.chargingType)
        assertEquals("Levazım Mah., İstanbul", charging.address)

        val fuel = pois[1]
        assertEquals(PoiKind.FUEL, fuel.kind)
        assertEquals("Shell Beşiktaş", fuel.name)
        assertEquals("Shell", fuel.operator)
    }
}
