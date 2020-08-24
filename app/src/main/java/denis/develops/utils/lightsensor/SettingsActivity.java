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
    private TextView cameraExposureText;
    private TextView updateFrequencyText;
    private TextView previewTimeText;
    private TextView previewTimeTitle;
    private TextView updateFrequencyTitle;
    private int lastMagnitudeSensorValue = 10;
    private int cameraExposureValue = 0;
    private int minPercentValue = 0;
    private int maxPercentValue = 100;
    private int maxBatteryLastPercentValue = 100;
    private boolean serviceEnabled = false;
    private boolean useBack = false;
    private boolean useFootCandle = false;
    private boolean useMonoPreview = true;
    private boolean dontUseCamera = false;
    private boolean cannotChangeBrightness = false;
    private boolean sunServiceEnabled = false;
    private boolean lowPowerEnabled = false;
    private int updateFrequencyValue = 1;
    private int previewTimeValue = 1;

    private void updateTextValues() {
        cameraExposureText.setText(Integer.toString(cameraExposureValue));
        minTextPercent.setText(minPercentValue + "%");
        maxTextPercent.setText(maxPercentValue + "%");
        maxBatteryTextPercent.setText(maxBatteryLastPercentValue + "%");
        sensorTextPercent.setText(((float) lastMagnitudeSensorValue + 10) / 20 + "x");
        updateFrequencyTitle.setText(getString(R.string.updateFrequencyTitle) + ", " + getString(R.string.seconds));
        updateFrequencyText.setText(String.format(
                getString(denis.develops.utils.lightsensor.R.string.update_in_second),
                (float) (1 << (4 + updateFrequencyValue)) / 1024
        ));
        if (previewTimeValue < 6) {
            previewTimeTitle.setText(getString(R.string.previewTimeTitle) + ", " + getString(R.string.minutes));
            previewTimeText.setText(Integer.toString(1 << previewTimeValue));
        } else if (previewTimeValue < 11) {
            previewTimeTitle.setText(getString(R.string.previewTimeTitle) + ", " + getString(R.string.hours));
            previewTimeText.setText(Integer.toString((1 << previewTimeValue) / 60));
        } else {
            previewTimeTitle.setText(getString(R.string.previewTimeTitle) + ", " + getString(R.string.days));
            previewTimeText.setText(Integer.toString((1 << previewTimeValue) / 60 / 24));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sensorTextPercent = findViewById(R.id.textSensorMagnitude);
        minTextPercent = findViewById(R.id.textPercent);
        maxTextPercent = findViewById(R.id.textMaxPercent);
        updateFrequencyText = findViewById(R.id.textUpdateFrequency);
        updateFrequencyTitle = findViewById(R.id.textUpdateFrequencyTitle);
        cameraExposureText = findViewById(R.id.textCameraExposure);
        previewTimeText = findViewById(R.id.textPreviewTime);
        previewTimeTitle = findViewById(R.id.textPreviewTimeTitle);
        maxBatteryTextPercent = findViewById(R.id.textBatteryMaxPercent);
        SeekBar magnitudeSensorSeek = findViewById(R.id.sensorMagnitudeValue);
        SeekBar minPercentSeek = findViewById(R.id.percentValue);
        SeekBar maxPercentSeek = findViewById(R.id.percentMaxValue);
        SeekBar maxBatteryPercentSeek = findViewById(R.id.percentBatteryMaxValue);
        SeekBar updateFrequencySeek = findViewById(R.id.updateFrequencyValue);
        SeekBar previewTimeSeek = findViewById(R.id.previewTimeValue);
        SeekBar cameraExposureSeek = findViewById(R.id.cameraExposureValue);
        Switch registerSwitch = findViewById(R.id.switchAuto);
        Switch registerSunSwitch = findViewById(R.id.switchAutoSun);

        Switch canntChangeBrightnessSwitch = findViewById(R.id.disableChangeBrightness);
        Switch useFootCandleSwitch = findViewById(R.id.use_foot_candle);
        Switch useMonoPreviewSwitch = findViewById(R.id.use_mono_preview);
        Switch useBackCameraSwitch = findViewById(R.id.useBackCamera);
        Switch dontUseCameraSwitch = findViewById(R.id.dontUseCamera);
        Switch lowPowerSwitch = findViewById(R.id.low_power_enabled);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        cameraExposureValue = prefs.getInt(MainActivity.CAMERA_EXPOSURE, 1);
        minPercentValue = prefs.getInt(MainActivity.MIN_PERCENT_VALUE, 0);
        maxPercentValue = prefs.getInt(MainActivity.MAX_PERCENT_VALUE, 100);
        maxBatteryLastPercentValue = prefs.getInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, 100);
        lastMagnitudeSensorValue = prefs.getInt(MainActivity.MAGNITUDE_SENSOR_VALUE, 10);
        updateFrequencyValue = prefs.getInt(MainActivity.FREQUENCY_VALUE, 4);
        previewTimeValue = prefs.getInt(MainActivity.PREVIEW_TIME_ACTIVE, 300);

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

        previewTimeSeek.setMax(17);
        previewTimeSeek.setProgress(previewTimeValue);
        previewTimeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                previewTimeValue = position;
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
        minPercentSeek.setProgress(minPercentValue + 10);
        minPercentSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                minPercentValue = position - 10;
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

        cameraExposureSeek.setMax(20);
        cameraExposureSeek.setProgress(cameraExposureValue + 10);
        cameraExposureSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                cameraExposureValue = position - 10;
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
        maxPercentSeek.setProgress(Math.max(maxPercentValue - 70, 0));
        maxPercentSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                maxPercentValue = position + 70;
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

        useMonoPreviewSwitch.setChecked(prefs.getBoolean(MainActivity.USE_MONO_PREVIEW, true));
        useMonoPreviewSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useMonoPreview = b;
                savePreferences();
            }
        });
        useMonoPreview = useMonoPreviewSwitch.isChecked();

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
        edit.putBoolean(MainActivity.USE_MONO_PREVIEW, useMonoPreview);
        edit.putInt(MainActivity.CAMERA_EXPOSURE, this.cameraExposureValue);
        edit.putInt(MainActivity.MIN_PERCENT_VALUE, this.minPercentValue);
        edit.putInt(MainActivity.MAX_PERCENT_VALUE, this.maxPercentValue);
        edit.putInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, this.maxBatteryLastPercentValue);
        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, cannotChangeBrightness);
        edit.putBoolean(MainActivity.USE_BACK_CAMERA, useBack);
        edit.putBoolean(MainActivity.DISABLE_CAMERA, dontUseCamera);
        edit.putBoolean(MainActivity.BATTERY_LOW, lowPowerEnabled);
        edit.putInt(MainActivity.MAGNITUDE_SENSOR_VALUE, this.lastMagnitudeSensorValue);
        edit.putInt(MainActivity.FREQUENCY_VALUE, this.updateFrequencyValue);
        edit.putInt(MainActivity.PREVIEW_TIME_ACTIVE, this.previewTimeValue);
        edit.apply();
    }
}
