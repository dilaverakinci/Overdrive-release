-- OverDrive Demo Charging Data: AC Home Charge & DC Fast Station Charge
-- Generated on 2026-10-09T20:39:21.405239

-- 1. Insert AC Charging Session (Home Wallbox 11 kW)
INSERT INTO charging_sessions (
    start_time, end_time, start_soc, end_soc, energy_added_kwh,
    peak_power_kw, avg_power_kw, range_gained_km, gun_state, is_dc,
    electricity_rate, currency, session_cost, time_to_full_min,
    hv_temp_high, hv_temp_low, hv_temp_avg,
    start_lat, start_lng, place_label, start_range_km, start_odometer_km,
    tariff_id, tariff_label, energy_source
) VALUES (
    1791401961405, 1791423561405, 22.0, 80.0, 48.20,
    11.2, 8.03, 310, 2, 0,
    9.90, '₺', 477.18, 0,
    26.5, 23.0, 24.8,
    37.97837, 40.15138, 'Ev (Home)', 115, 12450,
    'home_tariff', 'Ev Tarifesi', 'metered'
);

-- 2. Insert DC Fast Charging Session (120 kW Fast Charger)
INSERT INTO charging_sessions (
    start_time, end_time, start_soc, end_soc, energy_added_kwh,
    peak_power_kw, avg_power_kw, range_gained_km, gun_state, is_dc,
    electricity_rate, currency, session_cost, time_to_full_min,
    hv_temp_high, hv_temp_low, hv_temp_avg,
    start_lat, start_lng, place_label, start_range_km, start_odometer_km,
    tariff_id, tariff_label, energy_source
) VALUES (
    1791488361405, 1791490881405, 14.0, 82.0, 56.50,
    128.5, 80.71, 360, 3, 1,
    13.50, '₺', 762.75, 0,
    38.5, 32.0, 35.2,
    38.35412, 39.51234, 'Trugo Hızlı Şarj İstasyonu', 75, 12780,
    'dc_fast_tariff', 'DC Hızlı Şarj', 'metered'
);

-- 3. Insert Power & Temperature Ramp Samples for AC Session
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791401961405, 10.8, 22.0, 23.0, 24.2, 21.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791402561405, 10.95, 23.6, 23.1, 24.3, 22.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791403161405, 11.1, 25.2, 23.1, 24.3, 22.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791403761405, 10.8, 26.8, 23.2, 24.4, 22.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791404361405, 10.95, 28.4, 23.2, 24.4, 22.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791404961405, 11.1, 30.1, 23.3, 24.5, 22.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791405561405, 10.8, 31.7, 23.4, 24.6, 22.3);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791406161405, 10.95, 33.3, 23.4, 24.6, 22.3);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791406761405, 11.1, 34.9, 23.5, 24.7, 22.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791407361405, 10.8, 36.5, 23.6, 24.8, 22.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791407961405, 10.95, 38.1, 23.6, 24.8, 22.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791408561405, 11.1, 39.7, 23.7, 24.9, 22.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791409161405, 10.8, 41.3, 23.7, 24.9, 22.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791409761405, 10.95, 42.9, 23.8, 25.0, 22.7);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791410361405, 11.1, 44.6, 23.9, 25.1, 22.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791410961405, 10.8, 46.2, 23.9, 25.1, 22.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791411561405, 10.95, 47.8, 24.0, 25.2, 22.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791412161405, 11.1, 49.4, 24.0, 25.2, 22.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791412761405, 10.8, 51.0, 24.1, 25.3, 23.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791413361405, 10.95, 52.6, 24.2, 25.4, 23.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791413961405, 11.1, 54.2, 24.2, 25.4, 23.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791414561405, 10.8, 55.8, 24.3, 25.5, 23.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791415161405, 10.95, 57.4, 24.3, 25.5, 23.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791415761405, 11.1, 59.1, 24.4, 25.6, 23.3);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791416361405, 10.8, 60.7, 24.5, 25.7, 23.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791416961405, 10.95, 62.3, 24.5, 25.7, 23.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791417561405, 11.1, 63.9, 24.6, 25.8, 23.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791418161405, 10.8, 65.5, 24.6, 25.8, 23.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791418761405, 10.95, 67.1, 24.7, 25.9, 23.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791419361405, 11.1, 68.7, 24.8, 26.0, 23.7);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791419961405, 10.8, 70.3, 24.8, 26.0, 23.7);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791420561405, 10.58, 71.9, 24.9, 26.1, 23.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791421161405, 10.02, 73.6, 25.0, 26.2, 23.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791421761405, 9.47, 75.2, 25.0, 26.2, 23.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791422361405, 8.91, 76.8, 25.1, 26.3, 24.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791422961405, 8.36, 78.4, 25.1, 26.3, 24.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791401961405, 1791423561405, 7.8, 80.0, 25.2, 26.4, 24.1);

