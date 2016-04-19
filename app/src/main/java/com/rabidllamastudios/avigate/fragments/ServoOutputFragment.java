package com.rabidllamastudios.avigate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.test.suitebuilder.annotation.Suppress;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.models.ArduinoPacket;

import org.florescu.android.rangeseekbar.RangeSeekBar;

/**
 * This Fragment class sets the layout and UI logic for configuring the servo inputs
 * Created by Ryan Staatz on 12/19/15.
 */
public class ServoOutputFragment extends Fragment implements NumberPicker.OnValueChangeListener {

    private static final int PIN_MAX = 12;
    private static final int PIN_MIN = 0;
    private static final int SERVO_MAX = 180;
    private static final int SERVO_MIN = 0;

    private boolean mRangeChange = false;

    private int mAileronMax = SERVO_MAX;
    private int mAileronMin = SERVO_MIN;
    private int mElevatorMax = SERVO_MAX;
    private int mElevatorMin = SERVO_MIN;
    private int mRudderMax = SERVO_MAX;
    private int mRudderMin = SERVO_MIN;
    private int mThrottleMax = SERVO_MAX;
    private int mThrottleMin = SERVO_MIN;

    private Callback mCallback;
    private RangeSeekBar<Integer> mRangeSeekBar;
    private View mRootView;

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_servo_outputs, container, false);
        setHasOptionsMenu(true);

        //Initialize mRangeSeekBar
        mRangeSeekBar = (RangeSeekBar) mRootView.findViewById(R.id.seekbar_range);

        //Configure UI
        if (mCallback != null) mCallback.loadOutputConfiguration();
        configureSeekBars();
        configureEditTexts();

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Adds a 'reset servos' icon to the SupportActionBar menu
        inflater.inflate(R.menu.menu_fragment_output, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //This menu action is handled by the parent activity and not this fragment
                return false;
            case R.id.item_enable_transmitter:
                //This menu action is handled by ReceiverCalibrationFragment and not this fragment
                return false;
            case R.id.item_reset_servos:
                //Reset servo values if the reset servo icon is tapped
                resetServoValues();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        //Required method for implementing the NumberPicker class
    }

    /** Callback class used to communicate from this Fragment to the parent activity */
    public interface Callback {
        /** Triggers the loading of the servo output configuration */
        void loadOutputConfiguration();

        /** Sets the pin number for a given ServoType */
        void setServoOutputPin(ArduinoPacket.ServoType servoType, int pinValue);

        /** Configures the servo output range for a given ServoType */
        void setServoOutputRange(ArduinoPacket.ServoType servoType, int outputMin, int outputMax);

        /** Sets the servo value for a given ServoType */
        void setServoValue(ArduinoPacket.ServoType servoType, int servoValue);
    }

    /** Loads the servo output configuration into the UI from the masterArduinoPacket parameter */
    public void loadOutputConfiguration(ArduinoPacket masterArduinoPacket) {
        loadServoOutputConfig(masterArduinoPacket, ArduinoPacket.ServoType.AILERON);
        loadServoOutputConfig(masterArduinoPacket, ArduinoPacket.ServoType.ELEVATOR);
        loadServoOutputConfig(masterArduinoPacket, ArduinoPacket.ServoType.RUDDER);
        loadServoOutputConfig(masterArduinoPacket, ArduinoPacket.ServoType.THROTTLE);
    }

    /** Sets the callback for this Fragment. Enables communication with the parent activity */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //Resets all servo output values to their default
    private void resetServoValues() {
        SeekBar aileronSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_aileron);
        SeekBar elevatorSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_elevator);
        SeekBar rudderSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_rudder);
        SeekBar throttleSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_throttle);

        aileronSeekBar.setProgress((mAileronMax - mAileronMin) / 2);
        elevatorSeekBar.setProgress((mElevatorMax - mElevatorMin) / 2);
        rudderSeekBar.setProgress((mRudderMax - mRudderMin) / 2);
        throttleSeekBar.setProgress(SERVO_MIN);

        //Shows Snackbar alerting user that servo values were reset
        Snackbar.make(mRootView, "Servo values have been reset", Snackbar.LENGTH_SHORT).show();
    }

    //Loads the output configuration from a ArduinoPacket for a given ServoType
    private void loadServoOutputConfig(ArduinoPacket arduinoPacket,
                                       ArduinoPacket.ServoType servoType) {
        //If the config has an output pin set, set the output pin EditText for that ServoType
        EditText editText = getOutputPinEditText(servoType);
        if (editText != null && arduinoPacket.hasOutputPin(servoType)) {
            editText.setText(String.valueOf(arduinoPacket.getOutputPin(servoType)));
        }

        //If the config contains an output min, set that setOutputMin for that ServoType
        if (arduinoPacket.hasOutputMin(servoType)) {
            int min = arduinoPacket.getOutputMin(servoType);
            if (min != -1) setOutputMin(servoType, min);
        }

        //If the config contains an output max, set that setOutputMin for that ServoType
        if (arduinoPacket.hasOutputMax(servoType)) {
            int max = arduinoPacket.getOutputMax(servoType);
            if (max != -1) setOutputMax(servoType, max);
        }

        //In case the output range has not been set yet
        mCallback.setServoOutputRange(servoType, getServoMin(servoType), getServoMax(servoType));
    }

    //Configures all SeekBars in the layout
    private void configureSeekBars() {
        //Set control surfaces to neutral position
        configureSeekbar(ArduinoPacket.ServoType.AILERON, (mAileronMax - mAileronMin) / 2);
        configureSeekbar(ArduinoPacket.ServoType.ELEVATOR, (mElevatorMax - mElevatorMin) / 2);
        configureSeekbar(ArduinoPacket.ServoType.RUDDER, (mRudderMax - mRudderMin) / 2);
        //Set throttle to minimum
        configureSeekbar(ArduinoPacket.ServoType.THROTTLE, SERVO_MIN);
    }

    //Configures all EditTexts in the layout
    private void configureEditTexts() {
        //Configure output pin EditTexts
        configureOutputPinEditText(ArduinoPacket.ServoType.AILERON);
        configureOutputPinEditText(ArduinoPacket.ServoType.ELEVATOR);
        configureOutputPinEditText(ArduinoPacket.ServoType.RUDDER);
        configureOutputPinEditText(ArduinoPacket.ServoType.THROTTLE);

        //Configure servo value EditTexts
        configureServoValueEditText(ArduinoPacket.ServoType.AILERON);
        configureServoValueEditText(ArduinoPacket.ServoType.ELEVATOR);
        configureServoValueEditText(ArduinoPacket.ServoType.RUDDER);
        configureServoValueEditText(ArduinoPacket.ServoType.THROTTLE);
    }

    //Configures the SeekBar and OnSeekBarChangeListener
    private void configureSeekbar(final ArduinoPacket.ServoType servoType, int progress) {
        final EditText editText = getServoEditText(servoType);
        SeekBar seekBar = getSeekBar(servoType);
        if (editText != null && seekBar != null) {
            int minValue = getServoMin(servoType);
            int maxValue = getServoMax(servoType);
            if (minValue != -1 && maxValue != -1) seekBar.setMax(maxValue - minValue);
            //Set editText here since onProgressChanged may not be called on setProgress
            editText.setText(String.valueOf(progress + minValue));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    //Each time the servo output range is changed, this method is triggered
                    if (mRangeChange) {
                        //Triggered when the min and/or max for the progress bar is changed
                        //This allows changing the min and/or max without setting the progress
                        mRangeChange = false;
                    } else {
                        int min = getServoMin(servoType);
                        if (min != -1) {
                            int convertedValue = min + progress;
                            if (mCallback != null) {
                                mCallback.setServoValue(servoType, convertedValue);
                            }
                            editText.setText(String.valueOf(convertedValue));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}

            });
            seekBar.setProgress(progress);
        }
    }

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configureOutputPinEditText(final ArduinoPacket.ServoType servoType) {
        final EditText editText = getOutputPinEditText(servoType);
        if (editText != null ) {
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = getNumPickAlertDialogBuilder(editText,
                            servoType, true);
                    alertDialogBuilder.setTitle("Set " + servoType.getStringValue()
                            + " output pin number");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
        }
    }

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configureServoValueEditText(final ArduinoPacket.ServoType servoType) {
        final EditText editText = getServoEditText(servoType);
        if (editText != null ) {
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = getNumPickAlertDialogBuilder(editText,
                            servoType, false);
                    alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " value");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
            editText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showRangeAlertDialog(servoType);
                    return true;
                }
            });
        }
    }

    //Creates a customized AlertDialogBuilder that includes a NumberPicker
    private AlertDialog.Builder getNumPickAlertDialogBuilder(final EditText editText,
                                                             final ArduinoPacket.ServoType servoType,
                                                             final boolean isPinDialog) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final NumberPicker numberPicker = new NumberPicker(getActivity());
        final int max = getServoMax(servoType);
        final int min = getServoMin(servoType);
        //If this is a servo pin AlertDialog, use the servo pin min and max constants
        if (isPinDialog) {
            numberPicker.setMaxValue(PIN_MAX);
            numberPicker.setMinValue(PIN_MIN);
            //If this is not a pin AlertDialog, use the corresponding servo value min and max vars
        } else {
            if (max != -1 && min != -1) {
                numberPicker.setMaxValue(max);
                numberPicker.setMinValue(min);
            }
        }
        if (editText.getText().toString().equals(
                getString(R.string.et_arduino_value_pin_default))) {
            //If there is no output pin yet set, set selector to middle value in pin min/max range
            int middlePinValue = (PIN_MAX - PIN_MIN)/2;
            numberPicker.setValue(middlePinValue);
        } else {
            //If there is already an output pin set, set selector to this value
            numberPicker.setValue(Integer.parseInt(editText.getText().toString()));
        }
        numberPicker.setWrapSelectorWheel(false);
        //Create numPickFrameLayout to properly center numberPicker
        final FrameLayout numPickFrameLayout = new FrameLayout(getActivity());
        numPickFrameLayout.addView(numberPicker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        alertDialogBuilder.setView(numPickFrameLayout);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = numberPicker.getValue();
                //If this is a dialog to set the servo pin, then set the pin for the servo
                if (isPinDialog) {
                    if (mCallback != null) {
                        //Set the output pin
                        mCallback.setServoOutputPin(servoType, value);
                    }
                    //If this is not a dialog to set the servo pin, then set the servo value instead
                } else if (max != -1 && min != -1) {
                    SeekBar seekBar = getSeekBar(servoType);
                    if (seekBar != null) seekBar.setProgress(value - min);
                }
                editText.setText(String.valueOf(value));
            }
        });
        return alertDialogBuilder;
    }

    //Prompts the user to set the value range for a given servo type
    private void showRangeAlertDialog (final ArduinoPacket.ServoType servoType) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Set " + servoType.getStringValue() + " output range");
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
        alertDialogBuilder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
        //Based on the mRangeSeekBar input, set the min and max values for the corresponding SeekBar
        alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int max = mRangeSeekBar.getSelectedMaxValue();
                int min = mRangeSeekBar.getSelectedMinValue();
                //Store the max and min in the corresponding int for the current servo type
                setOutputMin(servoType, min);
                setOutputMax(servoType, max);
                EditText editText = getServoEditText(servoType);
                if (editText != null) {
                    //If the current servo value is not within the new range, constrain it to be
                    int currentServoValue = Integer.parseInt(editText.getText().toString());
                    if (currentServoValue < min) currentServoValue = min;
                    if (currentServoValue > max) currentServoValue = max;
                    //Send the servo output range to the Arduino
                    mCallback.setServoOutputRange(servoType, min, max);
                    //Configure the corresponding SeekBar to use the new range
                    SeekBar seekBar = getSeekBar(servoType);
                    if (seekBar != null) {
                        //Prevent the SeekBar from using a previous value before new range is set
                        mRangeChange = true;
                        seekBar.setMax(max - min);
                    }
                    //Set the progress value for configureSeekbar (actual servo value minus the min)
                    configureSeekbar(servoType, currentServoValue - min);
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //Takes a ServoType and returns the corresponding SeekBar
    @SuppressWarnings("ConstantConditions")
    private SeekBar getSeekBar(ArduinoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (SeekBar) mRootView.findViewById(R.id.seekbar_aileron);
            case ELEVATOR:
                return (SeekBar) mRootView.findViewById(R.id.seekbar_elevator);
            case RUDDER:
                return (SeekBar) mRootView.findViewById(R.id.seekbar_rudder);
            case THROTTLE:
                return (SeekBar) mRootView.findViewById(R.id.seekbar_throttle);
            default:
                return null;
        }
    }

    //Takes a ServoType and returns the EditText that contains that ServoType's output pin value
    @SuppressWarnings("ConstantConditions")
    private EditText getOutputPinEditText(ArduinoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_output_aileron);
            case ELEVATOR:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_output_elevator);
            case RUDDER:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_output_rudder);
            case THROTTLE:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_output_throttle);
            default:
                return null;
        }
    }

    //Takes a ServoType and return the EditText that contains that ServoType's servo value
    @SuppressWarnings("ConstantConditions")
    private EditText getServoEditText(ArduinoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_output_aileron);
            case ELEVATOR:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_output_elevator);
            case RUDDER:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_output_rudder);
            case THROTTLE:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_output_throttle);
            default:
                return null;
        }
    }

    //Takes a ServoType and returns the corresponding current max range value
    private int getServoMax(ArduinoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return mAileronMax;
            case ELEVATOR:
                return mElevatorMax;
            case RUDDER:
                return mRudderMax;
            case THROTTLE:
                return mThrottleMax;
            default:
                return -1;
        }
    }
    //Takes a ServoType and returns the corresponding current min range value
    private int getServoMin(ArduinoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return mAileronMin;
            case ELEVATOR:
                return mElevatorMin;
            case RUDDER:
                return mRudderMin;
            case THROTTLE:
                return mThrottleMin;
            default:
                return -1;
        }
    }

    //Takes a ServoType and sets the corresponding max output value to the outputMax parameter
    private void setOutputMax(ArduinoPacket.ServoType servoType, int outputMax) {
        switch (servoType) {
            case AILERON:
                mAileronMax = outputMax;
                return;
            case ELEVATOR:
                mElevatorMax = outputMax;
                return;
            case RUDDER:
                mRudderMax = outputMax;
                return;
            case THROTTLE:
                mThrottleMax = outputMax;
        }
    }

    //Takes a ServoType and sets the corresponding min output value to the outputMin parameter
    private void setOutputMin(ArduinoPacket.ServoType servoType, int outputMin) {
        switch (servoType) {
            case AILERON:
                mAileronMin = outputMin;
                return;
            case ELEVATOR:
                mElevatorMin = outputMin;
                return;
            case RUDDER:
                mRudderMin = outputMin;
                return;
            case THROTTLE:
                mThrottleMin = outputMin;
        }
    }
}