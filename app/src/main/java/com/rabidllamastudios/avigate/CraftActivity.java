package com.rabidllamastudios.avigate;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.model.ConnectionPacket;
import com.rabidllamastudios.avigate.model.GPSPacket;
import com.rabidllamastudios.avigate.model.OrientationPacket;
import com.rabidllamastudios.avigate.model.PressurePacket;
import com.rabidllamastudios.avigate.model.ServoPacket;

import java.util.ArrayList;
import java.util.List;

public class CraftActivity extends AppCompatActivity {

    //TODO adjust sensor rate with latency?
    //Sensor update rate in microseconds
    private static final int SENSOR_UPDATE_RATE = SensorManager.SENSOR_DELAY_UI;

    private Intent mCommService;
    private Intent mSensorService;
    private Intent mUsbSerialService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Register broadcast receiver for connection-related intents
        IntentFilter connectionIntentFilter = new IntentFilter(ConnectionPacket.INTENT_ACTION);
        registerReceiver(mConnectionReceiver, connectionIntentFilter);

        //Register broadcast receiver for sensor-related intents
        IntentFilter sensorIntentFilter = new IntentFilter();
        sensorIntentFilter.addAction(OrientationPacket.INTENT_ACTION);
        sensorIntentFilter.addAction(GPSPacket.INTENT_ACTION);
        sensorIntentFilter.addAction(PressurePacket.INTENT_ACTION);
        registerReceiver(mSensorReceiver, sensorIntentFilter);

