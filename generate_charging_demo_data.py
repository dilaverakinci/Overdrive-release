import json
import time
from datetime import datetime, timezone, timedelta

# Reference time: now (in ms)
now_ms = int(time.time() * 1000)

# Calculate timestamps
# Session 1: AC Home Charge (2 days ago at 22:30 to next morning 04:30)
# 48 hours ago + 2 hours = 46 hours ago
ac_start_ms = now_ms - (46 * 3600 * 1000)
ac_end_ms = ac_start_ms + (6 * 3600 * 1000) # 6 hours duration

# Session 2: DC Fast Charge (Yesterday at 14:15 to 14:55)
# 22 hours ago
dc_start_ms = now_ms - (22 * 3600 * 1000)
dc_end_ms = dc_start_ms + (42 * 60 * 1000) # 42 minutes duration

# Generate SQL script
sql_lines = []
sql_lines.append("-- OverDrive Demo Charging Data: AC Home Charge & DC Fast Station Charge")
sql_lines.append("-- Generated on " + datetime.now().isoformat())
sql_lines.append("")

# Delete existing demo data if any, or clear
sql_lines.append("-- 1. Insert AC Charging Session (Home Wallbox 11 kW)")
sql_lines.append(f"""INSERT INTO charging_sessions (
    start_time, end_time, start_soc, end_soc, energy_added_kwh,
    peak_power_kw, avg_power_kw, range_gained_km, gun_state, is_dc,
    electricity_rate, currency, session_cost, time_to_full_min,
    hv_temp_high, hv_temp_low, hv_temp_avg,
    start_lat, start_lng, place_label, start_range_km, start_odometer_km,
    tariff_id, tariff_label, energy_source
) VALUES (
    {ac_start_ms}, {ac_end_ms}, 22.0, 80.0, 48.20,
    11.2, 8.03, 310, 2, 0,
    9.90, '₺', 477.18, 0,
    26.5, 23.0, 24.8,
    37.97837, 40.15138, 'Ev (Home)', 115, 12450,
    'home_tariff', 'Ev Tarifesi', 'metered'
);""")

sql_lines.append("")
sql_lines.append("-- 2. Insert DC Fast Charging Session (120 kW Fast Charger)")
sql_lines.append(f"""INSERT INTO charging_sessions (
    start_time, end_time, start_soc, end_soc, energy_added_kwh,
    peak_power_kw, avg_power_kw, range_gained_km, gun_state, is_dc,
    electricity_rate, currency, session_cost, time_to_full_min,
    hv_temp_high, hv_temp_low, hv_temp_avg,
    start_lat, start_lng, place_label, start_range_km, start_odometer_km,
    tariff_id, tariff_label, energy_source
) VALUES (
    {dc_start_ms}, {dc_end_ms}, 14.0, 82.0, 56.50,
    128.5, 80.71, 360, 3, 1,
    13.50, '₺', 762.75, 0,
    38.5, 32.0, 35.2,
    38.35412, 39.51234, 'Trugo Hızlı Şarj İstasyonu', 75, 12780,
    'dc_fast_tariff', 'DC Hızlı Şarj', 'metered'
);""")

sql_lines.append("")
sql_lines.append("-- 3. Insert Power & Temperature Ramp Samples for AC Session")
# AC samples: 6 hours = 360 mins. Let's do samples every 10 minutes (36 points)
ac_samples = []
for i in range(37):
    t = ac_start_ms + i * (10 * 60 * 1000)
    frac = i / 36.0
    soc = round(22.0 + frac * 58.0, 1)
    if frac < 0.85:
        pwr = round(10.8 + (i % 3) * 0.15, 2)
    else:
        # taper towards end
        pwr = round(10.8 - (frac - 0.85) * 20.0, 2)
        if pwr < 3.0: pwr = 3.2
    temp_avg = round(23.0 + frac * 2.2, 1)
    temp_high = round(temp_avg + 1.2, 1)
    temp_low = round(temp_avg - 1.1, 1)
    ac_samples.append((t, pwr, soc, temp_avg, temp_high, temp_low))
    sql_lines.append(f"INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES ({ac_start_ms}, {t}, {pwr}, {soc}, {temp_avg}, {temp_high}, {temp_low});")

