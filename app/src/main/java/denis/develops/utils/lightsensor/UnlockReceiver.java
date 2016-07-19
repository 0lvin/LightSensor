package denis.develops.utils.lightsensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class UnlockReceiver extends BroadcastReceiver {
    final String EVENTS_NAME = "LightsSensors.receiver";

    public UnlockReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(MainActivity.AUTO_VALUE, false)) {
            Log.i(EVENTS_NAME, "Service disabled.");
            return;
        }
        try {
            Calendar curCalendar = Calendar.getInstance();
            Date curDate = curCalendar.getTime();
            int minutes = curDate.getHours() * 60 + curDate.getMinutes();
            double minDegree = minutes * 360 / 24 / 60 + 270;
            // 0 hour -> sin(270) -> -1
            double value = Math.min(Math.sin(Math.toRadians(minDegree)) * 128 + 128, 256);

            Log.i(EVENTS_NAME, "Set brightness to " + Double.toString(value));
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) value);
        } catch (Exception e) {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness:" + e.toString());
        }
    }

}
