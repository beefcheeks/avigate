package com.rabidllamastudios.avigate.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.PermissionsChecker;
import com.rabidllamastudios.avigate.models.ConnectionPacket;
import com.rabidllamastudios.avigate.models.CraftStatePacket;
import com.rabidllamastudios.avigate.models.ArduinoPacket;
import com.rabidllamastudios.avigate.services.NetworkService;
import com.rabidllamastudios.avigate.services.FlightControlService;
import com.rabidllamastudios.avigate.services.SensorService;
import com.rabidllamastudios.avigate.services.UsbSerialService;

import java.util.ArrayList;
import java.util.List;

public class CraftActivity extends AppCompatActivity {

    private static final String CLASS_NAME = CraftActivity.class.getSimpleName();
    private static final String DEGREES = " °";
    private static final String DEGREES_PER_SECOND = " °/s";
    //Sensor broadcast rate in milliseconds (ms)
    private static final int SENSOR_BROADCAST_RATE = 100;

    private Intent mNetworkService = null;
    private Intent mFlightControlService = null;
    private Intent mSensorService = null;
    private Intent mUsbSerialService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Register broadcast receiver for any ConnectionPacket Intents
        IntentFilter connectionIntentFilter = new IntentFilter(ConnectionPacket.INTENT_ACTION);
        registerReceiver(mConnectionReceiver, connectionIntentFilter);

        //Register broadcast receiver for any CraftStatePacket (sensor-related) Intents
        registerReceiver(mCraftStateReceiver, new IntentFilter(CraftStatePacket.INTENT_ACTION));

