package denis.develops.utils.lightsensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private TextView minTextPercent;
    private TextView maxTextPercent;
    private TextView maxBatteryTextPercent;
    private TextView sensorTextPercent;
    private TextView updateFrequencyText;
    private Switch registerSwitch;
    private Switch registerSunSwitch;
    private Switch useBackCameraSwitch;
    private Switch useFootCandleSwitch;
    private Switch canntChangeBrightnessSwitch;
    private Switch dontUseCameraSwitch;
    private Switch lowPowerSwitch;
    private int lastMagnitudeSensorValue = 10;
    private int minLastPercentValue = 0;
    private int maxLastPercentValue = 100;
    private int maxBatteryLastPercentValue = 100;
    private SeekBar minPercentSeek;
    private SeekBar maxPercentSeek;
    private SeekBar maxBatteryPercentSeek;
    private SeekBar magnitudeSensorSeek;
    private SeekBar updateFrequencySeek;
    private boolean serviceEnabled = false;
    private boolean useBack = false;
    private boolean useFootCandle = false;
    private boolean dontUseCamera = false;
    private boolean cannotChangeBrightness = false;
    private boolean sunServiceEnabled = false;
    private boolean lowPowerEnabled = false;
    private int updateFrequencyValue = 1;

    private void updateTextValues() {
        minTextPercent.setText(Integer.toString(minLastPercentValue) + "%");
        maxTextPercent.setText(Integer.toString(maxLastPercentValue) + "%");
        maxBatteryTextPercent.setText(Integer.toString(maxBatteryLastPercentValue) + "%");
        sensorTextPercent.setText(Float.toString((((float) lastMagnitudeSensorValue + 10) / 20)) + "x");
        updateFrequencyText.setText(String.format(
                getString(denis.develops.utils.lightsensor.R.string.update_in_second),
                (float) (1 << (4 + updateFrequencyValue)) / 1024
        ));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sensorTextPercent = (TextView) findViewById(R.id.textSensorMagnitude);
        minTextPercent = (TextView) findViewById(R.id.textPercent);
        maxTextPercent = (TextView) findViewById(R.id.textMaxPercent);
        updateFrequencyText = (TextView) findViewById(R.id.textUpdateFrequency);
        maxBatteryTextPercent = (TextView) findViewById(R.id.textBatteryMaxPercent);
        magnitudeSensorSeek = (SeekBar) findViewById(R.id.sensorMagnitudeValue);
        minPercentSeek = (SeekBar) findViewById(R.id.percentValue);
        maxPercentSeek = (SeekBar) findViewById(R.id.percentMaxValue);
        maxBatteryPercentSeek = (SeekBar) findViewById(R.id.percentBatteryMaxValue);
        updateFrequencySeek = (SeekBar) findViewById(R.id.updateFrequencyValue);
        registerSwitch = (Switch) findViewById(R.id.switchAuto);
        registerSunSwitch = (Switch) findViewById(R.id.switchAutoSun);

        canntChangeBrightnessSwitch = (Switch) findViewById(R.id.disableChangeBrightness);
        useFootCandleSwitch = (Switch) findViewById(R.id.use_foot_candle);
        useBackCameraSwitch = (Switch) findViewById(R.id.useBackCamera);
        dontUseCameraSwitch = (Switch) findViewById(R.id.dontUseCamera);
        lowPowerSwitch = (Switch) findViewById(R.id.low_power_enabled);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        minLastPercentValue = prefs.getInt(MainActivity.MIN_PERCENT_VALUE, 0);
        maxLastPercentValue = prefs.getInt(MainActivity.MAX_PERCENT_VALUE, 100);
        maxBatteryLastPercentValue = prefs.getInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, 100);
        lastMagnitudeSensorValue = prefs.getInt(MainActivity.MAGNITUDE_SENSOR_VALUE, 10);
        updateFrequencyValue = prefs.getInt(MainActivity.FREQUENCY_VALUE, 4);

        updateTextValues();

        updateFrequencySeek.setMax(12);
        updateFrequencySeek.setProgress(updateFrequencyValue);
        updateFrequencySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                updateFrequencyValue = position;
                updateTextValues();
                savePreferences();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        magnitudeSensorSeek.setMax(40);
        magnitudeSensorSeek.setProgress(lastMagnitudeSensorValue);
        magnitudeSensorSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                lastMagnitudeSensorValue = position;
                updateTextValues();
                savePreferences();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        minPercentSeek.setMax(70);
        minPercentSeek.setProgress(minLastPercentValue + 10);
        minPercentSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                minLastPercentValue = position - 10;
                savePreferences();
                updateTextValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        maxPercentSeek.setMax(40);
        maxPercentSeek.setProgress(Math.max(maxLastPercentValue - 70, 0));
        maxPercentSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                maxLastPercentValue = position + 70;
                savePreferences();
                updateTextValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        maxBatteryPercentSeek.setMax(100);
        maxBatteryPercentSeek.setProgress(Math.max(maxBatteryLastPercentValue, 0));
        maxBatteryPercentSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                maxBatteryLastPercentValue = position;
                savePreferences();
                updateTextValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        useFootCandleSwitch.setChecked(prefs.getBoolean(MainActivity.USE_FOOT_CANDLE_FOR_SHOW, false));
        useFootCandleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useFootCandle = b;
                savePreferences();
            }
        });
        useFootCandle = useFootCandleSwitch.isChecked();

        registerSwitch.setChecked(prefs.getBoolean(MainActivity.AUTO_VALUE, false));
        registerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                serviceEnabled = b;
                savePreferences();
            }
        });
        serviceEnabled = registerSwitch.isChecked();

        registerSunSwitch.setChecked(prefs.getBoolean(MainActivity.AUTO_SUN_VALUE, false));
        registerSunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sunServiceEnabled = b;
                savePreferences();
            }
        });
        sunServiceEnabled = registerSunSwitch.isChecked();

        useBackCameraSwitch.setChecked(prefs.getBoolean(MainActivity.USE_BACK_CAMERA, false));
        useBackCameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useBack = b;
                savePreferences();
            }
        });
        useBack = useBackCameraSwitch.isChecked();

        canntChangeBrightnessSwitch.setChecked(prefs.getBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, false));
        canntChangeBrightnessSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                cannotChangeBrightness = b;
                savePreferences();
            }
        });
        cannotChangeBrightness = canntChangeBrightnessSwitch.isChecked();

        dontUseCameraSwitch.setChecked(prefs.getBoolean(MainActivity.DISABLE_CAMERA, false));
        dontUseCameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dontUseCamera = b;
                savePreferences();
            }
        });
        dontUseCamera = dontUseCameraSwitch.isChecked();

        lowPowerSwitch.setChecked(prefs.getBoolean(MainActivity.BATTERY_LOW, false));
        lowPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                lowPowerEnabled = b;
                savePreferences();
            }
        });
        lowPowerEnabled = lowPowerSwitch.isChecked();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(MainActivity.AUTO_VALUE, serviceEnabled);
        edit.putBoolean(MainActivity.AUTO_SUN_VALUE, sunServiceEnabled);
        edit.putBoolean(MainActivity.USE_FOOT_CANDLE_FOR_SHOW, useFootCandle);
        edit.putInt(MainActivity.MIN_PERCENT_VALUE, this.minLastPercentValue);
        edit.putInt(MainActivity.MAX_PERCENT_VALUE, this.maxLastPercentValue);
        edit.putInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, this.maxBatteryLastPercentValue);
        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, cannotChangeBrightness);
        edit.putBoolean(MainActivity.USE_BACK_CAMERA, useBack);
        edit.putBoolean(MainActivity.USE_FOOT_CANDLE_FOR_SHOW, useFootCandle);
        edit.putBoolean(MainActivity.DISABLE_CAMERA, dontUseCamera);
        edit.putBoolean(MainActivity.BATTERY_LOW, lowPowerEnabled);
        edit.putInt(MainActivity.MAGNITUDE_SENSOR_VALUE, this.lastMagnitudeSensorValue);
        edit.putInt(MainActivity.FREQUENCY_VALUE, this.updateFrequencyValue);
        edit.apply();
    }
}
