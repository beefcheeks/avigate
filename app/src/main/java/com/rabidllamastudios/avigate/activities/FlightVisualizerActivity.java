package com.rabidllamastudios.avigate.activities;

import android.app.ActionBar;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.FlightRenderer;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

/**
 * Displays a virtual 3D image of a craft that changes orientation based on the device's orientation
 * Created by Ryan Staatz on 11/6/2015
 */
public class FlightVisualizerActivity extends AppCompatActivity implements SensorEventListener {

    FlightRenderer mFlightRenderer;
    private SensorManager mSensorManager;
    private Sensor mOrientation;
    private Quaternion mPhoneOrientationQuaternion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_visualizer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Create 3d scene using rajawali 3d library
        final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        //Create new flight renderer to handle aircraft and camera orientations
        mFlightRenderer = new FlightRenderer(this);
        surface.setSurfaceRenderer(mFlightRenderer);

        //Initialize sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Initialize phone orientation quaternion
        mPhoneOrientationQuaternion = new Quaternion();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            //when orientation updates, set phone quaternion equal to latest values
            mPhoneOrientationQuaternion.setAll(event.values[3], event.values[0], event.values[1], event.values[2]);
            //update FlightRenderer with latest orientation quaternion values
            mFlightRenderer.setAircraftOrientationQuaternion(mPhoneOrientationQuaternion);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME);
    }
}
