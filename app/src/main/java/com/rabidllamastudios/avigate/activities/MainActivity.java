package com.rabidllamastudios.avigate.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.rabidllamastudios.avigate.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user clicks the Controller Module button */
    public void startControllerModule(View view) {
        Intent intent = new Intent(this, SelectCraftActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Craft Module button */
    public void startCraftModule(View view) {
        Intent intent = new Intent(this, CraftActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Sensor Reading button */
    public void startSensorReadings(View view) {
        Intent intent = new Intent(this, DisplaySensorActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Connectivity Test button */
    public void startConnectivityTest(View view) {
        Intent intent = new Intent(this, ConnectivityTestActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Flight Visualizer button */
    public void startFlightVisualizer(View view) {
        Intent intent = new Intent(this, FlightVisualizerActivity.class);
        startActivity(intent);
    }

}