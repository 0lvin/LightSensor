package denis.develops.utils.lightsensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    final static String PREFERENCES_NAME = "preferences";
    final static String MAGNITUDE_VALUE = "MagnitudeValue";
    final static String PERCENT_VALUE = "PercentValue";
    final static String RUNTIME_VALUE = "LastRunTime";
    final static String EVENTS_NAME = "LightsSensors";
    final static String AUTO_VALUE = "AutoUpdateOnEvent";
    final static String DISABLE_CHANGE_BRIGHTNESS = "DisableChangeBrightness";
    final static String DISABLE_CAMERA = "DisableCamera";
    final static String USE_BACK_CAMERA = "UseBackCamera";
    final static int IWANTCAMERA = 1;
    final static int IWANTCHANGESETTINGS = 2;
    private ContentResolver cResolver;
    private int lastBrightnessValue = 0;
    private int lastMagnitudeValue = 10;
    private int percentValueSettings = 0;
    private float lastLightSensorValue = 0;
    private float lastCameraSensorValue = 0;
    private ProgressBar bar;
    private SeekBar magnitude_seek;
    private Camera camera = null;
    private SurfaceView preview;
    private SurfaceHolder surfaceHolder;
    private TextView textAuthor;
    private TextView textLightSensor;
    private boolean usedFront = false;
    private boolean usedLightSensor = false;
    private boolean usedBack = false;
    private boolean useBack = false;
    private boolean cannotChangeBrightness = false;
    private boolean dontUseCamera = false;
    private TextView textCameraLight;
    private TextView textMagnitude;
    private Date startTime;
    private long lastTime;

    public static float getMiddleIntense(byte[] data, int width, int height) {
        long sum = 0;
        int size = width * height;
        for (int i = 0; i < size; i++) {
            sum += data[i] & 0xFF;
        }
        return sum / size;
    }

    private void registerBroadcastReceiver() {
        try {
            Log.i(this.EVENTS_NAME, "Register unlock receiver.");
            final IntentFilter theFilter = new IntentFilter();
            /** System Defined Broadcast */
            theFilter.addAction(Intent.ACTION_SCREEN_ON);
            theFilter.addAction(Intent.ACTION_USER_PRESENT);
            UnlockReceiver mUnlockReceiver = new UnlockReceiver();

            getApplicationContext().registerReceiver(mUnlockReceiver, theFilter);

            // disable service on system level
            ComponentName receiver = new ComponentName(getApplicationContext(), UnlockReceiver.class);
            PackageManager pm = getApplicationContext().getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

        } catch (Exception e) {
            Log.e("Error", "Cannot register unlock receiver:" + e.toString());
        }
    }

    private Camera.Size getMinimalPreviewSize(Camera.Parameters params) {
        int minWidth = -1;
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            int compareWidth = sizes.get(i).width;
            if (minWidth == -1) {
                minWidth = compareWidth;
            } else if (minWidth > compareWidth) {
                minWidth = compareWidth;
            }
        }
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).width == minWidth) {
                return sizes.get(i);
            }
        }
        //something wrong, but try return first
        return sizes.get(0);
    }

    private int getFrontCameraId(CameraInfo info) {
        //search front camera
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                usedFront = true;
                return i;
            }
        }
        return -1;
    }

    private int getBackCameraId(CameraInfo info) {
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                usedBack = true;
                return i;
            }
        }
        return -1;
    }

    /*
        returned camera, front camera preferred
     */
    private int getCameraId() {
        CameraInfo info = new CameraInfo();
        int camera_id = -1;
        usedBack = false;
        usedFront = false;
        if (!useBack) {
            camera_id = this.getFrontCameraId(info);
        } else {
            camera_id = this.getBackCameraId(info);
        }
        if (camera_id > -1) {
            return camera_id;
        }
        // looks as does not exist
        if (!useBack) {
            return this.getBackCameraId(info);
        } else {
            return this.getFrontCameraId(info);
        }
    }

    protected void init_camera() {
        if (camera != null)
            return;

        int cameraId = this.getCameraId();
        if (cameraId != -1) {
            camera = Camera.open(cameraId);
            Camera.Parameters params = camera.getParameters();
            params.setColorEffect(Camera.Parameters.EFFECT_MONO);
            params.setPreviewFormat(ImageFormat.NV21);
            if (params.isAutoExposureLockSupported()) {
                params.setAutoExposureLock(true);
                params.setExposureCompensation(1);
            }
            Camera.Size previewSize = getMinimalPreviewSize(params);
            params.setPreviewSize(previewSize.width, previewSize.height);
            camera.setParameters(params);
        } else {
            dontUseCamera = true;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(this.PREFERENCES_NAME, MODE_PRIVATE);
        percentValueSettings = prefs.getInt(this.PERCENT_VALUE, 0);
        cannotChangeBrightness = prefs.getBoolean(this.DISABLE_CHANGE_BRIGHTNESS, false);
        dontUseCamera = prefs.getBoolean(this.DISABLE_CAMERA, false);
        if (!cannotChangeBrightness || !dontUseCamera) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // temporary disable camera usage
                    dontUseCamera = true;
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, IWANTCAMERA);
                }
                if (checkSelfPermission(android.Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED || !Settings.System.canWrite(this)) {
                    // temporary disable camera usage
                    cannotChangeBrightness = true;
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_SETTINGS}, IWANTCHANGESETTINGS);
                }
            }
        }
        useBack = prefs.getBoolean(this.USE_BACK_CAMERA, false);
        if (!dontUseCamera) {
            this.init_camera();
        }
        lastCameraSensorValue = 0;
        lastLightSensorValue = 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case IWANTCAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dontUseCamera = false;

                } else {
                    SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(MainActivity.DISABLE_CAMERA, true);
                    edit.apply();
                }
                break;
            }
            case IWANTCHANGESETTINGS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cannotChangeBrightness = false;

                } else {
                    SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, true);
                    edit.apply();
                }
                break;
            }
        }
    }

    protected void delete_camera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        camera = null;
    }

    private void savePreferences(long newTimeValue) {
        SharedPreferences prefs = getSharedPreferences(this.PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(this.MAGNITUDE_VALUE, this.lastMagnitudeValue);
        if (newTimeValue > 0) {
            edit.putLong(this.RUNTIME_VALUE, newTimeValue);
        }
        edit.apply();
    }

    @Override
    protected void onPause() {
        Date current = new Date();
        long newTimeValue = this.lastTime + (current.getTime() - this.startTime.getTime()) / 1000;
        super.onPause();
        delete_camera();
        this.savePreferences(newTimeValue);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private long lastUpdate = 0;

    private void updateTextValues() {
        Date current = new Date();
        long newTime = current.getTime() / 1000;
        if (lastUpdate == newTime)
            return;
        lastUpdate = newTime;
        Long usedSeconds = this.lastTime % 60;
        Long usedMinutes = (this.lastTime / 60) % 60;
        Long usedHours = (this.lastTime / 3600);

        String sensorText = getString(R.string.sensor_light);
        if (!this.usedLightSensor) {
            textLightSensor.setText(sensorText + getString(R.string.sensor_not_exist));
        } else {
            textLightSensor.setText(sensorText + Integer.toString((int) lastLightSensorValue));
        }
        String cameraText = getString(R.string.camera_light);
        textCameraLight.setText(cameraText + Integer.toString((int) (SensorManager.LIGHT_OVERCAST * lastCameraSensorValue / 255)));
        textMagnitude.setText(Float.toString(getMagnitude()) + "x");
        String stateText = getString(R.string.license_text) + "\n";
        if (usedLightSensor) {
            stateText += getString(R.string.used_light_sensor) + "\n";
        }
        if (usedBack) {
            stateText += getString(R.string.used_back_camera) + "\n";
        }
        if (usedFront) {
            stateText += getString(R.string.used_front_camera) + "\n";
        }
        stateText += getString(R.string.have_used_for) + " ";
        stateText += Long.toString(usedHours) + " " + getString(R.string.hours) + " ";
        stateText += Long.toString(usedMinutes) + " " + getString(R.string.minutes) + " ";
        stateText += Long.toString(usedSeconds) + " " + getString(R.string.seconds) + ".\n";
        textAuthor.setText(stateText);
    }

    private void initLightSensor() {
        // Obtain references to the SensorManager and the Light Sensor
        final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (sensors.size() > 0) {
            usedLightSensor = true;
            final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            // Implement a listener to receive updates
            SensorEventListener listener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    lastLightSensorValue = event.values[0];
                    updateTextValues();
                    if (dontUseCamera) {
                        int iColor = (int) (lastLightSensorValue / SensorManager.LIGHT_OVERCAST * 256);
                        preview.setBackgroundColor(Color.argb(0, iColor, iColor, iColor));
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

            sensorManager.registerListener(
                    listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void easterEggInit() {
        textAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://0lvin.blogspot.com/"));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textMagnitude = (TextView) findViewById(R.id.textMagnitude);

        textAuthor = (TextView) findViewById(R.id.textViewAuthor);
        textLightSensor = (TextView) findViewById(R.id.textLight);
        textCameraLight = (TextView) findViewById(R.id.textCameraLight);
        bar = (ProgressBar) findViewById(R.id.brightnessValue);
        magnitude_seek = (SeekBar) findViewById(R.id.magnitudeValue);

        preview = (SurfaceView) findViewById(R.id.imageView);

        cResolver = getContentResolver();

        registerBroadcastReceiver();

        this.initLightSensor();
        this.easterEggInit();

        this.startTime = new Date();
        if (savedInstanceState != null) {
            lastMagnitudeValue = savedInstanceState.getInt(this.MAGNITUDE_VALUE, 10);
            this.lastTime = savedInstanceState.getLong(this.RUNTIME_VALUE, 0);
        } else {
            SharedPreferences prefs = getSharedPreferences(this.PREFERENCES_NAME, MODE_PRIVATE);
            percentValueSettings = prefs.getInt(this.PERCENT_VALUE, 0);
            lastMagnitudeValue = prefs.getInt(this.MAGNITUDE_VALUE, 10);
            this.lastTime = prefs.getLong(this.RUNTIME_VALUE, 0);
        }

        magnitude_seek.setMax(40);
        magnitude_seek.setProgress(lastMagnitudeValue);
        magnitude_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                lastMagnitudeValue = position;
                updateTextValues();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        bar.setMax(256);
        bar.setProgress(this.lastBrightnessValue);

        surfaceHolder = preview.getHolder();
        surfaceHolder.addCallback(this);
        this.updateTextValues();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Date current = new Date();
        long newTimeValue = this.lastTime + (current.getTime() - this.startTime.getTime()) / 1000;
        super.onSaveInstanceState(outState);
        outState.putInt(this.MAGNITUDE_VALUE, this.lastMagnitudeValue);
        outState.putLong(this.RUNTIME_VALUE, newTimeValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera != null) {
            try {

                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(this);

                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                float aspect = (float) previewSize.width / previewSize.height;

                int previewSurfaceWidth = preview.getWidth();
                int previewSurfaceHeight = preview.getHeight();

                LayoutParams lp = preview.getLayoutParams();

                // fix orientation
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    // portrait
                    camera.setDisplayOrientation(90);
                    lp.height = previewSurfaceHeight;
                    lp.width = (int) (previewSurfaceHeight / aspect);

                } else {
                    // landscape
                    camera.setDisplayOrientation(0);
                    lp.width = previewSurfaceWidth;
                    lp.height = (int) (previewSurfaceWidth / aspect);
                }

                preview.setLayoutParams(lp);
                camera.startPreview();
            } catch (IOException e) {
                Log.e("Error", "Cannot access system brightness:" + e.toString());
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private float getMagnitude() {
        return (((float) lastMagnitudeValue + 10) / 20);
    }

    private void updateBrightness() {
        float cameraLightValue = lastCameraSensorValue * getMagnitude()
                + (percentValueSettings * 256 / 100);
        float sensorLightValue = lastLightSensorValue / SensorManager.LIGHT_OVERCAST * 256;
        if (!usedLightSensor) {
            // we don't have such sensor so use value from camera
            sensorLightValue = cameraLightValue;
        }

        // create something in the middle
        int newBrightness = (int) ((sensorLightValue + cameraLightValue + lastBrightnessValue) / 3);
        if (newBrightness > 255) {
            newBrightness = 255;
        }

        this.updateTextValues();

        // check difference, but don't do any changes if values similar
        if (Math.abs(cameraLightValue - newBrightness) > 5) {
            lastBrightnessValue = newBrightness;
            bar.setProgress(lastBrightnessValue);
            if (!cannotChangeBrightness) {
                try {
                    System.putInt(getContentResolver(), System.SCREEN_BRIGHTNESS, lastBrightnessValue);
                } catch (Exception e) {
                    //Throw an error case it couldn't be retrieved
                    Log.e("Error", "Cannot access system brightness:" + e.toString());
                }
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size size = params.getPreviewSize();
        lastCameraSensorValue = this.getMiddleIntense(data, size.width, size.height);
        this.updateBrightness();
    }
}