        //Register broadcast receiver for usb-related intents
        IntentFilter usbIntentFilter = new IntentFilter();
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_READY);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_NO_USB);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, usbIntentFilter);

        //Initialize mServoOutputFilter IntentFilter
        IntentFilter deviceOutputIntentFilter = new IntentFilter(ServoPacket.INTENT_ACTION_OUTPUT);
        registerReceiver(mDeviceOutputReceiver, deviceOutputIntentFilter);

        //Configure and start the UsbSerialService
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this);
        startService(mUsbSerialService);

        //Configure sensor service intent
        mSensorService = SensorService.getConfiguredIntent(this, SENSOR_UPDATE_RATE);

        //Check for location permissions before starting the sensor service
        PermissionsChecker permissionsChecker = new PermissionsChecker(this, mPermissionsCheckerCallback);
        if (permissionsChecker.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION, PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE)) {
            startService(mSensorService);
        }

        //Configure and start the communications service.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(GPSPacket.INTENT_ACTION);
        localSubs.add(OrientationPacket.INTENT_ACTION);
        localSubs.add(PressurePacket.INTENT_ACTION);
        localSubs.add(ServoPacket.INTENT_ACTION_OUTPUT);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_READY);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_NO_USB);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        remoteSubs.add(FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        remoteSubs.add(ServoPacket.INTENT_ACTION_INPUT);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs,
                CommunicationsService.DeviceType.CRAFT);
        startService(mCommService);
    }

    // If the user allows location permissions, start the sensor service
    private PermissionsChecker.Callback mPermissionsCheckerCallback = new PermissionsChecker.Callback() {
        @Override
        public void permissionGranted(int permissionsConstant) {
            if (permissionsConstant == PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE) {
                startService(mSensorService);
            }
        }
    };

    @Override
    public void onDestroy() {
        //Stop all services and unregister all broadcast receivers
        stopService(mCommService);
        stopService(mSensorService);
        stopService(mUsbSerialService);
        unregisterReceiver(mConnectionReceiver);
        unregisterReceiver(mDeviceOutputReceiver);
        unregisterReceiver(mSensorReceiver);
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the intent is type connection packet, update corresponding textview value
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                TextView connectionStatusTV = (TextView) findViewById(R.id.tv_craft_value_connect);
                ConnectionPacket connectionPacket = new ConnectionPacket(intent.getExtras());
                if (connectionPacket.isConnected()) {
                    connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_connected));
                    //Request the status of the Arduino after a connection is established
                    ServoPacket servoPacket = new ServoPacket();
                    servoPacket.addStatusRequest();
                    sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
                } else {
                    connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_disconnected));
                }
            }
        }
    };

    //Listens for responses from the connected USB serial device and updates the output TextView
    private final BroadcastReceiver mDeviceOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServoPacket.INTENT_ACTION_OUTPUT)) {
                ServoPacket servoPacket = new ServoPacket(intent.getExtras());
                TextView outputTV = (TextView) findViewById(R.id.tv_craft_value_arduino_output);

                //Updates outputTV if the incoming JSON contains calibration mode data
                if (servoPacket.hasCalibrationMode()) {
                    String calibration = "Calibration mode: "
                            + String.valueOf(servoPacket.isCalibrationMode());
                    outputTV.setText(calibration);
                }
                //Updates outputTV if the incoming JSON contains receiver control data
                if (servoPacket.hasReceiverControl()) {
                    String receiverControl = "Receiver control: "
                            + String.valueOf(servoPacket.isReceiverControl());
                    outputTV.setText(receiverControl);
                }
                //Updates outputTV if the incoming JSON contains receiver control data
                if (servoPacket.hasInputRanges()) {
                    outputTV.setText(servoPacket.toJsonString());
                }
                //Updates outputTV if the incoming JSON contains an error message
                if (servoPacket.hasErrorMessage()) {
                    String error = "Error: " + servoPacket.getErrorMessage();
                    outputTV.setText(error);
                }
            }
        }
    };

    //Listens for sensor data and updates various TextViews accordingly
    private BroadcastReceiver mSensorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the intent is type orientation packet, update corresponding textview values
            if (intent.getAction().equals(OrientationPacket.INTENT_ACTION)) {
                TextView pitchTV = (TextView) findViewById(R.id.tv_craft_value_pitch);
                TextView yawTV = (TextView) findViewById(R.id.tv_craft_value_yaw);
                TextView rollTV = (TextView) findViewById(R.id.tv_craft_value_roll);
                OrientationPacket orientationPacket = new OrientationPacket(intent.getExtras());
                //yaw and roll switched due to necessary coordinate system transformation
                pitchTV.setText(String.valueOf(orientationPacket.getOrientation().getPitch()));
                yawTV.setText(String.valueOf(orientationPacket.getOrientation().getRoll()));
                rollTV.setText(String.valueOf(orientationPacket.getOrientation().getYaw()));
            //If the intent is type gps packet, update corresponding textview values
            } else if (intent.getAction().equals(GPSPacket.INTENT_ACTION)) {
                TextView gpsCoordinatesTV = (TextView) findViewById(R.id.tv_craft_value_gps_coordinates);
                TextView gpsAccuracyTV = (TextView) findViewById(R.id.tv_craft_value_gps_accuracy);
                TextView gpsBearingTV = (TextView) findViewById(R.id.tv_craft_value_bearing);
                GPSPacket gpsPacket = new GPSPacket(intent.getExtras());
                String coordinates = String.valueOf(gpsPacket.getLatitude()) + " ," + String.valueOf(gpsPacket.getLongitude());
                String accuracy = String.valueOf(gpsPacket.getAccuracy()) + " m";
                gpsCoordinatesTV.setText(coordinates);
                gpsAccuracyTV.setText(accuracy);
                if (gpsPacket.getBearing() == Double.NaN) {
                    gpsBearingTV.setText(getResources().getString(R.string.tv_placeholder_sensor));
                } else {
                    String bearing = String.valueOf(gpsPacket.getBearing()) + " °";
                    gpsBearingTV.setText(bearing);
                }
            //If the intent type is pressure packet, update corresponding textview value
            } else if (intent.getAction().equals(PressurePacket.INTENT_ACTION)) {
                TextView pressureTV = (TextView) findViewById(R.id.tv_craft_value_barometer);
                PressurePacket pressurePacket = new PressurePacket(intent.getExtras());
                String altitude = String.valueOf(pressurePacket.getPressure()) + " hPa";
                pressureTV.setText(altitude);
            }
        }
    };

    //Listens for USB state notifications from UsbSerialService and updates a TextView accordingly
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView usbStatusTV = (TextView) findViewById(R.id.tv_craft_value_arduino_status);
            if (intent.getAction().equals(UsbSerialService.INTENT_ACTION_USB_READY)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_ready));
            } else if (intent.getAction().equals(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_disconnected));
                TextView outputTV = (TextView) findViewById(R.id.tv_craft_value_arduino_output);
                outputTV.setText("");
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_permission_granted));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_permission_not_granted));
            } else if (intent.getAction().equals(UsbSerialService.INTENT_ACTION_NO_USB)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_not_connected));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_not_supported));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_CDC_DRIVER_NOT_WORKING)) {
                usbStatusTV.setText(getString(R.string.tv_usb_value_no_cdc_driver));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_DEVICE_NOT_WORKING)) {
                usbStatusTV.setText(R.string.tv_usb_value_device_not_working);
            }
        }
    };

}
