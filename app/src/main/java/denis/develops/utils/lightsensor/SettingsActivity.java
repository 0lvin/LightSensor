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
    private Switch useBackCameraSwitch;
    private Switch canntChangeBrightnessSwitch;
    private int lastPercentValue = 0;
    private SeekBar percent_seek;
    private boolean serviceEnabled = false;
    private boolean useBack = false;
    private boolean cannotChangeBrightness = false;

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
        canntChangeBrightnessSwitch = (Switch) findViewById(R.id.disableChangeBrightness);
        useBackCameraSwitch = (Switch) findViewById(R.id.useBackCamera);

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
        registerSwitch.setChecked(prefs.getBoolean(MainActivity.AUTO_VALUE, false));

        registerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                serviceEnabled = b;
                savePreferences();
            }
        });
        serviceEnabled = registerSwitch.isChecked();

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
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(MainActivity.AUTO_VALUE, serviceEnabled);
        edit.putInt(MainActivity.PERCENT_VALUE, this.lastPercentValue);
        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, cannotChangeBrightness);
        edit.putBoolean(MainActivity.USE_BACK_CAMERA, useBack);
        edit.apply();
    }
}
