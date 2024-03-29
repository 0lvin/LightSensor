package denis.develops.utils.lightsensor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements Camera.PreviewCallback, Camera.AutoFocusCallback {

    final static String PREFERENCES_NAME = "preferences";
    final static String MAGNITUDE_SENSOR_VALUE = "MagnitudeSensorValue";
    final static String MIN_PERCENT_VALUE = "PercentValue";
    final static String MAX_PERCENT_VALUE = "MaxPercentValue";
    final static String MAX_BATTERY_PERCENT_VALUE = "MaxBatteryPercentValue";
    final static String BATTERY_LOW = "BatteryLowUsed";
    final static String AUTO_VALUE = "AutoUpdateOnEvent";
    final static String AUTO_SUN_VALUE = "AutoUpdateOnEventSun";
    final static String DISABLE_CHANGE_BRIGHTNESS = "DisableChangeBrightness";
    final static String USE_FOOT_CANDLE_FOR_SHOW = "FootCandle";
    final static String USE_MONO_PREVIEW = "MonoPreviewOnly";
    final static String CAMERA_EXPOSURE = "CameraExposure";
    final static String DISABLE_CAMERA = "DisableCamera";
    final static String USE_BACK_CAMERA = "UseBackCamera";
    final static String LONGITUDE_VALUE = "LongitudeValue";
    final static String LATITUDE_VALUE = "LatitudeValue";
    final static String FREQUENCY_VALUE = "UpdateFrequencyValue";
    final static String PREVIEW_TIME_ACTIVE = "PreviewTimeActive";
    final static String TIMER_PERIOD_VALUE = "TimerPeriodValue";
    private final static String MAGNITUDE_VALUE = "MagnitudeValue";
    private final static String RUNTIME_VALUE = "LastRunTime";
    private final static String EVENTS_NAME = "LightsSensors";
    private final static String ALTITUDE_VALUE = "AltitudeValue";
    private final static int IWANTCAMERA = 1;
    private final static int IWANTCHANGESETTINGS = 2;
    private final static int IWANTLOCATION = 3;
    private final static int PRECISE = 100000;
    private Camera.Size cameraPreviewSize = null;
    private long lastUpdateTimeMillisecondsStamp = 0;
    private SensorEventListener lightsSensorListener = null;
    private SensorManager sensorManager = null;
    private int activeTimeLeft = 60;
    private long activeTimeLeftBase = 0;
    private int lastBrightnessValue = 0;
    private int lastMagnitudeValue = 10;
    private int lastMagnitudeSensorValue = 10;
    private int minPercentValueSettings = 0;
    private int maxPercentValueSettings = 100;
    /*
        3 ~ 8 fps
        4 ~ 4 fps
        5 ~ 2 fps
        6 ~ 1 fps
        7 ~ 0.5 fps
        8 ~ 0.25 fps (once in 4 seconds)
     */
    private int updateFrequencyValue = 1;
    private float lastLightSensorValue = 0;
    private float lastCameraSensorValue = 0;
    private ProgressBar bar;
    private Camera camera = null;
    private TextureView previewTexture;
    private SurfaceView previewSurface;
    private TextView textAuthor;
    private TextView textLightSensor;
    private boolean usedFront = false;
    private boolean usedLightSensor = false;
    private boolean usedBack = false;
    private boolean useBack = false;
    private boolean useSunFix = false;
    private boolean cannotChangeBrightness = false;
    private boolean dontUseCamera = false;
    private boolean useFootCandle = false;
    private boolean useMonoPreview = true;
    private boolean low_battery = false;
    private TextView textCameraLight;
    private TextView textMagnitude;
    private Date startTime;
    private long lastTime;
    private long lastUpdate;
    private int cameraExposure;
    // location
    private LocationListener locationListener = null;
    private float locationAltitude = 0;
    private float locationLongitude = 0;
    private float locationLatitude = 0;
    private String locationString = "";

    /*
      data - NV21 raw data from camera (width * height * 2),
      width - preview width,
      height - preview height,
      pos - pos in step,
      step - step for for
     */
    private static float getMiddleIntense(byte[] data, int width, int height, int pos, int step) {
        if (step <= 0) {
            step = 1;
        }
        long sum = 0;
        int size = width * height;
        for (int i = 0; i < size; i += step) {
            sum += data[i + pos] & 0xFF;
        }
        return (sum * step) / size;
    }

    private void registerBroadcastReceiver(int timerPeriodValue) {
        UnlockReceiver mUnlockReceiver = new UnlockReceiver();
        mUnlockReceiver.registerReceivers(getApplicationContext(), timerPeriodValue);
    }

    private int[] getMinimalFps(Camera.Parameters params) {
        List<int[]> fpsValue;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
            return null;
        }
        fpsValue = params.getSupportedPreviewFpsRange();

        if (fpsValue == null) {
            return null;
        }

        int minFps = 0;
        int pos = 0;
        for (int i = 0; i < fpsValue.size(); i++) {
            if (minFps > fpsValue.get(i)[1] || minFps == 0) {
                minFps = fpsValue.get(i)[1];
                pos = i;
            }
        }
        return fpsValue.get(pos);
    }

    private String getMinimalIso(Camera.Parameters params) {
        String fullIsoListString = params.get("iso-values");
        int minIso = 0;
        String result = null;
        Pattern intPattern = Pattern.compile("\\d+");
        if (fullIsoListString != null) {
            String[] fullIsoList = fullIsoListString.split(",");
            for (String aFullIsoList : fullIsoList) {
                String isoString = aFullIsoList;
                int currValue;
                if (isoString == null)
                    continue;
                isoString = isoString.toLowerCase();
                if (isoString.startsWith("iso")) {
                    isoString = isoString.substring("iso".length());
                }
                // skip auto value
                if ("auto".equals(isoString))
                    continue;

                if (!intPattern.matcher(isoString).matches())
                    continue;

                // try convert to int
                currValue = Integer.parseInt(isoString);
                if (minIso == 0) {
                    minIso = currValue;
                    result = aFullIsoList;
                }

                // compare with minimal
                if (currValue != 0) {
                    if (minIso > currValue) {
                        minIso = currValue;
                        result = aFullIsoList;
                    }
                }
            }
        }
        // if we found something
        if (minIso > 0) {
            return result;
        }
        return null;
    }

    private Camera.Size getMinimalPreviewSize(Camera.Parameters params) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR) {
            return null;
        }
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
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return -1;
        }
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
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return -1;
        }
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
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return -1;
        }
        CameraInfo info = new CameraInfo();
        int camera_id;
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

    private void initCamera(boolean monoEnabled) {
        if (camera != null)
            return;

        int cameraId = this.getCameraId();
        if (cameraId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            try {
                camera = Camera.open(cameraId);
            } catch (Exception e) {
                Log.e(EVENTS_NAME, "Camera used by other application?:" + e.toString());
                dontUseCamera = true;
                return;
            }

            Camera.Parameters params = camera.getParameters();
            if (monoEnabled) {
                try {
                    params.setColorEffect(Camera.Parameters.EFFECT_MONO);
                    camera.setParameters(params);
                } catch (Exception e) {
                    params = camera.getParameters();
                    Log.e(EVENTS_NAME, "Cannot set color effect:" + e.toString());
                }
            }
            try {
                params.setPreviewFormat(ImageFormat.NV21);
                camera.setParameters(params);
            } catch (Exception e) {
                params = camera.getParameters();
                Log.e(EVENTS_NAME, "Cannot set preview format:" + e.toString());
            }

            String minimalIso = getMinimalIso(params);
            if (minimalIso != null) {
                try {
                    params.set("iso", minimalIso);
                    camera.setParameters(params);
                } catch (Exception e) {
                    params = camera.getParameters();
                    Log.e(EVENTS_NAME, "Cannot set camera iso value:" + e.toString());
                }
            }
            int[] fpsValue = getMinimalFps(params);
            if (fpsValue != null) {
                try {
                    params.setPreviewFpsRange(fpsValue[0], fpsValue[1]);
                    camera.setParameters(params);
                } catch (Exception e) {
                    params = camera.getParameters();
                    Log.e(EVENTS_NAME, "Cannot set fps range:" + e.toString());
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                if (params.isAutoExposureLockSupported()) {
                    try {
                        params.setAutoExposureLock(true);
                        if (cameraExposure > params.getMaxExposureCompensation()) {
                            cameraExposure = params.getMaxExposureCompensation();
                        }
                        if (cameraExposure < params.getMinExposureCompensation()) {
                            cameraExposure = params.getMinExposureCompensation();
                        }
                        params.setExposureCompensation(cameraExposure);
                        camera.setParameters(params);
                    } catch (Exception e) {
                        params = camera.getParameters();
                        Log.e(EVENTS_NAME, "Cannot set exposure:" + e.toString());
                    }
                }
            }
            Camera.Size previewSize = getMinimalPreviewSize(params);
            if (previewSize != null) {
                try {
                    params.setPreviewSize(previewSize.width, previewSize.height);
                    camera.setParameters(params);
                } catch (Exception e) {
                    params = camera.getParameters();
                    Log.e(EVENTS_NAME, "Issue with camera set params:" + e.toString());
                }
            }
            cameraPreviewSize = params.getPreviewSize();
        } else {
            dontUseCamera = true;
        }
    }

    private void updateLocation(Location location) {
        if (location != null) {
            try {
                locationAltitude = (float) Math.round(location.getAltitude() * PRECISE) / PRECISE;
                locationLongitude = (float) Math.round(location.getLongitude() * PRECISE) / PRECISE;
                locationLatitude = (float) Math.round(location.getLatitude() * PRECISE) / PRECISE;
                Log.i(EVENTS_NAME, String.format(getString(R.string.LatString), locationLatitude));
                Log.i(EVENTS_NAME, String.format(getString(R.string.LongString), locationLongitude));
                Log.i(EVENTS_NAME, String.format(getString(R.string.AltString), locationAltitude));
            } catch (Exception e) {
                Log.e(EVENTS_NAME, "Issue with location:" + e.toString());
            }
        }
        generateLocationString();
    }

    private void initLocationSensor() {
        if (locationListener != null)
            return;

        // init location receiver
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                updateLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Register the listener with the Location Manager to receive location updates

                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 100, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    updateLocation(lastKnownLocation);
                }

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Date current = new Date();
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        useFootCandle = prefs.getBoolean(USE_FOOT_CANDLE_FOR_SHOW, false);
        useMonoPreview = prefs.getBoolean(USE_MONO_PREVIEW, true);
        cameraExposure = prefs.getInt(CAMERA_EXPOSURE, 1);
        minPercentValueSettings = prefs.getInt(MIN_PERCENT_VALUE, 0);
        maxPercentValueSettings = prefs.getInt(MAX_PERCENT_VALUE, 100);
        updateFrequencyValue = prefs.getInt(FREQUENCY_VALUE, 4);
        activeTimeLeft = (1 << prefs.getInt(PREVIEW_TIME_ACTIVE, 1)) * 60;
        activeTimeLeftBase = current.getTime() / 1000;

        if (prefs.getBoolean(BATTERY_LOW, false)) {
            int maxBatteryPercentValue = prefs.getInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, 100);
            if (maxBatteryPercentValue < maxPercentValueSettings) {
                maxPercentValueSettings = maxBatteryPercentValue;
            }
        }
        cannotChangeBrightness = prefs.getBoolean(DISABLE_CHANGE_BRIGHTNESS, false);
        dontUseCamera = prefs.getBoolean(DISABLE_CAMERA, false);
        low_battery = prefs.getBoolean(BATTERY_LOW, false);
        if (!cannotChangeBrightness || !dontUseCamera) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!dontUseCamera)
                    if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // temporary disable camera usage
                        dontUseCamera = true;
                        requestPermissions(new String[]{android.Manifest.permission.CAMERA}, IWANTCAMERA);
                    }
                if (!cannotChangeBrightness) {
                    if (!Settings.System.canWrite(this)) {
                        // temporary disable settings change
                        cannotChangeBrightness = true;
                        // add call back for check settings
                        if (checkSelfPermission(android.Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.WRITE_SETTINGS}, IWANTCHANGESETTINGS);
                        }
                        // really ask about permission
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + this.getPackageName()));
                        startActivity(intent);
                    }
                }
            }
        }
        useBack = prefs.getBoolean(USE_BACK_CAMERA, false);
        if (!dontUseCamera) {
            this.initCamera(useMonoPreview);
            this.recreatePreview();
        }
        lastCameraSensorValue = 0;
        lastLightSensorValue = 0;
        this.initLightSensor();

        useSunFix = prefs.getBoolean(AUTO_SUN_VALUE, false);
        if (useSunFix) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // temporary disable unlock sun update
                    useSunFix = false;
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, IWANTLOCATION);
                }
            }
        }

        if (useSunFix) {
            initLocationSensor();
        }
    }

    private void recreatePreview() {
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (previewTexture.isAvailable()) {
                openCamera();
            }
            // fix texture listener
            previewTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    deleteCamera();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            });
        } else {
            previewSurface.getHolder().addCallback(new SurfaceHolder.Callback() {

                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    openCamera();
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    deleteCamera();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case IWANTLOCATION: {
                // If request is cancelled, the result arrays are empty.
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    useSunFix = true;
                    initLocationSensor();
                    edit.putBoolean(MainActivity.AUTO_SUN_VALUE, true);
                } else {
                    edit.putBoolean(MainActivity.AUTO_SUN_VALUE, false);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    edit.apply();
                }
                break;
            }
            case IWANTCAMERA: {
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dontUseCamera = false;
                    this.initCamera(useMonoPreview);
                    edit.putBoolean(MainActivity.DISABLE_CAMERA, false);
                } else {
                    edit.putBoolean(MainActivity.DISABLE_CAMERA, true);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    edit.apply();
                }
                break;
            }
            case IWANTCHANGESETTINGS: {
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cannotChangeBrightness = false;

                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
                        // maybe i have rights?
                        cannotChangeBrightness = false;
                        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, false);
                    } else {
                        edit.putBoolean(MainActivity.DISABLE_CHANGE_BRIGHTNESS, true);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    edit.apply();
                }
                break;
            }
        }
    }

    private void deleteCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        camera = null;
    }

    private void savePreferences(long newTimeValue) {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(MAGNITUDE_VALUE, this.lastMagnitudeValue);
        edit.putFloat(ALTITUDE_VALUE, this.locationAltitude);
        edit.putFloat(LATITUDE_VALUE, this.locationLatitude);
        edit.putFloat(LONGITUDE_VALUE, this.locationLongitude);
        edit.putInt(CAMERA_EXPOSURE, cameraExposure);
        if (newTimeValue > 0) {
            edit.putLong(RUNTIME_VALUE, newTimeValue);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            edit.apply();
        }
    }

    private void destroyLightSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE && lightsSensorListener != null && sensorManager != null) {
            sensorManager.unregisterListener(lightsSensorListener);
        }
        sensorManager = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Date current = new Date();
        long newTimeValue = this.lastTime + (current.getTime() - this.startTime.getTime()) / 1000;
        this.destroyLightSensor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (locationListener != null) {
                    // init location receiver
                    // Acquire a reference to the system Location Manager
                    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                    locationListener = null;
                }
            }
        }

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
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.source_code) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/0lvin/LightSensor"));
            startActivity(browserIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void generateLocationString() {
        locationString = String.format(getString(R.string.LatString) + "\n" +
                        getString(R.string.LongString) + "\n" +
                        getString(R.string.AltString) + "\n",
                locationLatitude, locationLongitude, locationAltitude);

        double sunrise = UnlockReceiver.getSunsetTime(true, locationLongitude, locationLatitude);
        if (sunrise < -1 && sunrise > 24) {
            locationString += getString(R.string.sun_never_rises);
        } else {
            int hour = (int) sunrise;
            int minutes = (int) ((sunrise - hour) * 60);
            locationString += String.format(getString(R.string.sunrise_time), hour, minutes);
        }
        locationString += "\n";

        double sunset = UnlockReceiver.getSunsetTime(false, locationLongitude, locationLatitude);
        if (sunset < -1 && sunset > 24) {
            locationString += getString(R.string.sun_never_sets);
        } else {
            int hour = (int) sunset;
            int minutes = (int) ((sunset - hour) * 60);
            locationString += String.format(getString(R.string.sunset_time), hour, minutes);
        }
        locationString += "\n";
    }

    private void updateShowedValues() {
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
            if (this.useFootCandle) {
                textLightSensor.setText(sensorText + String.format("%.2f", (float) (lastLightSensorValue * 0.0929)));
            } else {
                textLightSensor.setText(sensorText + String.format("%.1f", (float) lastLightSensorValue));
            }
        }
        String cameraText = getString(R.string.camera_light);
        float light_value_lux = SensorManager.LIGHT_OVERCAST * lastCameraSensorValue / 255;
        if (useFootCandle) {
            textCameraLight.setText(cameraText + String.format("%.2f", (float) (light_value_lux * 0.0929)));
        } else {
            textCameraLight.setText(cameraText + String.format("%.1f", (float) light_value_lux));
        }
        textMagnitude.setText(getMagnitude() + "x");
        String stateText = "";
        long time_left = (activeTimeLeftBase + activeTimeLeft) - newTime;
        if (time_left > 0) {
            stateText += getString(R.string.preview_active_seconds) + " ";
            if (time_left < 120) {
                stateText += time_left + " " + getString(R.string.seconds) + ".\n";
            } else if (time_left < 3600) {
                stateText += (int) (time_left / 60) + " " + getString(R.string.minutes) + ".\n";
            } else if (time_left < (3600 * 24 * 2)) {
                stateText += (int) (time_left / 3600) + " " + getString(R.string.hours) + ".\n";
            } else {
                stateText += (int) (time_left / (3600 * 24)) + " " + getString(R.string.days) + ".\n";
            }
        } else {
            stateText += getString(R.string.preview_stopped_some_time) + "\n";
            // disable camera sensor
            deleteCamera();
            // disable lights sensor
            destroyLightSensor();
            View.OnClickListener clickCallback = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Date current = new Date();
                    activeTimeLeftBase = current.getTime() / 1000;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        previewTexture.setOnClickListener(null);
                    } else {
                        previewSurface.setOnClickListener(null);
                    }

                    if (!dontUseCamera) {
                        initCamera(useMonoPreview);
                        recreatePreview();
                    }

                    // restore light sensor
                    initLightSensor();
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                previewTexture.setOnClickListener(clickCallback);
            } else {
                previewSurface.setOnClickListener(clickCallback);
            }
        }
        stateText += getString(R.string.license_text) + "\n";
        if (this.useFootCandle) {
            stateText += getString(R.string.used_foot_candle) + "\n";
        } else {
            stateText += getString(R.string.used_lux) + "\n";
        }
        if (this.usedLightSensor) {
            stateText += getString(R.string.used_light_sensor) + "\n";
        }
        if (this.usedBack) {
            stateText += getString(R.string.used_back_camera) + "\n";
        }
        if (this.usedFront) {
            stateText += getString(R.string.used_front_camera) + "\n";
        }
        stateText += getString(R.string.have_used_for) + " ";
        stateText += usedHours + " " + getString(R.string.hours) + " ";
        stateText += usedMinutes + " " + getString(R.string.minutes) + " ";
        stateText += usedSeconds + " " + getString(R.string.seconds) + ".\n";
        stateText += this.locationString;
        stateText += getString(R.string.low_battery) + " " + this.low_battery;

        textAuthor.setText(stateText);
        if (dontUseCamera && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            previewTexture.setAlpha(lastLightSensorValue / SensorManager.LIGHT_OVERCAST);
        }

        bar.setProgress(lastBrightnessValue);
    }

    private void initLightSensor() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.CUPCAKE) {
            // Obtain references to the SensorManager and the Light Sensor
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
                if (sensors.size() > 0) {
                    usedLightSensor = true;
                    final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                    if (lightsSensorListener != null && sensorManager != null) {
                        sensorManager.registerListener(
                                lightsSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textMagnitude = findViewById(R.id.textMagnitude);

        textAuthor = findViewById(R.id.textViewAuthor);
        textLightSensor = findViewById(R.id.textLight);
        textCameraLight = findViewById(R.id.textCameraLight);
        bar = findViewById(R.id.brightnessValue);
        SeekBar magnitude_seek = findViewById(R.id.magnitudeValue);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            previewTexture = findViewById(R.id.imageView);
        } else {
            previewSurface = findViewById(R.id.imageView);
        }

        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        int timerPeriodValue = prefs.getInt(MainActivity.TIMER_PERIOD_VALUE, 4);
        registerBroadcastReceiver(timerPeriodValue);

        this.startTime = new Date();
        lastMagnitudeSensorValue = prefs.getInt(MAGNITUDE_SENSOR_VALUE, 10);
        minPercentValueSettings = prefs.getInt(MIN_PERCENT_VALUE, 0);
        maxPercentValueSettings = prefs.getInt(MAX_PERCENT_VALUE, 100);
        updateFrequencyValue = prefs.getInt(FREQUENCY_VALUE, 4);
        if (prefs.getBoolean(BATTERY_LOW, false)) {
            int maxBatteryPercentValue = prefs.getInt(MainActivity.MAX_BATTERY_PERCENT_VALUE, 100);
            if (maxBatteryPercentValue < maxPercentValueSettings) {
                maxPercentValueSettings = maxBatteryPercentValue;
            }
        }
        if (savedInstanceState != null) {
            lastMagnitudeValue = savedInstanceState.getInt(MAGNITUDE_VALUE, 10);
            locationLatitude = savedInstanceState.getFloat(LATITUDE_VALUE, 0);
            locationLongitude = savedInstanceState.getFloat(LONGITUDE_VALUE, 0);
            locationAltitude = savedInstanceState.getFloat(ALTITUDE_VALUE, 0);
            this.lastTime = savedInstanceState.getLong(RUNTIME_VALUE, 0);
        } else {
            lastMagnitudeValue = prefs.getInt(MAGNITUDE_VALUE, 10);
            locationAltitude = prefs.getFloat(ALTITUDE_VALUE, 0);
            locationLongitude = prefs.getFloat(LONGITUDE_VALUE, 0);
            locationLatitude = prefs.getFloat(LATITUDE_VALUE, 0);
            this.lastTime = prefs.getLong(RUNTIME_VALUE, 0);
        }
        this.generateLocationString();
        magnitude_seek.setMax(40);
        magnitude_seek.setProgress(lastMagnitudeValue);
        magnitude_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                lastMagnitudeValue = position;
                updateShowedValues();
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

        this.recreatePreview();
        // update values
        this.updateShowedValues();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            // Implement a listener to receive updates
            lightsSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    lastLightSensorValue = event.values[0] * getSensorMagnitude();
                    updateShowedValues();
                    updateBrightness();
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };
        } else {
            lightsSensorListener = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Date current = new Date();
        long newTimeValue = this.lastTime + (current.getTime() - this.startTime.getTime()) / 1000;
        super.onSaveInstanceState(outState);
        outState.putInt(MAGNITUDE_VALUE, this.lastMagnitudeValue);
        outState.putLong(RUNTIME_VALUE, newTimeValue);
        outState.putFloat(ALTITUDE_VALUE, locationAltitude);
        outState.putFloat(LATITUDE_VALUE, locationLatitude);
        outState.putFloat(LONGITUDE_VALUE, locationLongitude);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
    }

    private void openCamera() {
        if (!dontUseCamera) {
            this.initCamera(useMonoPreview);
        }

        if (camera != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    camera.setPreviewTexture(previewTexture.getSurfaceTexture());
                } else {
                    camera.setPreviewDisplay(previewSurface.getHolder());
                }
                camera.setPreviewCallback(this);

                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                float aspect = (float) previewSize.width / previewSize.height;

                int previewSurfaceWidth = 0;
                int previewSurfaceHeight = 0;
                LayoutParams lp;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    previewSurfaceWidth = previewTexture.getWidth();
                    previewSurfaceHeight = previewTexture.getHeight();
                    lp = previewTexture.getLayoutParams();
                } else {
                    previewSurfaceWidth = previewSurface.getWidth();
                    previewSurfaceHeight = previewSurface.getHeight();
                    lp = previewSurface.getLayoutParams();
                }

                // fix orientation
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    // portrait
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                        try {
                            camera.setDisplayOrientation(90);
                        } catch (RuntimeException e) {
                            Log.e(EVENTS_NAME, "Cannot set display orientation:" + e.toString());
                        }
                    }
                    lp.height = previewSurfaceHeight;
                    lp.width = (int) (previewSurfaceHeight / aspect);

                } else {
                    // landscape
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                        try {
                            camera.setDisplayOrientation(0);
                        } catch (RuntimeException e) {
                            Log.e(EVENTS_NAME, "Cannot set display orientation:" + e.toString());
                        }
                    }
                    lp.width = previewSurfaceWidth;
                    lp.height = (int) (previewSurfaceWidth / aspect);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    previewTexture.setLayoutParams(lp);
                } else {
                    previewSurface.setLayoutParams(lp);
                }
                try {
                    camera.startPreview();
                } catch (RuntimeException e) {
                    Log.e(EVENTS_NAME, "Can not start preview:" + e.toString());
                }
            } catch (IOException e) {
                Log.e(EVENTS_NAME, "Can not configure preview:" + e.toString());
            }
        }
    }

    private float getMagnitude() {
        return (((float) lastMagnitudeValue + 10) / 20);
    }

    private float getSensorMagnitude() {
        return (((float) lastMagnitudeSensorValue + 10) / 20);
    }

    private void updateBrightness() {
        float cameraLightValue = lastCameraSensorValue * getMagnitude()
                + (minPercentValueSettings * 256 / 100);
        float sensorLightValue = lastLightSensorValue / SensorManager.LIGHT_OVERCAST * 256;
        if (!usedLightSensor) {
            // we don't have such sensor so use value from camera
            sensorLightValue = cameraLightValue;
        }

        // create something in the middle
        int newBrightness = Math.min((int) ((sensorLightValue + cameraLightValue + lastBrightnessValue) / 3), maxPercentValueSettings * 256 / 100);
        if (newBrightness > 255) {
            newBrightness = 255;
        }

        // check difference, but don't do any changes if values similar
        if (Math.abs(cameraLightValue - newBrightness) > 5) {
            lastBrightnessValue = newBrightness;
        }
        if (!cannotChangeBrightness) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, lastBrightnessValue);
            } catch (Exception e) {
                //Throw an error case it couldn't be retrieved
                Log.e(EVENTS_NAME, "Cannot access system brightness:" + e.toString());
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            int step_skip_dots = 1 << updateFrequencyValue;
            long currMillis = System.currentTimeMillis() >> (4 + updateFrequencyValue);
            if (lastUpdateTimeMillisecondsStamp == currMillis)
                return;
            lastUpdateTimeMillisecondsStamp = currMillis;
            lastCameraSensorValue = getMiddleIntense(data, cameraPreviewSize.width,
                    cameraPreviewSize.height, (int) (lastUpdateTimeMillisecondsStamp % step_skip_dots), step_skip_dots);
            this.updateBrightness();
            this.updateShowedValues();
        } catch (Exception e) {
            Log.e(EVENTS_NAME, "Issue with camera preview:" + e.toString());
            Camera.Parameters params = camera.getParameters();
            cameraPreviewSize = params.getPreviewSize();
        }
    }
}
