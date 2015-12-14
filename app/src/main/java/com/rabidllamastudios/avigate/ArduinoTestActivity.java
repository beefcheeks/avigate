package com.rabidllamastudios.avigate;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.model.ServoPacket;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan Staatz
 * This activity is intended as a test for USB-OTG connected CDC-ACM devices (e.g. Arduino)
 * Some code adapted from: https://github.com/felHR85/SerialPortExample
 */

public class ArduinoTestActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {

    private static final int BAUD_RATE = 115200;
    private static final int PIN_MAX = 12;
    private static final int PIN_MIN = 0;
    private static final int SERVO_MAX = 180;
    private static final int SERVO_MIN = 0;

    private boolean mUsbSerialIsReady = false;

    private int aileronMax = SERVO_MAX;
    private int aileronMin = SERVO_MIN;
    private int elevatorMax = SERVO_MAX;
    private int elevatorMin = SERVO_MIN;
    private int rudderMax = SERVO_MAX;
    private int rudderMin = SERVO_MIN;
    private int throttleMax = SERVO_MAX;
    private int throttleMin = SERVO_MIN;

    private Intent mCommService;
    private Intent mUsbSerialService;
    private IntentFilter mUsbIntentFilter;
    private IntentFilter mDeviceOutputIntentFilter;
    private RangeSeekBar<Integer> mRangeSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initialize mUsbFilter IntentFilter
        mUsbIntentFilter = new IntentFilter();
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_USB_READY);
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_USB_PERMISSION_GRANTED);
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_NO_USB);
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_USB_DISCONNECTED);
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_USB_NOT_SUPPORTED);
        mUsbIntentFilter.addAction(UsbSerialService.ACTION_USB_PERMISSION_NOT_GRANTED);

        //Initialize mServoOutputFilter IntentFilter
        mDeviceOutputIntentFilter = new IntentFilter(ServoPacket.INTENT_ACTION_OUTPUT);

        //Start the communications service.
        startCommunicationsService();

        //Initialize mRangeSeekBar
        mRangeSeekBar = (RangeSeekBar) findViewById(R.id.seekbar_range);

        //Initialize UI
        int servoNeutral = (SERVO_MAX - SERVO_MIN)/2;
        configureSeekbar(ServoPacket.ServoType.AILERON, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.ELEVATOR, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.RUDDER, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.THROTTLE, SERVO_MIN, SERVO_MIN, SERVO_MAX);
        configurePinEditText(ServoPacket.ServoType.AILERON);
        configurePinEditText(ServoPacket.ServoType.ELEVATOR);
        configurePinEditText(ServoPacket.ServoType.RUDDER);
        configurePinEditText(ServoPacket.ServoType.THROTTLE);
        configureServoEditText(ServoPacket.ServoType.AILERON);
        configureServoEditText(ServoPacket.ServoType.ELEVATOR);
        configureServoEditText(ServoPacket.ServoType.RUDDER);
        configureServoEditText(ServoPacket.ServoType.THROTTLE);
        configureEmergencyResetButton();
    }

    @Override
    public void onPause() {
        //Unregister receivers and stop UsbSerialService
        mUsbSerialIsReady = false;
        try {
            unregisterReceiver(mDeviceOutputReceiver);
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
        registerReceiver(mUsbReceiver, mUsbIntentFilter);  //Start listening for USB notifications from Android system
        registerReceiver(mDeviceOutputReceiver, mDeviceOutputIntentFilter);  //Start listening for servo output intents
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this, BAUD_RATE);  //Get the configured intent to start the UsbSerialService
        startService(mUsbSerialService); //Start the UsbSerialService
        startCommunicationsService();
        super.onResume();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {}


    private void startCommunicationsService() {
        //Configure and start the communications service.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(ServoPacket.INTENT_ACTION_INPUT);
        remoteSubs.add(ServoPacket.INTENT_ACTION_OUTPUT);
        remoteSubs.add(UsbSerialService.ACTION_USB_READY);
        remoteSubs.add(UsbSerialService.ACTION_USB_PERMISSION_GRANTED);
        remoteSubs.add(UsbSerialService.ACTION_NO_USB);
        remoteSubs.add(UsbSerialService.ACTION_USB_DISCONNECTED);
        remoteSubs.add(UsbSerialService.ACTION_USB_NOT_SUPPORTED);
        remoteSubs.add(UsbSerialService.ACTION_USB_PERMISSION_NOT_GRANTED);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs, CommunicationsService.DeviceType.CONTROLLER);
        startService(mCommService);
    }
    //Emergency reset button resets all sliders back to defaults
    private void configureEmergencyResetButton() {
        Button emergencyResetButton = (Button) findViewById(R.id.button_emergency_reset);
        //Resets all sliders to defaults - servos to neutral position, throttle to minimum position
        emergencyResetButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar aileronSeekBar = (SeekBar) findViewById(R.id.seekbar_aileron);
                SeekBar elevatorSeekBar = (SeekBar) findViewById(R.id.seekbar_elevator);
                SeekBar rudderSeekBar = (SeekBar) findViewById(R.id.seekbar_rudder);
                SeekBar throttleSeekBar = (SeekBar) findViewById(R.id.seekbar_throttle);

                aileronSeekBar.setProgress((aileronMax-aileronMin)/2);
                elevatorSeekBar.setProgress((elevatorMax-elevatorMin)/2);
                rudderSeekBar.setProgress((rudderMax-rudderMin)/2);
                throttleSeekBar.setProgress(SERVO_MIN);

                //Request the status of the microcontroller
                ServoPacket statusRequestServoPacket = new ServoPacket();
                statusRequestServoPacket.addStatusRequest();
                sendBroadcast(statusRequestServoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
            }
        });
        //Resets range for all servos to full range, then resets all sliders to defaults
        emergencyResetButton.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int servoNeutral = (SERVO_MAX - SERVO_MIN) / 2;
                configureSeekbar(ServoPacket.ServoType.AILERON, servoNeutral, SERVO_MIN, SERVO_MAX);
                configureSeekbar(ServoPacket.ServoType.ELEVATOR, servoNeutral, SERVO_MIN, SERVO_MAX);
                configureSeekbar(ServoPacket.ServoType.RUDDER, servoNeutral, SERVO_MIN, SERVO_MAX);
                configureSeekbar(ServoPacket.ServoType.THROTTLE, SERVO_MIN, SERVO_MIN, SERVO_MAX);
                return true;
            }
        });
    }

    //Configures the SeekBar and OnSeekBarChangeListener
    private void configureSeekbar(final ServoPacket.ServoType servoType, int startValue, final int minValue, final int maxValue) {
        final EditText editText = getServoET(servoType);
        SeekBar seekBar = getSeekBar(servoType);
        if (editText != null && seekBar != null) {
            seekBar.setMax(maxValue - minValue);
            //Set editText here since onProgressChanged may not be called on setProgress
            editText.setText(String.valueOf(startValue));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int convertedValue = minValue + progress;
                    sendServoValue(servoType, convertedValue);
                    editText.setText(String.valueOf(convertedValue));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBar.setProgress(startValue - minValue);
        }
    }

    //Takes a ServoType and returns the corresponding SeekBar
    private SeekBar getSeekBar(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (SeekBar) findViewById(R.id.seekbar_aileron);
            case ELEVATOR:
                return (SeekBar) findViewById(R.id.seekbar_elevator);
            case RUDDER:
                return (SeekBar) findViewById(R.id.seekbar_rudder);
            case THROTTLE:
                return (SeekBar) findViewById(R.id.seekbar_throttle);
        }
        return null;
    }

    //Takes a ServoType and returns the EditText that contains that ServoType's pin value
    private EditText getPinET(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (EditText) findViewById(R.id.et_arduino_value_pin_aileron);
            case ELEVATOR:
                return (EditText) findViewById(R.id.et_arduino_value_pin_elevator);
            case RUDDER:
                return (EditText) findViewById(R.id.et_arduino_value_pin_rudder);
            case THROTTLE:
                return (EditText) findViewById(R.id.et_arduino_value_pin_throttle);
        }
        return null;
    }

    //Takes a ServoType and return the EditText that contains that ServoType's servo value
    private EditText getServoET(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (EditText) findViewById(R.id.et_arduino_value_aileron);
            case ELEVATOR:
                return (EditText) findViewById(R.id.et_arduino_value_elevator);
            case RUDDER:
                return (EditText) findViewById(R.id.et_arduino_value_rudder);
            case THROTTLE:
                return (EditText) findViewById(R.id.et_arduino_value_throttle);
        }
        return null;
    }

    //Sets the servo value for a given servo type
    private void sendServoValue(ServoPacket.ServoType servoType, int value) {
        if (mUsbSerialIsReady) {
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setServoValue(servoType, value);
            Intent servoInputIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoInputIntent);
        }
    }

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configurePinEditText(final ServoPacket.ServoType servoType) {
        final EditText editText = getPinET(servoType);
        if (editText != null ) {
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = getNumPickAlertDialogBuilder(editText, servoType, true);
                    alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " pin number");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
        }
    }

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configureServoEditText(final ServoPacket.ServoType servoType) {
        final EditText editText = getServoET(servoType);
        if (editText != null ) {
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = getNumPickAlertDialogBuilder(editText, servoType, false);
                    alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " value");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
            editText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    configureRangeAlertDialog(servoType);
                    return true;
                }
            });
        }
    }

    //Creates a customized AlertDialogBuilder that includes a NumberPicker
    private AlertDialog.Builder getNumPickAlertDialogBuilder(final EditText editText, final ServoPacket.ServoType servoType, final boolean isPinDialog) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        final NumberPicker numberPicker = new NumberPicker(this);
        final int max = getServoMax(servoType);
        final int min = getServoMin(servoType);
        //If this is a servo pin AlertDialog, use the servo pin min and max constants
        if (isPinDialog) {
            numberPicker.setMaxValue(PIN_MAX);
            numberPicker.setMinValue(PIN_MIN);
        //If this is not a servo pin AlertDialog, use the corresponding servo value min and max vars
       } else {
            if (max != -1 && min != -1) {
                numberPicker.setMaxValue(max);
                numberPicker.setMinValue(min);
            }
        }
        numberPicker.setValue(Integer.parseInt(editText.getText().toString()));
        numberPicker.setOnValueChangedListener(this);
        numberPicker.setWrapSelectorWheel(false);
        //Create numPickFrameLayout to properly center numberPicker
        final FrameLayout numPickFrameLayout = new FrameLayout(this);
        numPickFrameLayout.addView(numberPicker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        alertDialogBuilder.setView(numPickFrameLayout);
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = numberPicker.getValue();
                //If this is a dialog to set the servo pin, then set the pin for the servo
                if (isPinDialog) {
                    setServoOutputPin(servoType, value);
                //If this is not a dialog to set the servo pin, then set the servo value instead
                } else if (max != -1 && min != -1) {
                    SeekBar seekBar = getSeekBar(servoType);
                    if (seekBar != null) seekBar.setProgress(value - min);
                }
                editText.setText(String.valueOf(value));
                dialog.dismiss();
            }
        });
        return alertDialogBuilder;
    }

    //Sets the pin number for a given servo type
    private void setServoOutputPin(ServoPacket.ServoType servoType, int pinValue) {
        if (mUsbSerialIsReady) {
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setOutputPin(servoType, pinValue);
            Intent servoConfigIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoConfigIntent);
        }
    }

    //Prompts the user to set the value range for a given servo type
    private void configureRangeAlertDialog (final ServoPacket.ServoType servoType) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " range");
        mRangeSeekBar.setRangeValues(SERVO_MIN, SERVO_MAX);
        mRangeSeekBar.setSelectedMaxValue(getServoMax(servoType));
        mRangeSeekBar.setSelectedMinValue(getServoMin(servoType));
        //Remove the parent view from mRangeSeekBar to prevent a 'Parent Not Null error'
        ViewGroup parent = (ViewGroup) mRangeSeekBar.getParent();
        if (parent != null) {
            parent.removeView(mRangeSeekBar);
            mRangeSeekBar.setVisibility(View.VISIBLE);
        }
        alertDialogBuilder.setView(mRangeSeekBar);
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //Based on the mRangeSeekBar input, set the min and max values for the corresponding SeekBar
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int max = mRangeSeekBar.getSelectedMaxValue();
                int min = mRangeSeekBar.getSelectedMinValue();
                //Store the max and min in the corresponding int for the current servo type
                switch (servoType) {
                    case AILERON:
                        aileronMax = max;
                        aileronMin = min;
                        break;
                    case ELEVATOR:
                        elevatorMax = max;
                        elevatorMin = min;
                        break;
                    case RUDDER:
                        rudderMax = max;
                        rudderMin = min;
                        break;
                    case THROTTLE:
                        throttleMax = max;
                        throttleMin = min;
                        break;
                }
                EditText editText = getServoET(servoType);
                if (editText != null) {
                    //If the current servo value is not within the new range, constrain it
                    int currentServoValue = Integer.parseInt(editText.getText().toString());
                    if (currentServoValue < min) currentServoValue = min;
                    if (currentServoValue > max) currentServoValue = max;
                    //Send the servo output range to the usb device
                    sendServoOutputRange(servoType, min, max);
                    //configure the corresponding seekbar to use the new range and servo value
                    configureSeekbar(servoType, currentServoValue, min, max);
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //Takes a servo packet and returns the corresponding current max range value
    private int getServoMax(ServoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return aileronMax;
            case ELEVATOR:
                return elevatorMax;
            case RUDDER:
                return rudderMax;
            case THROTTLE:
                return throttleMax;
        }
        return -1;
    }
    //Takes a servo packet and returns the corresponding current min range value
    private int getServoMin(ServoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return aileronMin;
            case ELEVATOR:
                return elevatorMin;
            case RUDDER:
                return rudderMin;
            case THROTTLE:
                return throttleMin;
        }
        return -1;
    }

    private void sendServoOutputRange(ServoPacket.ServoType servoType, int outputMin, int outputMax) {
        if (mUsbSerialIsReady) {
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setOutputRange(servoType, outputMin, outputMax);
            Intent servoConfigIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoConfigIntent);
        }
    }

    //Usb state notifications from UsbSerialService are received here.
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
            if (intent.getAction().equals(UsbSerialService.ACTION_USB_READY)) {
                statusTV.setText("USB ready");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_USB_DISCONNECTED)) {
                mUsbSerialIsReady = false;
                statusTV.setText("USB disconnected");
                TextView outputTV = (TextView) findViewById(R.id.tv_arduino_value_output);
                outputTV.setText("");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_USB_PERMISSION_GRANTED)) {
                statusTV.setText("USB permission granted");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_USB_PERMISSION_NOT_GRANTED)) {
                statusTV.setText("USB permission not granted");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_NO_USB)) {
                statusTV.setText("USB not connected");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_USB_NOT_SUPPORTED)) {
                statusTV.setText("USB device not supported");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_CDC_DRIVER_NOT_WORKING)) {
                statusTV.setText("USB CDC driver not found");
            } else if (intent.getAction().equals(UsbSerialService.ACTION_USB_DEVICE_NOT_WORKING)) {
                statusTV.setText("USB device not working");
            }
        }
    };

    //Listens for responses from the connected USB serial device and updates the output TextView
    private final BroadcastReceiver mDeviceOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServoPacket.INTENT_ACTION_OUTPUT)) {
                ServoPacket servoPacket = new ServoPacket(intent.getExtras());
                //If the device sent out a ready status, configure servos and enable SeekBars
                if (servoPacket.isStatusReady() && !mUsbSerialIsReady) {
                    mUsbSerialIsReady = true;
                    configureAllServoOutput();
                    TextView statusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
                    statusTV.setText("USB ready");
                }
                TextView outputTV = (TextView) findViewById(R.id.tv_arduino_value_output);
                outputTV.setText(servoPacket.toJsonString());
            }
        }
    };

    //Sends the configuration information for all output servos to the device
    private void configureAllServoOutput() {
        if (mUsbSerialIsReady) {
            EditText aileronET = (EditText) findViewById(R.id.et_arduino_value_pin_aileron);
            EditText elevatorET = (EditText) findViewById(R.id.et_arduino_value_pin_elevator);
            EditText rudderET = (EditText) findViewById(R.id.et_arduino_value_pin_rudder);
            EditText throttleET = (EditText) findViewById(R.id.et_arduino_value_pin_throttle);
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setOutputPin(ServoPacket.ServoType.AILERON, Integer.parseInt(aileronET.getText().toString()));
            servoPacket.setOutputPin(ServoPacket.ServoType.ELEVATOR, Integer.parseInt(elevatorET.getText().toString()));
            servoPacket.setOutputPin(ServoPacket.ServoType.RUDDER, Integer.parseInt(rudderET.getText().toString()));
            servoPacket.setOutputPin(ServoPacket.ServoType.THROTTLE, Integer.parseInt(throttleET.getText().toString()));
            Intent servoConfigIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoConfigIntent);
        }
    }

}