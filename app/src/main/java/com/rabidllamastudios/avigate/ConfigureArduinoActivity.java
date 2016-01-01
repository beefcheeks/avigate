package com.rabidllamastudios.avigate;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.fragments.ReceiverCalibrationFragment;
import com.rabidllamastudios.avigate.fragments.ServoInputFragment;
import com.rabidllamastudios.avigate.fragments.ServoOutputFragment;
import com.rabidllamastudios.avigate.model.ConnectionPacket;
import com.rabidllamastudios.avigate.model.ServoPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan Staatz
 * This activity is intended as a test for USB-OTG connected CDC-ACM devices (e.g. Arduino)
 * Some code adapted from: https://github.com/felHR85/SerialPortExample
 */
public class ConfigureArduinoActivity extends AppCompatActivity {

    private static final int BAUD_RATE = 115200;

    private boolean mUsbSerialIsReady = false;

    private Intent mCommService;
    private Intent mUsbSerialService;
    private IntentFilter mDeviceOutputIntentFilter;
    private IntentFilter mConnectionIntentFilter;
    private IntentFilter mUsbIntentFilter;

    private ReceiverCalibrationFragment mReceiverCalibrationFragment;
    private ServoInputFragment mServoInputFragment;
    private ServoOutputFragment mServoOutputFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_arduino);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Configure the fragments and their callbacks
        mServoOutputFragment = new ServoOutputFragment();
        mServoInputFragment = new ServoInputFragment();
        mReceiverCalibrationFragment = new ReceiverCalibrationFragment();
        mServoOutputFragment.setCallback(mServoOutputCallback);
        mServoInputFragment.setCallback(mServoInputCallback);
        mReceiverCalibrationFragment.setCallback(mReceiverCalibrationCallback);

        //Configure the FragmentPagerAdapter
        TabFragmentPagerAdapter fragmentPagerAdapter =
                new TabFragmentPagerAdapter(getSupportFragmentManager());
        fragmentPagerAdapter.addEntry(0, "Outputs", mServoOutputFragment);
        fragmentPagerAdapter.addEntry(1, "Inputs", mServoInputFragment);
        fragmentPagerAdapter.addEntry(2, "Calibration", mReceiverCalibrationFragment);

        //Configure the ViewPager
        NonSwipeableViewPager viewPager =
                (NonSwipeableViewPager) findViewById(R.id.viewpager_configure_arduino);
        viewPager.setOffscreenPageLimit(fragmentPagerAdapter.getCount());
        viewPager.setAdapter(fragmentPagerAdapter);

        //Set up the TabLayout with the configure ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout_configure_arduino);
        tabLayout.setupWithViewPager(viewPager);

        //Initialize the mConnectionIntentFilter
        mConnectionIntentFilter = new IntentFilter(ConnectionPacket.INTENT_ACTION);

        //Initialize mUsbFilter IntentFilter
        mUsbIntentFilter = new IntentFilter();
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_READY);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_NO_USB);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        mUsbIntentFilter.addAction(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);

        //Initialize mServoOutputFilter IntentFilter
        mDeviceOutputIntentFilter = new IntentFilter(ServoPacket.INTENT_ACTION_OUTPUT);
    }

    @Override
    public void onPause() {
        //Unregister receivers and stop CommunicationsService and UsbSerialService
        mUsbSerialIsReady = false;
        try {
            unregisterReceiver(mConnectionReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            unregisterReceiver(mArduinoOutputReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            unregisterReceiver(mUsbReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopService(mCommService);
        stopService(mUsbSerialService);
        super.onPause();
    }

    @Override
    public void onResume() {
        //Register receivers and start CommunicationsService and UsbSerialService
        registerReceiver(mConnectionReceiver, mConnectionIntentFilter);
        registerReceiver(mUsbReceiver, mUsbIntentFilter);
        registerReceiver(mArduinoOutputReceiver, mDeviceOutputIntentFilter);
        //Get the configured intent to start the UsbSerialService
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this, BAUD_RATE);
        startService(mUsbSerialService);
        //Start the CommunicationsService
        startCommunicationsService();
        super.onResume();
    }

    //Implemented callback methods for ServoOutputFragment.Callback
    private ServoOutputFragment.Callback mServoOutputCallback = new ServoOutputFragment.Callback() {

        @Override
        public void setServoOutputPin(ServoPacket.ServoType servoType, int pinValue) {
            if (mUsbSerialIsReady) {
                //Configures the servo output pin value for a given ServoType
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setOutputPin(servoType, pinValue);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        }

        @Override
        public void setServoOutputRange(ServoPacket.ServoType servoType, int outputMin,
                                        int outputMax) {
            if (mUsbSerialIsReady) {
                //Sets the servo output range for a given ServoType
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setOutputRange(servoType, outputMin, outputMax);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        }

        @Override
        public void setServoValue(ServoPacket.ServoType servoType, int servoValue) {
            if (mUsbSerialIsReady) {
                //Sets the servo output value for a given ServoType
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setServoValue(servoType, servoValue);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        }
    };

    //Implemented callback methods for ServoInputFragment.Callback
    private ServoInputFragment.Callback mServoInputCallback = new ServoInputFragment.Callback() {
        @Override
        public void setControlType(ServoPacket.ServoType servoType, boolean receiverOnly) {
            if (mUsbSerialIsReady) {
                //Sets the control type (e.g. shared or receiver only) for a given ServoType
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setInputControl(servoType, receiverOnly);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        }

        @Override
        public void setServoInputPin(ServoPacket.ServoType servoType, int pinValue) {
            if (mUsbSerialIsReady) {
                //Configures the servo output pin value for a given ServoType
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setInputPin(servoType, pinValue);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        }
    };

    //Implemented callback methods for ReceiverCalibrationFragment.Callback
    private ReceiverCalibrationFragment.Callback mReceiverCalibrationCallback =
            new ReceiverCalibrationFragment.Callback() {
        @Override
        public void calibrationButtonPressed(boolean calibrationMode) {
            if (mUsbSerialIsReady) {
                //Sets the calibrationMode accordingly when the calibrate button is pressed,
                ServoPacket servoPacket = new ServoPacket();
                servoPacket.setCalibrationMode(calibrationMode);
                sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            } else {
                mReceiverCalibrationFragment.showNoUsbDeviceCalibrationWarningDialog();
            }
        }


    private final BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
                //If connected, query the status of the Arduino
                if (new ConnectionPacket(intent.getExtras()).isConnected()) {
                    ServoPacket statusRequestServoPacket = new ServoPacket();
                    statusRequestServoPacket.addStatusRequest();
                    sendBroadcast(
                            statusRequestServoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
                    statusTV.setText(getString(R.string.tv_arduino_value_status_connected));
                } else {
                    //If disconnected, inform the user
                    statusTV.setText(getString(R.string.tv_arduino_value_status_disconnected));
                }
            }
        }
    };

    //Usb state notifications from UsbSerialService are received here.
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
            if (intent.getAction().equals(UsbSerialService.INTENT_ACTION_USB_READY)) {
                statusTV.setText(getString(R.string.tv_usb_value_ready));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_DISCONNECTED)) {
                mUsbSerialIsReady = false;
                statusTV.setText(getString(R.string.tv_usb_value_disconnected));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED)) {
                statusTV.setText(getString(R.string.tv_usb_value_permission_granted));
            } else if (intent.getAction()
                    .equals(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED)) {
                statusTV.setText(getString(R.string.tv_usb_value_permission_not_granted));
            } else if (intent.getAction().equals(UsbSerialService.INTENT_ACTION_NO_USB)) {
                statusTV.setText(getString(R.string.tv_usb_value_not_connected));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED)) {
                statusTV.setText(getString(R.string.tv_usb_value_not_supported));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_CDC_DRIVER_NOT_WORKING)) {
                statusTV.setText(getString(R.string.tv_usb_value_no_cdc_driver));
            } else if (intent.getAction().equals(
                    UsbSerialService.INTENT_ACTION_USB_DEVICE_NOT_WORKING)) {
                statusTV.setText(R.string.tv_usb_value_device_not_working);
            }
        }
    };

    //Listens for responses from the connected USB serial device and updates the output TextView
    private final BroadcastReceiver mArduinoOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServoPacket.INTENT_ACTION_OUTPUT)) {
                ServoPacket servoPacket = new ServoPacket(intent.getExtras());

                //Ready the status TextView for use in if statement methods below
                TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);

                //If the device sent out a ready status
                if (servoPacket.isStatusReady()) {
                    if (!mUsbSerialIsReady) mUsbSerialIsReady = true;
                    statusTV.setText(getString(R.string.tv_arduino_value_ready));
                }

                //If the ServoPacket contains the receiverControl json key, set mReceiverOnly
                if (servoPacket.hasReceiverControl()) {
                    String receiverControl = "Receiver control: "
                            + String.valueOf(servoPacket.isReceiverControl());
                    statusTV.setText(receiverControl);
                }

                //If the ServoPacket contains the calibrationMode, set text and buttons accordingly
                if (servoPacket.hasCalibrationMode()) {
                    //If the arduino is in calibration mode, inform the user
                    if (servoPacket.isCalibrationMode()) {
                        statusTV.setText(getString(R.string.tv_arduino_value_calibrating));
                        mReceiverCalibrationFragment.calibrationStarted();

                    //If the arduino is no longer in calibration mode, inform the user
                    } else {
                        mReceiverCalibrationFragment.calibrationStopped(false);
                    }
                }

                //If the ServoPacket contains receiver input calibration ranges, show them on screen
                if (servoPacket.hasInputRanges()) {
                    statusTV.setText(getString(R.string.tv_arduino_value_calibration_success));
                    showCalibrationRange(ServoPacket.ServoType.AILERON, servoPacket);
                    showCalibrationRange(ServoPacket.ServoType.CUTOVER, servoPacket);
                    showCalibrationRange(ServoPacket.ServoType.ELEVATOR, servoPacket);
                    showCalibrationRange(ServoPacket.ServoType.RUDDER, servoPacket);
                    showCalibrationRange(ServoPacket.ServoType.THROTTLE, servoPacket);
                    mReceiverCalibrationFragment.calibrationStopped(true);
                }

                //If the ServoPacket contains an error message, display the message to the user
                if (servoPacket.hasErrorMessage()) {
                    String output = "Error: " + servoPacket.getErrorMessage();
                    statusTV.setText(output);
                }
            }
        }
    };

    //Configure and start the communications service.
    private void startCommunicationsService() {
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(ServoPacket.INTENT_ACTION_INPUT);
        remoteSubs.add(ServoPacket.INTENT_ACTION_OUTPUT);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_USB_READY);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_NO_USB);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        remoteSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs,
                CommunicationsService.DeviceType.CONTROLLER);
        startService(mCommService);
    }

    //Sets the ReceiverCalibrationFragment to display the incoming receiver input calibration ranges
    private void showCalibrationRange(ServoPacket.ServoType servoType, ServoPacket servoPacket) {
        mReceiverCalibrationFragment.showCalibrationRange(servoType,
                servoPacket.getInputMin(servoType), servoPacket.getInputMax(servoType));
    }

}