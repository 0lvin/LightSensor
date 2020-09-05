package denis.develops.utils.lightsensor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;

public class UnlockReceiver extends BroadcastReceiver {

    private static String EVENTS_NAME = "LightsSensors.receiver";

    public UnlockReceiver() {
    }

    public static double getSunsetTime(boolean sunrise, double longitude, double latitude) {
        Calendar curCalendar = Calendar.getInstance();

        int N = curCalendar.get(Calendar.DAY_OF_YEAR);
        float offsetLocal = (float) (curCalendar.get(Calendar.ZONE_OFFSET) + curCalendar.get(Calendar.DST_OFFSET)) / (3600 * 1000);

        //based on http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
        double zenith = 90.83333333333333;

        //convert the longitude to hour value and calculate an approximate time
        double lngHour = longitude / 15;

        double t;
        if (sunrise) {
            t = N + ((6 - lngHour) / 24);
        } else {
            t = N + ((18 - lngHour) / 24);
        }
        // calculate the Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;

        //calculate the Sun's true longitude
        double L = M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(Math.toRadians(2 * M))) + 282.634;

        if (L > 360) {
            L = L - 360;
        } else if (L < 0) {
            L = L + 360;
        }

        //calculate the Sun's right ascension
        double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))));

        if (RA > 360) {
            RA = RA - 360;
        } else if (RA < 0) {
            RA = RA + 360;
        }

        //right ascension value needs to be in the same quadrant as L
        double Lquadrant = (Math.floor(L / 90)) * 90;
        double RAquadrant = (Math.floor(RA / 90)) * 90;
        RA = RA + (Lquadrant - RAquadrant);

        //right ascension value needs to be converted into hours
        RA = RA / 15;

        //calculate the Sun's declination
        double sinDec = 0.39782 * Math.sin(Math.toRadians(L));
        double cosDec = Math.cos(Math.asin(sinDec));

        //calculate the Sun's local hour angle
        double cosH = (Math.cos(Math.toRadians(zenith)) - (sinDec * Math.sin(Math.toRadians(latitude)))) / (cosDec * Math.cos(Math.toRadians(latitude)));

        if (cosH > 1) {
            // "the sun never rises on this location (on the specified date)"
            return -1;
        }

        if (cosH < -1) {
            //"the sun never sets on this location (on the specified date)"
            return 25;
        }

        double H;
        //finish calculating H and convert into hours
        if (sunrise) {
            H = 360 - Math.toDegrees(Math.acos(cosH));
        } else {
            H = Math.toDegrees(Math.acos(cosH));
        }
        H = H / 15;

        //calculate local mean time of rising/setting
        double T = H + RA - (0.06571 * t) - 6.622;

        //adjust back to UTC

        double UT = T - lngHour;

        // convert UT value to local time zone of latitude/longitude
        UT = UT + offsetLocal;
        if (UT < 0) {
            UT = UT + 24;
        } else if (UT > 24) {
            UT = UT - 24;
        }

        return UT;
    }

    public void registerReceivers(Context context) {
        try {
            Log.i(EVENTS_NAME, "Register unlock receiver.");
            final IntentFilter theFilter = new IntentFilter();
            /* System Defined Broadcast */
            theFilter.addAction(Intent.ACTION_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                theFilter.addAction(Intent.ACTION_USER_PRESENT);
            }

            context.registerReceiver(this, theFilter);

        } catch (Exception e) {
            Log.e(EVENTS_NAME, "Cannot register unlock receiver:" + e.toString());
        }

        try {
            // enable service on system level
            ComponentName receiver = new ComponentName(context, UnlockReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.e(EVENTS_NAME, "Cannot register package:" + e.toString());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            try {
                AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, UnlockReceiver.class);
                PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
                alarmMgr.setInexactRepeating(AlarmManager.RTC, 0, AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
                Log.e(EVENTS_NAME, "Registered alarm receiver");
            } catch (Exception e) {
                Log.e(EVENTS_NAME, "Cannot register alarm receiver:" + e.toString());
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // init alarms and broadcast
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            this.registerReceivers(context);
        }

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean auto_change = prefs.getBoolean(MainActivity.AUTO_VALUE, false);
        boolean sun_change = prefs.getBoolean(MainActivity.AUTO_SUN_VALUE, false);
        if (!auto_change && !sun_change) {
            Log.i(EVENTS_NAME, "Service disabled.");
            return;
        }

        Log.i(EVENTS_NAME, "Received intent: " + ((action != null) ? action : "unknown"));

        // check battery status
        if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(MainActivity.BATTERY_LOW, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                edit.apply();
            }
        } else if (Intent.ACTION_BATTERY_OKAY.equals(action) ||
                Intent.ACTION_POWER_CONNECTED.equals(action)) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(MainActivity.BATTERY_LOW, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                edit.apply();
            }
        }

        double summ_value = this.getLightByCalendar(prefs);
        boolean smooth_change = !(
                Intent.ACTION_SCREEN_ON.equals(action) ||
                        Intent.ACTION_USER_PRESENT.equals(action) ||
                        Intent.ACTION_USER_UNLOCKED.equals(action));
        this.setBrightness(context, prefs, summ_value, smooth_change);
    }

    private void setBrightness(Context context, SharedPreferences prefs, double summ_value, boolean smooth) {
        float minPercentValue = prefs.getInt(MainActivity.MIN_PERCENT_VALUE, 0);
        float maxPercentValue = prefs.getInt(MainActivity.MAX_PERCENT_VALUE, 100);

        if (prefs.getBoolean(MainActivity.BATTERY_LOW, false)) {
            Log.i(EVENTS_NAME, "Battery low.");
            // limit battery
            float maxBatteryPercentValue = prefs.getInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, 100);
            if (maxBatteryPercentValue < maxPercentValue) {
                maxPercentValue = maxBatteryPercentValue;
            }
        }

        try {
            double value = Math.min(summ_value + (minPercentValue * 256 / 100), (maxPercentValue * 256 / 100));

            if (smooth) {
                Log.i(EVENTS_NAME, "Set smooth brightness to " + value);
                value = (value + (double) Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS)) / 2;
            }

            if (value > 255) {
                value = 255;
            }

            Log.i(EVENTS_NAME, "Set brightness to " + value);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) value);
        } catch (Exception e) {
            //Throw an error case it couldn't be retrieved
            Log.e(EVENTS_NAME, "Cannot access system brightness:" + e.toString());
        }
    }

    private double getLightByCalendar(SharedPreferences prefs) {
        boolean auto_change = prefs.getBoolean(MainActivity.AUTO_VALUE, false);
        boolean sun_change = prefs.getBoolean(MainActivity.AUTO_SUN_VALUE, false);

        Calendar curCalendar = Calendar.getInstance();
        double auto_value = 255;
        double sun_value = 255;
        double curr_time_hour = curCalendar.get(Calendar.HOUR_OF_DAY) + (float) curCalendar.get(Calendar.MINUTE) / 60;
        if (auto_change) {
            double minDegree = curr_time_hour * 360 / 24 + 270;
            // 0 hour -> sin(270) -> -1
            auto_value = Math.sin(Math.toRadians(minDegree)) * 128 + 128;
            Log.i(EVENTS_NAME, "Auto value:" + auto_value);
        }

        if (sun_change) {
            double locationLongitude = prefs.getFloat(MainActivity.LONGITUDE_VALUE, 0);
            double locationLatitude = prefs.getFloat(MainActivity.LATITUDE_VALUE, 0);

            double sunrise = getSunsetTime(true, locationLongitude, locationLatitude);
            double sunset = getSunsetTime(false, locationLongitude, locationLatitude);
            if (sunrise < 0 || sunset > 24) {
                // nothing to do with it
                sun_change = false;
            } else {
                if (curr_time_hour > sunset || curr_time_hour < sunrise) {
                    sun_value = 0;
                } else {
                    if ((sunset - sunrise) > 0) {
                        sun_value = Math.sin(Math.toRadians((curr_time_hour - sunrise) / (sunset - sunrise) * 180)) * 256;
                    } else {
                        sun_change = false;
                    }
                }
                Log.i(EVENTS_NAME, "Sun value:" + sun_value);
            }
        }
        double summ_value = 0;
        if (sun_change && auto_change) {
            summ_value = (sun_value + auto_value) / 2;
        } else if (sun_change) {
            summ_value = sun_value;
        } else if (auto_change) {
            summ_value = auto_value;
        }
        return summ_value;
    }
}
