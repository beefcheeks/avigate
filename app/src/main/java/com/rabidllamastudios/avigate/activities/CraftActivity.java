package com.rabidllamastudios.avigate.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.PermissionsChecker;
import com.rabidllamastudios.avigate.models.ConnectionPacket;
import com.rabidllamastudios.avigate.models.CraftStatePacket;
import com.rabidllamastudios.avigate.models.ArduinoPacket;
import com.rabidllamastudios.avigate.services.MasterFlightService;
import com.rabidllamastudios.avigate.services.NetworkService;
import com.rabidllamastudios.avigate.services.UsbSerialService;


public class CraftActivity extends AppCompatActivity {

    private static final String DEGREES = " °";
    private static final String DEGREES_PER_SECOND = " °/s";

    private IntentFilter mArduinoOutputIntentFilter;
    private IntentFilter mConnectionIntentFilter;
    private IntentFilter mCraftStateIntentFilter;
    private IntentFilter mUsbIntentFilter;
    private PermissionsChecker mPermissionsChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initialize IntentFilters for registering BroadcastReceivers in onResume() method
        mArduinoOutputIntentFilter = new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
        mConnectionIntentFilter = new IntentFilter(ConnectionPacket.INTENT_ACTION);
        mCraftStateIntentFilter = new IntentFilter(CraftStatePacket.INTENT_ACTION);

        //Initialize and add multiple Intent actions for mUsbIntentFilter
        mUsbIntentFilter = new IntentFilter();
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_READY);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_NO_USB);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);

        //Initialize mPermissionsChecker
        mPermissionsChecker = new PermissionsChecker(mPermissionsCheckerCallback);
    }

    @Override
    public void onPause() {
        //Unregister all receivers
        unregisterReceiver(mArduinoOutputReceiver);
        unregisterReceiver(mConnectionReceiver);
        unregisterReceiver(mCraftStateReceiver);
        unregisterReceiver(mUsbReceiver);
        //Call super method
        super.onPause();
    }

    @Override
    public void onResume() {
        //Register all receiver methods
        registerReceiver(mArduinoOutputReceiver, mArduinoOutputIntentFilter);
        registerReceiver(mConnectionReceiver, mConnectionIntentFilter);
        registerReceiver(mCraftStateReceiver, mCraftStateIntentFilter);
        registerReceiver(mUsbReceiver, mUsbIntentFilter);
        //Check for location permissions before attempting to start the MasterFlightService
        if (mPermissionsChecker.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION,
                PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE)) {
            startMasterFlightService();
        }
        //Request the connection status from NetworkService
        sendBroadcast(new Intent(NetworkService.INTENT_ACTION_REQUEST_CONNECTION_STATUS));
        //Call super method
        super.onResume();
    }

    // If the user allows location permissions, start the MasterFlightService
    private PermissionsChecker.Callback mPermissionsCheckerCallback =
            new PermissionsChecker.Callback() {
                @Override
                public void permissionGranted(int permissionsConstant) {
                    if (permissionsConstant ==
                            PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE) {
                        startMasterFlightService();
                    }
                }
            };

    //Starts the MasterFlightService if it is not already running
    private void startMasterFlightService() {
        if (!isServiceRunning(MasterFlightService.class)) {
            startService(MasterFlightService.getConfiguredIntent(getApplicationContext()));
        }
    }

    //Checks if a given service class is running
    //Taken from: http://stackoverflow.com/a/5921190
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

    //Listens for ConnectionPacket Intents and updates various TextViews accordingly
    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the Intent is type ConnectionPacket, update corresponding TextView value
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                TextView connectionStatusTV = (TextView) findViewById(R.id.tv_craft_value_connect);
                //If ConnectionPacket method isConnected is true, show the network is connected
                if (new ConnectionPacket(intent.getExtras()).isConnected()) {
                    connectionStatusTV.setText(
                            getResources().getString(R.string.tv_placeholder_connected));
                    //TODO get Arduino status without notifying NetworkService of status response
                    //Request the status of the Arduino after reconnection
                    ArduinoPacket arduinoPacket = new ArduinoPacket();
                    arduinoPacket.addStatusRequest();
                    sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
                //If ConnectionPacket method isConnected is false, show the network is disconnected
                } else {
                    connectionStatusTV.setText(
                            getResources().getString(R.string.tv_placeholder_disconnected));
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
                String pressure = String.valueOf(barometricPressure.getPressure()) + " hPa";
                TextView pressureTV = (TextView) findViewById(R.id.tv_craft_value_barometer);
                pressureTV.setText(pressure);
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
