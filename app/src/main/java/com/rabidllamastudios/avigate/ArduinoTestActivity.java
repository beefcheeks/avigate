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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.model.ServoPacket;

/**
 * Created by Ryan Staatz
 * This activity is intended as a test for USB-OTG connected CDC-ACM devices (e.g. Arduino)
 * Some code adapted from: https://github.com/felHR85/SerialPortExample
 */

public class ArduinoTestActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {

    private static final int BAUD_RATE = 115200;
    private static final int NUMPICK_MAX_VALUE = 16;
    private static final int NUMPICK_MIN_VALUE = 0;
    private static final int SERVO_NEUTRAL = 90;
    private static final int SERVO_MIN = 0;

    private boolean mUsbSerialIsReady = false;

    private Intent mUsbSerialService;
    private IntentFilter mUsbIntentFilter;
    private IntentFilter mDeviceOutputIntentFilter;

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

        //Initialize UI
        configureSeekbar((SeekBar) findViewById(R.id.seekbar_aileron), SERVO_NEUTRAL, ServoPacket.ServoType.AILERON, (TextView) findViewById(R.id.tv_arduino_value_aileron));
        configureSeekbar((SeekBar) findViewById(R.id.seekbar_elevator), SERVO_NEUTRAL, ServoPacket.ServoType.ELEVATOR, (TextView) findViewById(R.id.tv_arduino_value_elevator));
        configureSeekbar((SeekBar) findViewById(R.id.seekbar_rudder), SERVO_NEUTRAL, ServoPacket.ServoType.RUDDER, (TextView) findViewById(R.id.tv_arduino_value_rudder));
        configureSeekbar((SeekBar) findViewById(R.id.seekbar_throttle), SERVO_MIN, ServoPacket.ServoType.THROTTLE, (TextView) findViewById(R.id.tv_arduino_value_throttle));
        configureEditText((EditText) findViewById(R.id.et_pin_aileron), ServoPacket.ServoType.AILERON);
        configureEditText((EditText) findViewById(R.id.et_pin_elevator), ServoPacket.ServoType.ELEVATOR);
        configureEditText((EditText) findViewById(R.id.et_pin_rudder), ServoPacket.ServoType.RUDDER);
        configureEditText((EditText) findViewById(R.id.et_pin_throttle), ServoPacket.ServoType.THROTTLE);
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
        stopService(mUsbSerialService);
        super.onPause();
    }

    @Override
    public void onResume() {
        registerReceiver(mUsbReceiver, mUsbIntentFilter);  //Start listening for USB notifications from Android system
        registerReceiver(mDeviceOutputReceiver, mDeviceOutputIntentFilter);  //Start listening for servo output intents
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this, BAUD_RATE);  //Get the configured intent to start the UsbSerialService
        startService(mUsbSerialService); //Start the UsbSerialService
        super.onResume();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {}

    //Emergency reset button resets all sliders back to defaults
    public void emergencyReset(View view) {
        resetAllSeekbars();
    }

    //Configures the SeekBar and OnSeekBarChangeListener
    private void configureSeekbar(SeekBar seekBar, int progressValue, final ServoPacket.ServoType servoType, final TextView textView) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendServoInput(servoType, progress);
                textView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar.setProgress(progressValue);
        seekBar.setEnabled(false);
    }

    //Sets the servo value for a given servo type
    private void sendServoInput(ServoPacket.ServoType servoType, int value) {
        if (mUsbSerialIsReady) {
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setServoValue(servoType, value);
            Intent servoInputIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoInputIntent);
        }
    }

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configureEditText(final EditText editText, final ServoPacket.ServoType servoType) {
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configureServo(servoType, editText);
            }
        });
    }

    //Prompts the user with an AlertDialog to select a pin number for a given servo type
    private void configureServo(final ServoPacket.ServoType servoType, final EditText editText) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " pin number");
        alertDialogBuilder.setCancelable(true);
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(NUMPICK_MIN_VALUE);
        numberPicker.setMaxValue(NUMPICK_MAX_VALUE);
        numberPicker.setValue(Integer.parseInt(editText.getText().toString()));
        numberPicker.setOnValueChangedListener(this);
        numberPicker.setWrapSelectorWheel(false);
        final FrameLayout parent = new FrameLayout(this);
        parent.addView(numberPicker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        alertDialogBuilder.setView(parent);
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int pinValue = numberPicker.getValue();
                setServoPin(servoType, pinValue);
                editText.setText(String.valueOf(pinValue));
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //Sets the pin number for a given servo type
    private void setServoPin(ServoPacket.ServoType servoType, int pinValue) {
        if (mUsbSerialIsReady) {
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setServoPin(servoType, pinValue);
            Intent servoConfigIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoConfigIntent);
        }
    }

    //Resets all seekbar values to their default
    private void resetAllSeekbars() {
        SeekBar aileronSeekBar = (SeekBar) findViewById(R.id.seekbar_aileron);
        SeekBar elevatorSeekBar = (SeekBar) findViewById(R.id.seekbar_elevator);
        SeekBar rudderSeekBar = (SeekBar) findViewById(R.id.seekbar_rudder);
        SeekBar throttleSeekBar = (SeekBar) findViewById(R.id.seekbar_throttle);
        aileronSeekBar.setProgress(SERVO_NEUTRAL);
        elevatorSeekBar.setProgress(SERVO_NEUTRAL);
        rudderSeekBar.setProgress(SERVO_NEUTRAL);
        throttleSeekBar.setProgress(SERVO_MIN);
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
                setSeekbarEnabled(false);
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
                    configureAllServos();
                    setSeekbarEnabled(true);
                }
                TextView outputTV = (TextView) findViewById(R.id.tv_arduino_value_output);
                outputTV.setText(servoPacket.toJsonString());
            }
        }
    };

    //Sends the configuration information for all output servos to the device
    private void configureAllServos() {
        if (mUsbSerialIsReady) {
            EditText aileronET = (EditText) findViewById(R.id.et_pin_aileron);
            EditText elevatorET = (EditText) findViewById(R.id.et_pin_elevator);
            EditText rudderET = (EditText) findViewById(R.id.et_pin_rudder);
            EditText throttleET = (EditText) findViewById(R.id.et_pin_throttle);
            ServoPacket servoPacket = new ServoPacket();
            servoPacket.setServoPin(ServoPacket.ServoType.AILERON, Integer.parseInt(aileronET.getText().toString()));
            servoPacket.setServoPin(ServoPacket.ServoType.ELEVATOR, Integer.parseInt(elevatorET.getText().toString()));
            servoPacket.setServoPin(ServoPacket.ServoType.RUDDER, Integer.parseInt(rudderET.getText().toString()));
            servoPacket.setServoPin(ServoPacket.ServoType.THROTTLE, Integer.parseInt(throttleET.getText().toString()));
            Intent servoConfigIntent = servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT);
            sendBroadcast(servoConfigIntent);
        }
    }

    //Enables or disables the seekbar based on the boolean value of the setToEnabled parameter
    private void setSeekbarEnabled (boolean setToEnabled) {
        SeekBar aileronSeekBar = (SeekBar) findViewById(R.id.seekbar_aileron);
        SeekBar elevatorSeekBar = (SeekBar) findViewById(R.id.seekbar_elevator);
        SeekBar rudderSeekBar = (SeekBar) findViewById(R.id.seekbar_rudder);
        SeekBar throttleSeekBar = (SeekBar) findViewById(R.id.seekbar_throttle);

        if (setToEnabled) {
            aileronSeekBar.setEnabled(true);
            elevatorSeekBar.setEnabled(true);
            rudderSeekBar.setEnabled(true);
            throttleSeekBar.setEnabled(true);
        } else {
            aileronSeekBar.setEnabled(false);
            elevatorSeekBar.setEnabled(false);
            rudderSeekBar.setEnabled(false);
            throttleSeekBar.setEnabled(false);
            resetAllSeekbars();
        }
    }

}