        //Register broadcast receiver for USB-related Intents
        IntentFilter usbIntentFilter = new IntentFilter();
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_READY);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_NO_USB);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        usbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, usbIntentFilter);

        //Initialize mServoOutputFilter IntentFilter and register associated BroadcastReceiver
        IntentFilter arduinoOutputIntentFilter =
                new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
        registerReceiver(mArduinoOutputReceiver, arduinoOutputIntentFilter);

        //Configure and start the UsbSerialService
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this);
        startService(mUsbSerialService);

        //Configure the SensorService Intent
        mSensorService = SensorService.getConfiguredIntent(this, SENSOR_BROADCAST_RATE);

        //Configure the start service Intent
        IntentFilter startServiceIntentFilter = new IntentFilter(
                FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        registerReceiver(mStartServiceReceiver, startServiceIntentFilter);

        //Check for location permissions before starting the sensor service
        PermissionsChecker permissionsChecker =
                new PermissionsChecker(this, mPermissionsCheckerCallback);
        if (permissionsChecker.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE)) {
            startService(mSensorService);
        }

        //Configure and start NetworkService.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(CraftStatePacket.INTENT_ACTION);
        localSubs.add(ArduinoPacket.INTENT_ACTION_OUTPUT);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_READY);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_NO_USB);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        remoteSubs.add(FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        remoteSubs.add(ArduinoPacket.INTENT_ACTION_INPUT);
        mNetworkService = NetworkService.getConfiguredIntent(this, localSubs, remoteSubs,
                NetworkService.DeviceType.CRAFT);
        startService(mNetworkService);
    }

    // If the user allows location permissions, start SensorService
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
        //Unregister all receivers
        unregisterReceiver(mArduinoOutputReceiver);
        unregisterReceiver(mConnectionReceiver);
        unregisterReceiver(mCraftStateReceiver);
        unregisterReceiver(mStartServiceReceiver);
        unregisterReceiver(mUsbReceiver);
        //Stop all services (if running)
        if (mNetworkService != null) stopService(mNetworkService);
        if (mFlightControlService != null) stopService(mFlightControlService);
        if (mSensorService != null) stopService(mSensorService);
        if (mUsbSerialService != null) stopService(mUsbSerialService);
        //Call super method
        super.onDestroy();
    }

    //Listens for responses from the connected USB serial device and updates the output TextView
    private final BroadcastReceiver mArduinoOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ArduinoPacket.INTENT_ACTION_OUTPUT)) {
                ArduinoPacket arduinoPacket = new ArduinoPacket(intent.getExtras());
                TextView outputTV = (TextView) findViewById(R.id.tv_craft_value_arduino_output);
                //Updates outputTV if the incoming JSON contains a ready status
                if (arduinoPacket.isStatusReady()) {
                    outputTV.setText(getString(R.string.tv_arduino_value_ready));
                }
                //Updates outputTV if the incoming JSON contains calibration mode data
                if (arduinoPacket.hasCalibrationMode()) {
                    String calibration = "Calibration mode: "
                            + String.valueOf(arduinoPacket.isCalibrationMode());
                    outputTV.setText(calibration);
                }
                //Updates outputTV if the incoming JSON contains receiver control data
                if (arduinoPacket.hasReceiverControl()) {
                    String receiverControl = "Receiver control: "
                            + String.valueOf(arduinoPacket.isReceiverControl());
                    outputTV.setText(receiverControl);
                }
                //Updates outputTV if the incoming JSON contains receiver control data
                if (arduinoPacket.hasInputRanges()) {
                    outputTV.setText(arduinoPacket.toJsonString());
                }
                //Updates outputTV if the incoming JSON contains an error message
                if (arduinoPacket.hasErrorMessage()) {
                    String error = "Error: " + arduinoPacket.getErrorMessage();
                    outputTV.setText(error);
                }
            }
        }
    };

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the Intent is type ConnectionPacket, update corresponding TextView value
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                TextView connectionStatusTV = (TextView) findViewById(R.id.tv_craft_value_connect);
                ConnectionPacket connectionPacket = new ConnectionPacket(intent.getExtras());
                if (connectionPacket.isConnected()) {
                    connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_connected));
                    //Request the status of the Arduino after a connection is established
                    ArduinoPacket arduinoPacket = new ArduinoPacket();
                    arduinoPacket.addStatusRequest();
                    sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
                } else {
                    connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_disconnected));
                }
            }
        }
    };

    //Listens for CraftStatePacket Intents and updates various TextViews accordingly
    private BroadcastReceiver mCraftStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the Intent is type CraftStatePacket, update corresponding TextView values
            if (intent.getAction().equals(CraftStatePacket.INTENT_ACTION)) {
                CraftStatePacket craftStatePacket = new CraftStatePacket(intent.getExtras());

                //Process orientation data and update corresponding TextViews
                CraftStatePacket.Orientation orientation = craftStatePacket.getOrientation();
                String roll = String.valueOf(orientation.getCraftRoll(false)) + DEGREES;
                String pitch = String.valueOf(orientation.getCraftPitch(false)) + DEGREES;
                String yaw = String.valueOf(orientation.getCraftYaw(false)) + DEGREES;

                TextView rollTV = (TextView) findViewById(R.id.tv_craft_value_roll);
                TextView pitchTV = (TextView) findViewById(R.id.tv_craft_value_pitch);
                TextView yawTV = (TextView) findViewById(R.id.tv_craft_value_yaw);

                rollTV.setText(roll);
                pitchTV.setText(pitch);
                yawTV.setText(yaw);

                //Process angular velocity data and update corresponding TextViews
                CraftStatePacket.AngularVelocity angularVelocity =
                        craftStatePacket.getAngularVelocity();
                String rollRate = String.valueOf(angularVelocity.getCraftRollRate(false))
                        + DEGREES_PER_SECOND;
                String pitchRate = String.valueOf(angularVelocity.getCraftPitchRate(false))
                        + DEGREES_PER_SECOND;
                String yawRate = String.valueOf(angularVelocity.getCraftYawRate(false))
                        + DEGREES_PER_SECOND;

                TextView rollRateTV = (TextView) findViewById(R.id.tv_craft_value_rate_roll);
                TextView pitchRateTV = (TextView) findViewById(R.id.tv_craft_value_rate_pitch);
                TextView yawRateTV = (TextView) findViewById(R.id.tv_craft_value_rate_yaw);

                rollRateTV.setText(rollRate);
                pitchRateTV.setText(pitchRate);
                yawRateTV.setText(yawRate);

                //Process location data and update corresponding TextViews
                Location location = craftStatePacket.getLocation();
                String coordinates = String.valueOf(location.getLatitude()) + " ,"
                        + String.valueOf(location.getLongitude());
                String accuracy = String.valueOf(location.getAccuracy()) + " m";
                String bearing = String.valueOf(location.getBearing()) + DEGREES;

                TextView gpsCoordinatesTV =
                        (TextView) findViewById(R.id.tv_craft_value_gps_coordinates);
                TextView gpsAccuracyTV = (TextView) findViewById(R.id.tv_craft_value_gps_accuracy);
                TextView gpsBearingTV = (TextView) findViewById(R.id.tv_craft_value_bearing);

                gpsCoordinatesTV.setText(coordinates);
                gpsAccuracyTV.setText(accuracy);
                gpsBearingTV.setText(bearing);

                //Process barometric pressure data and update corresponding TextViews
                CraftStatePacket.BarometricPressure barometricPressure =
                        craftStatePacket.getBarometricPressure();
                String altitude = String.valueOf(barometricPressure.getPressure()) + " hPa";
                TextView pressureTV = (TextView) findViewById(R.id.tv_craft_value_barometer);
                pressureTV.setText(altitude);
            }
        }
    };

    //Listens for any Intents that are used to start other services
    private BroadcastReceiver mStartServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE)) {
                Log.i(CLASS_NAME, "FlightControlService start command received");
                intent.setClass(getApplicationContext(), FlightControlService.class);
                mFlightControlService = intent;
                startService(mFlightControlService);
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