-- 4. Insert Power & Temperature Ramp Samples for DC Session
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488361405, 45.0, 14.0, 25.0, 28.2, 22.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488421405, 95.0, 15.5, 25.3, 28.5, 22.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488481405, 110.0, 17.0, 25.6, 28.8, 22.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488541405, 125.0, 18.5, 26.0, 29.2, 23.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488601405, 124.0, 20.7, 26.3, 29.5, 23.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488661405, 125.2, 22.9, 26.6, 29.8, 23.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488721405, 126.4, 25.1, 26.9, 30.1, 24.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488781405, 127.6, 27.3, 27.2, 30.4, 24.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488841405, 128.5, 29.5, 27.6, 30.8, 24.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488901405, 125.2, 31.7, 27.9, 31.1, 25.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791488961405, 126.4, 33.9, 28.2, 31.4, 25.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489021405, 127.6, 36.1, 28.5, 31.7, 25.7);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489081405, 124.0, 38.3, 28.8, 32.0, 26.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489141405, 125.2, 40.5, 29.2, 32.4, 26.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489201405, 126.4, 42.7, 29.5, 32.7, 26.7);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489261405, 127.6, 44.9, 29.8, 33.0, 27.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489321405, 124.0, 47.1, 30.1, 33.3, 27.3);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489381405, 125.2, 49.3, 30.4, 33.6, 27.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489441405, 126.4, 51.5, 30.8, 34.0, 28.0);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489501405, 91.2, 53.1, 31.1, 34.3, 28.3);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489561405, 90.4, 54.7, 31.4, 34.6, 28.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489621405, 89.6, 56.3, 31.7, 34.9, 28.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489681405, 88.8, 57.9, 32.0, 35.2, 29.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489741405, 88.0, 59.5, 32.4, 35.6, 29.6);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489801405, 87.2, 61.1, 32.7, 35.9, 29.9);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489861405, 86.4, 62.7, 33.0, 36.2, 30.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489921405, 85.6, 64.3, 33.3, 36.5, 30.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791489981405, 84.8, 65.9, 33.6, 36.8, 30.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490041405, 84.0, 67.5, 34.0, 37.2, 31.2);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490101405, 58.8, 68.6, 34.3, 37.5, 31.5);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490161405, 57.6, 69.7, 34.6, 37.8, 31.8);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490221405, 56.4, 70.8, 34.9, 38.1, 32.1);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490281405, 55.2, 71.9, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490341405, 54.0, 73.0, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490401405, 52.8, 74.1, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490461405, 51.6, 75.2, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490521405, 50.4, 76.3, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490581405, 40.8, 77.2, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490641405, 39.6, 78.1, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490701405, 38.4, 79.0, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490761405, 37.2, 79.9, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490821405, 36.0, 80.8, 35.2, 38.4, 32.4);
INSERT INTO charging_power_samples (session_start_time, t, power_kw, soc, temp, temp_high, temp_low) VALUES (1791488361405, 1791490881405, 34.8, 81.7, 35.2, 38.4, 32.4);

-- 5. Insert Daily Rollups (charging_daily & soc_daily)
MERGE INTO charging_daily (day_epoch, session_count, energy_kwh, cost, dc_count, ac_count, peak_power_kw, soh_at_day, range_gained_km, incomplete_count)
KEY(day_epoch) VALUES (1791331200000, 1, 48.20, 477.18, 0, 1, 11.2, 100.0, 310, 0);
MERGE INTO charging_daily (day_epoch, session_count, energy_kwh, cost, dc_count, ac_count, peak_power_kw, soh_at_day, range_gained_km, incomplete_count)
KEY(day_epoch) VALUES (1791417600000, 1, 56.50, 762.75, 1, 0, 128.5, 100.0, 360, 0);