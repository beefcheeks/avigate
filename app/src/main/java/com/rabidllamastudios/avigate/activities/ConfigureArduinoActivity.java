package com.rabidllamastudios.avigate.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.fragments.ReceiverCalibrationFragment;
import com.rabidllamastudios.avigate.fragments.ServoInputFragment;
import com.rabidllamastudios.avigate.fragments.ServoOutputFragment;
import com.rabidllamastudios.avigate.helpers.NonSwipeableViewPager;
import com.rabidllamastudios.avigate.helpers.SharedPreferencesManager;
import com.rabidllamastudios.avigate.helpers.TabFragmentPagerAdapter;
import com.rabidllamastudios.avigate.models.ArduinoPacket;
import com.rabidllamastudios.avigate.models.ConnectionPacket;
import com.rabidllamastudios.avigate.services.CommunicationsService;
import com.rabidllamastudios.avigate.services.UsbSerialService;

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
    private String mCraftProfileName = null;

    private SharedPreferencesManager mSharedPreferencesManager;
    private ArduinoPacket mImportedArduinoPacket = null;
    private ArduinoPacket mMasterArduinoPacket;

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

        loadArduinoConfiguration(getIntent());

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
        mDeviceOutputIntentFilter = new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //If changes have been made to the configuration, prompt the user to save them
                if (!mMasterArduinoPacket.equals(mImportedArduinoPacket)) {
                    //If there are duplicate pins configured, warn the user
                    if (mMasterArduinoPacket.hasDuplicatePins()) {
                        showDuplicatePinsAlertDialog();
                    } else {
                        showSaveChangesAlertDialog();
                    }
                    return true;
                }
            case R.id.item_enable_transmitter:
                //This menu action is handled by ReceiverCalibrationFragment and not this activity
                return false;
            case R.id.item_reset_servos:
                //This menu action is handled by ServoOutputFragment and not this activity
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        //If changes have been made to the configuration, prompt the user to save them
        if (!mMasterArduinoPacket.equals(mImportedArduinoPacket)) {
            if (mMasterArduinoPacket.hasDuplicatePins()) {
                //If there are duplicate pins configured, warn the user
                showDuplicatePinsAlertDialog();
            } else {
                showSaveChangesAlertDialog();
            }
        }
    }

    //Implemented callback methods for ServoOutputFragment.Callback
    private ServoOutputFragment.Callback mServoOutputCallback = new ServoOutputFragment.Callback() {

        @Override
        public void loadOutputConfiguration() {
            //Loads the output configuration for the ServoOutputFragment
            mServoOutputFragment.loadOutputConfiguration(mMasterArduinoPacket);
        }

        @Override
        public void setServoOutputPin(ArduinoPacket.ServoType servoType, int pinValue) {
            if (mUsbSerialIsReady) {
                //Configures the servo output pin value for a given ServoType
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setOutputPin(servoType, pinValue);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            }
            mMasterArduinoPacket.setOutputPin(servoType, pinValue);
        }

        @Override
        public void setServoOutputRange(ArduinoPacket.ServoType servoType, int outputMin,
                                        int outputMax) {
            if (mUsbSerialIsReady) {
                //Sets the servo output range for a given ServoType
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setOutputRange(servoType, outputMin, outputMax);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            }
            mMasterArduinoPacket.setOutputRange(servoType, outputMin, outputMax);
        }

        @Override
        public void setServoValue(ArduinoPacket.ServoType servoType, int servoValue) {
            if (mUsbSerialIsReady) {
                //Sets the servo output value for a given ServoType
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setServoValue(servoType, servoValue);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            }
        }
    };

    //Implemented callback methods for ServoInputFragment.Callback
    private ServoInputFragment.Callback mServoInputCallback = new ServoInputFragment.Callback() {
        @Override
        public void loadInputConfiguration() {
            //Loads the input configuration for the ServoInputFragment
            mServoInputFragment.loadInputConfiguration(mMasterArduinoPacket);
        }

        @Override
        public void setControlType(ArduinoPacket.ServoType servoType, boolean receiverOnly) {
            if (mUsbSerialIsReady) {
                //Sets the control type (e.g. shared or receiver only) for a given ServoType
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setInputControl(servoType, receiverOnly);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            }
            mMasterArduinoPacket.setInputControl(servoType, receiverOnly);
        }

        @Override
        public void setServoInputPin(ArduinoPacket.ServoType servoType, int pinValue) {
            if (mUsbSerialIsReady) {
                //Configures the servo output pin value for a given ServoType
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setInputPin(servoType, pinValue);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            }
            mMasterArduinoPacket.setInputPin(servoType, pinValue);
        }
    };

    //Implemented callback methods for ReceiverCalibrationFragment.Callback
    private ReceiverCalibrationFragment.Callback mReceiverCalibrationCallback =
            new ReceiverCalibrationFragment.Callback() {
        @Override
        public void calibrationButtonPressed(boolean calibrationMode) {
            if (mUsbSerialIsReady) {
                //Sets the calibrationMode accordingly when the calibrate button is pressed,
                ArduinoPacket arduinoPacket = new ArduinoPacket();
                arduinoPacket.setCalibrationMode(calibrationMode);
                sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
            } else {
                mReceiverCalibrationFragment.showNoUsbDeviceCalibrationWarningDialog();
            }
        }

        @Override
        public void loadCalibrationValues() {
            //Loads the calibration values stored in the master ArduinoPacket
            mReceiverCalibrationFragment.loadCalibrationConfig(mMasterArduinoPacket);
        }

        @Override
        public void transmitterIconPressed(boolean enableTransmitter) {
            //Different warning dialogs are displayed depending on the configuration / Arduino state
            if (mMasterArduinoPacket.hasInputRanges()) {
                if (mUsbSerialIsReady) {
                    //If the transmitter is to be enabled, send the appropriate ArduinoPacket
                    if (enableTransmitter) {
                        //Use getInputRangesJson method to get the minimum necessary ArduinoPacket
                        //The maximum Arduino input stream size is 255 characters
                        sendReceiverInputRange(ArduinoPacket.ServoType.AILERON);
                        sendReceiverInputRange(ArduinoPacket.ServoType.ELEVATOR);
                        sendReceiverInputRange(ArduinoPacket.ServoType.RUDDER);
                        sendReceiverInputRange(ArduinoPacket.ServoType.THROTTLE);
                        sendReceiverInputRange(ArduinoPacket.ServoType.CUTOVER);
                    } else {
                        mReceiverCalibrationFragment.showTransmitterWarningDialog();
                    }
                } else {
                    mReceiverCalibrationFragment.showNoUsbDeviceTransmitterWarningDialog();
                }
            } else {
                mReceiverCalibrationFragment.showNotCalibratedWarningDialog();
            }
        }
    };

    //Detects when a connection has been made via a ConnectionPacket Intent
    private final BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
                //If connected, query the status of the Arduino
                if (new ConnectionPacket(intent.getExtras()).isConnected()) {
                    ArduinoPacket statusRequestArduinoPacket = new ArduinoPacket();
                    statusRequestArduinoPacket.addStatusRequest();
                    sendBroadcast(
                            statusRequestArduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
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
            if (intent.getAction().equals(ArduinoPacket.INTENT_ACTION_OUTPUT)) {
                ArduinoPacket arduinoPacket = new ArduinoPacket(intent.getExtras());

                //Ready the status TextView for use in if statement methods below
                TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);

                //If the device sent out a ready status
                if (arduinoPacket.isStatusReady()) {
                    if (!mUsbSerialIsReady) mUsbSerialIsReady = true;
                    statusTV.setText(getString(R.string.tv_arduino_value_ready));
                    //Send the config for each servo (minus receiver input and max) to the Arduino
                    sendServoConfig(ArduinoPacket.ServoType.AILERON);
                    sendServoConfig(ArduinoPacket.ServoType.ELEVATOR);
                    sendServoConfig(ArduinoPacket.ServoType.RUDDER);
                    sendServoConfig(ArduinoPacket.ServoType.THROTTLE);
                    sendServoConfig(ArduinoPacket.ServoType.CUTOVER);
                }

                //If the ArduinoPacket contains the receiverControl json key, set mReceiverOnly
                if (arduinoPacket.hasReceiverControl()) {
                    String receiverControl = "Receiver control: "
                            + String.valueOf(arduinoPacket.isReceiverControl());
                    statusTV.setText(receiverControl);
                }

                //If the ArduinoPacket contains calibrationMode, set text and buttons accordingly
                if (arduinoPacket.hasCalibrationMode()) {
                    //If the arduino is in calibration mode, inform the user
                    if (arduinoPacket.isCalibrationMode()) {
                        statusTV.setText(getString(R.string.tv_arduino_value_calibrating));
                        mReceiverCalibrationFragment.calibrationStarted();

                    //If the arduino is no longer in calibration mode, inform the user
                    } else {
                        mReceiverCalibrationFragment.calibrationStopped(false);
                    }
                }

                //If the ArduinoPacket contains receiver input calibration ranges, show them to user
                if (arduinoPacket.hasInputRanges()) {
                    statusTV.setText(getString(R.string.tv_arduino_value_calibration_success));
                    showCalibrationRange(ArduinoPacket.ServoType.AILERON, arduinoPacket);
                    showCalibrationRange(ArduinoPacket.ServoType.CUTOVER, arduinoPacket);
                    showCalibrationRange(ArduinoPacket.ServoType.ELEVATOR, arduinoPacket);
                    showCalibrationRange(ArduinoPacket.ServoType.RUDDER, arduinoPacket);
                    showCalibrationRange(ArduinoPacket.ServoType.THROTTLE, arduinoPacket);
                    mReceiverCalibrationFragment.calibrationStopped(true);
                }

                //If the ArduinoPacket contains an error message, display the message to the user
                if (arduinoPacket.hasErrorMessage()) {
                    String output = "Error: " + arduinoPacket.getErrorMessage();
                    statusTV.setText(output);
                }
            }
        }
    };

    //Loads the Arduino configuration into a ArduinoPacket from SharedPreferences
    private void loadArduinoConfiguration(Intent intent) {
        mMasterArduinoPacket = new ArduinoPacket();
        mCraftProfileName = intent.getStringExtra(SharedPreferencesManager.KEY_CRAFT_NAME);
        mSharedPreferencesManager = new SharedPreferencesManager(this);
        //If the craft profile name is not null, load the craft configuration
        if (mCraftProfileName != null) {
            String config = mSharedPreferencesManager.getCraftConfiguration(
                    mCraftProfileName);
            //If the craft configuration is not null, save the configuration into ServoPackets
            if (config != null) {
                //Save the
                mImportedArduinoPacket = new ArduinoPacket(config);
                mMasterArduinoPacket = new ArduinoPacket(config);
            }
        }
    }

    //Configure and start the communications service.
    private void startCommunicationsService() {
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(ArduinoPacket.INTENT_ACTION_INPUT);
        remoteSubs.add(ArduinoPacket.INTENT_ACTION_OUTPUT);
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

    //Prompts the user to save any changes made to the Arduino configuration for this craft profile
    private void showDuplicatePinsAlertDialog() {
        if (mCraftProfileName != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Warning - Duplicate Pins!");
            alertDialogBuilder.setMessage(
                    "Duplicate pins detected. If not intended, this may result in" +
                            " an UNCONTROLLABLE CRAFT! " +
                            "Would you like to go back and correct the duplicate pins?");
            alertDialogBuilder.setNegativeButton("Proceed", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showSaveChangesAlertDialog();
                }
            });
            alertDialogBuilder.setPositiveButton("Go back", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            NavUtils.navigateUpFromSameTask(ConfigureArduinoActivity.this);
        }
    }

    //Prompts the user to save any changes made to the Arduino configuration for this craft profile
    private void showSaveChangesAlertDialog() {
        if (mCraftProfileName != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Save changes?");
            alertDialogBuilder.setMessage(
                    "Would you like to save changes made to this configuration?");
            alertDialogBuilder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    NavUtils.navigateUpFromSameTask(ConfigureArduinoActivity.this);
                }
            });
            alertDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSharedPreferencesManager.updateCraftConfiguration(mCraftProfileName,
                            mMasterArduinoPacket.toJsonString());
                    NavUtils.navigateUpFromSameTask(ConfigureArduinoActivity.this);
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            NavUtils.navigateUpFromSameTask(ConfigureArduinoActivity.this);
        }
    }

    //Sends the receiver input min and max for a given ServoType
    private void sendReceiverInputRange(ArduinoPacket.ServoType servoType) {
        String servoInputRangeJson = mMasterArduinoPacket.getInputRangeJson(servoType);
        if (servoInputRangeJson != null) {
            ArduinoPacket arduinoPacket = new ArduinoPacket(servoInputRangeJson);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
    }

    //Sends the configuration (minus the receiver input min and max) for a given ServoType
    private void sendServoConfig(ArduinoPacket.ServoType servoType) {
        String servoConfigJson = mMasterArduinoPacket.getConfigJson(servoType);
        if (servoConfigJson != null) {
            ArduinoPacket arduinoPacket = new ArduinoPacket(servoConfigJson);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
    }

    //Sets the ReceiverCalibrationFragment to display the incoming receiver input calibration ranges
    private void showCalibrationRange(ArduinoPacket.ServoType servoType,
                                      ArduinoPacket arduinoPacket) {
        mReceiverCalibrationFragment.showCalibrationRange(servoType,
                arduinoPacket.getInputMin(servoType), arduinoPacket.getInputMax(servoType));
        mMasterArduinoPacket.setInputRange(servoType, arduinoPacket.getInputMin(servoType),
                arduinoPacket.getInputMax(servoType));
    }

}