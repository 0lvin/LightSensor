package denis.develops.utils.lightsensor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;


public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    final String MAGNITUDE_VALUE = "MagnitudeValue";
    private ContentResolver cResolver;
    private int brightness = 0;
    private int magnitude = 10;
    private ProgressBar bar;
    private SeekBar magnitude_seek;
    private Camera camera = null;
    private SurfaceView preview;
    private SurfaceHolder surfaceHolder;
    private TextView textAuthor;
    private TextView textLightSensor;
    private boolean usedFront = false;
    private TextView textCameraLight;
    private TextView textMagnitude;

    public static float getMiddleIntense(byte[] data, int width, int height) {
        long sum = 0;
        int size = width * height;
        for (int i = 0; i < size; i++) {
            sum += data[i] & 0xFF;
        }
        return sum / size;
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

    /*
        returned camera, front camera proffered
     */
    private int getCameraId() {
        CameraInfo info = new CameraInfo();
        //search front camera
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                usedFront = true;
                return i;
            }
        }
        // looks as front not exist
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                usedFront = false;
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        camera = null;
    }

    private void initLightSensor() {
        // Obtain references to the SensorManager and the Light Sensor
        final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (sensors.size() == 0) {
            textLightSensor.setText("Light by sensor: no sensors");
        } else {
            final Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            // Implement a listener to receive updates
            SensorEventListener listener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    int lightQuantity = (int) event.values[0];
                    textLightSensor.setText("Light by sensor: " + Integer.toString(lightQuantity));
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

        this.initLightSensor();
        this.updateBrightnessBar();
        this.easterEggInit();

        if (savedInstanceState != null) {
            magnitude = savedInstanceState.getInt(this.MAGNITUDE_VALUE);
        }

        magnitude_seek.setMax(40);
        magnitude_seek.setProgress(magnitude);

        bar.setMax(256);
        bar.setProgress(this.brightness);

        surfaceHolder = preview.getHolder();
        surfaceHolder.addCallback(this);
    }

    private void updateBrightnessBar() {
        try {
            //Get the current system brightness
            brightness = System.getInt(cResolver, System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
        bar.setProgress(brightness);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(this.MAGNITUDE_VALUE, this.magnitude);
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
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size size = params.getPreviewSize();
        float intense = this.getMiddleIntense(data, size.width, size.height);
        this.updateBrightnessBar();
        textCameraLight.setText("Light by camera: " + Integer.toString((int) (SensorManager.LIGHT_OVERCAST * intense / 255)));
        magnitude = magnitude_seek.getProgress();
        float magnitude_x = (((float) magnitude + 10) / 20);
        float new_intense = intense * magnitude_x;

        textMagnitude.setText(Float.toString(magnitude_x) + "x");
        if (new_intense > 255) {
            new_intense = 255;
        }
        if (Math.abs(new_intense - brightness) > 10) {
            brightness = (int) ((brightness + new_intense) / 2);
            bar.setProgress(brightness);
            System.putInt(getContentResolver(), System.SCREEN_BRIGHTNESS, brightness);
        }
    }
}
