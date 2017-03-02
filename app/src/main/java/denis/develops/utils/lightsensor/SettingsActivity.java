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
    private TextView textPercent;
    private Switch registerSwitch;
    private Switch registerSunSwitch;
    private Switch useBackCameraSwitch;
    private Switch useFootCandleSwitch;
    private Switch canntChangeBrightnessSwitch;
    private Switch dontUseCameraSwitch;
    private int lastPercentValue = 0;
    private SeekBar percent_seek;
    private boolean serviceEnabled = false;
    private boolean useBack = false;
    private boolean useFootCandle = false;
    private boolean dontUseCamera = false;
    private boolean cannotChangeBrightness = false;
    private boolean sunServiceEnabled = false;

    private void updateTextValues() {
        textPercent.setText(Integer.toString(lastPercentValue) + "%");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        textPercent = (TextView) findViewById(R.id.textPercent);
        percent_seek = (SeekBar) findViewById(R.id.percentValue);
        registerSwitch = (Switch) findViewById(R.id.switchAuto);
        registerSunSwitch = (Switch) findViewById(R.id.switchAutoSun);


        canntChangeBrightnessSwitch = (Switch) findViewById(R.id.disableChangeBrightness);
        useFootCandleSwitch = (Switch) findViewById(R.id.use_foot_candle);
        useBackCameraSwitch = (Switch) findViewById(R.id.useBackCamera);
        dontUseCameraSwitch = (Switch) findViewById(R.id.dontUseCamera);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        lastPercentValue = prefs.getInt(MainActivity.PERCENT_VALUE, 0);
        updateTextValues();

        percent_seek.setMax(60);
        percent_seek.setProgress(lastPercentValue);
        percent_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                lastPercentValue = position;
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
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(MainActivity.AUTO_VALUE, serviceEnabled);
        edit.putBoolean(MainActivity.AUTO_SUN_VALUE, sunServiceEnabled);
        edit.putBoolean(MainActivity.USE_FOOT_CANDLE_FOR_SHOW, useFootCandle);
        edit.putInt(MainActivity.PERCENT_VALUE, this.lastPercentValue);
        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, cannotChangeBrightness);
        edit.putBoolean(MainActivity.USE_BACK_CAMERA, useBack);
        edit.putBoolean(MainActivity.USE_FOOT_CANDLE_FOR_SHOW, useFootCandle);
        edit.putBoolean(MainActivity.DISABLE_CAMERA, dontUseCamera);
        edit.apply();
    }
}