sql_lines.append("")
sql_lines.append("-- 4. Insert Power & Temperature Ramp Samples for DC Session")
# DC samples: 42 mins. Let's do samples every 1 minute (42 points)
# Typical DC curve: ramp up quickly from 40 to 128 kW, stay ~120 kW until 60% soc, then step down to 90, 60, 35 kW.
dc_samples = []
for i in range(43):
    t = dc_start_ms + i * (60 * 1000)
    minute = i
    if minute == 0:
        pwr = 45.0
        soc = 14.0
    elif minute <= 3:
        pwr = round(80.0 + minute * 15.0, 1) # ramp to ~125 kW
        soc = round(14.0 + minute * 1.5, 1)
    elif minute <= 18:
        # Peak zone (14% -> ~50% SoC)
        pwr = round(124.0 + (minute % 4) * 1.2, 1)
        if minute == 8: pwr = 128.5 # exact peak
        soc = round(18.5 + (minute - 3) * 2.2, 1)
    elif minute <= 28:
        # Step down to 90 kW (~50% -> ~68% SoC)
        pwr = round(92.0 - (minute - 18) * 0.8, 1)
        soc = round(51.5 + (minute - 18) * 1.6, 1)
    elif minute <= 36:
        # Step down to 55 kW (~68% -> ~78% SoC)
        pwr = round(60.0 - (minute - 28) * 1.2, 1)
        soc = round(67.5 + (minute - 28) * 1.1, 1)
    else:
        # Final taper to 35 kW (~78% -> 82% SoC)
        pwr = round(42.0 - (minute - 36) * 1.2, 1)
        soc = round(76.3 + (minute - 36) * 0.9, 1)
    
    # Temperature warms up during DC fast charge (25°C -> 38.5°C)
    temp_avg = round(25.0 + min(minute * 0.32, 10.2), 1)
    temp_high = round(temp_avg + 3.2, 1)
    temp_low = round(temp_avg - 2.8, 1)
    dc_samples.append((t, pwr, soc, temp_avg, temp_high, temp_low))
    sql_lines.append(f"INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES ({dc_start_ms}, {t}, {pwr}, {soc}, {temp_avg}, {temp_high}, {temp_low});")

sql_lines.append("")
sql_lines.append("-- 5. Insert Daily Rollups (charging_daily & soc_daily)")
# UTC midnight buckets:
ac_day = (ac_start_ms // 86400000) * 86400000
dc_day = (dc_start_ms // 86400000) * 86400000

sql_lines.append(f"""MERGE INTO charging_daily (day_epoch, session_count, energy_kwh, cost, dc_count, ac_count, peak_power_kw, soh_at_day, range_gained_km, incomplete_count)
KEY(day_epoch) VALUES ({ac_day}, 1, 48.20, 477.18, 0, 1, 11.2, 100.0, 310, 0);""")

sql_lines.append(f"""MERGE INTO charging_daily (day_epoch, session_count, energy_kwh, cost, dc_count, ac_count, peak_power_kw, soh_at_day, range_gained_km, incomplete_count)
KEY(day_epoch) VALUES ({dc_day}, 1, 56.50, 762.75, 1, 0, 128.5, 100.0, 360, 0);""")

sql_content = "\n".join(sql_lines)

with open("seed_charging_data.sql", "w", encoding="utf-8") as f:
    f.write(sql_content)

print(f"Generated seed_charging_data.sql ({len(sql_lines)} lines)")

# Also create JSON format for reference/API testing
json_payload = {
    "sessions": [
        {
            "id": 1,
            "startTime": dc_start_ms,
            "endTime": dc_end_ms,
            "inProgress": False,
            "chargingNow": False,
            "startSoc": 14.0,
            "endSoc": 82.0,
            "energyAdded": 56.5,
            "peakPower": 128.5,
            "avgPower": 80.71,
            "rangeGained": 360,
            "gunState": 3,
            "isDc": True,
            "electricityRate": 13.50,
            "cost": 762.75,
            "currency": "₺",
            "timeToFullMin": 0,
            "tempHigh": 38.5,
            "tempLow": 32.0,
            "tempAvg": 35.2,
            "durationMinutes": 42,
            "lat": 38.35412,
            "lng": 39.51234,
            "placeLabel": "Trugo Hızlı Şarj İstasyonu",
            "startOdometerKm": 12780,
            "startRangeKm": 75,
            "tariffId": "dc_fast_tariff",
            "tariffLabel": "DC Hızlı Şarj",
            "isEstimated": False
        },
        {
            "id": 2,
            "startTime": ac_start_ms,
            "endTime": ac_end_ms,
            "inProgress": False,
            "chargingNow": False,
            "startSoc": 22.0,
            "endSoc": 80.0,
            "energyAdded": 48.2,
            "peakPower": 11.2,
            "avgPower": 8.03,
            "rangeGained": 310,
            "gunState": 2,
            "isDc": False,
            "electricityRate": 9.90,
            "cost": 477.18,
            "currency": "₺",
            "timeToFullMin": 0,
            "tempHigh": 26.5,
            "tempLow": 23.0,
            "tempAvg": 24.8,
            "durationMinutes": 360,
            "lat": 37.97837,
            "lng": 40.15138,
            "placeLabel": "Ev (Home)",
            "startOdometerKm": 12450,
            "startRangeKm": 115,
            "tariffId": "home_tariff",
            "tariffLabel": "Ev Tarifesi",
            "isEstimated": False
        }
    ],
    "samples_ac": [
        {"t": s[0], "powerKw": s[1], "soc": s[2], "temp": s[3], "tempHigh": s[4], "tempLow": s[5]}
        for s in ac_samples
    ],
    "samples_dc": [
        {"t": s[0], "powerKw": s[1], "soc": s[2], "temp": s[3], "tempHigh": s[4], "tempLow": s[5]}
        for s in dc_samples
    ]
}

with open("seed_charging_data.json", "w", encoding="utf-8") as f:
    json.dump(json_payload, f, indent=2, ensure_ascii=False)

print("Generated seed_charging_data.json successfully")
