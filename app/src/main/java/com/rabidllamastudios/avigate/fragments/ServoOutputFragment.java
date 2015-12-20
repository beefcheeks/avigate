package com.rabidllamastudios.avigate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.model.ServoPacket;

import org.florescu.android.rangeseekbar.RangeSeekBar;

/**
 * Created by Ryan on 12/19/15.
 * This Fragment class sets the layout and UI logic for configuring the servo inputs
 */
public class ServoOutputFragment extends Fragment implements NumberPicker.OnValueChangeListener {

    private static final int PIN_MAX = 12;
    private static final int PIN_MIN = 0;
    private static final int SERVO_MAX = 180;
    private static final int SERVO_MIN = 0;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_servo_outputs, container, false);

        //Initialize mRangeSeekBar
        mRangeSeekBar = (RangeSeekBar) mRootView.findViewById(R.id.seekbar_range);

        //Configure UI
        configureSeekBars();
        configureEditTexts();

        return mRootView;
    }

    //Set the callback for ServoInputFragment
    //Enables the Fragment to communicate with the activity
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //Configures all output pins based on the corresponding EditText output pin values
    public void configureOutputPins() {
        EditText aileronEditText = (EditText) mRootView.findViewById(
                R.id.et_arduino_value_pin_output_aileron);
        EditText elevatorEditText = (EditText) mRootView.findViewById(
                R.id.et_arduino_value_pin_output_elevator);
        EditText rudderEditText = (EditText) mRootView.findViewById(
                R.id.et_arduino_value_pin_output_rudder);
        EditText throttleEditText = (EditText) mRootView.findViewById(
                R.id.et_arduino_value_pin_output_throttle);
        mCallback.setServoOutputPin(ServoPacket.ServoType.AILERON,
                Integer.parseInt(aileronEditText.getText().toString()));
        mCallback.setServoOutputPin(ServoPacket.ServoType.ELEVATOR,
                Integer.parseInt(elevatorEditText.getText().toString()));
        mCallback.setServoOutputPin(ServoPacket.ServoType.RUDDER,
                Integer.parseInt(rudderEditText.getText().toString()));
        mCallback.setServoOutputPin(ServoPacket.ServoType.THROTTLE,
                Integer.parseInt(throttleEditText.getText().toString()));
    }
    //Configures all SeekBars in the layout
    private void configureSeekBars() {
        int servoNeutral = (SERVO_MAX - SERVO_MIN)/2;
        configureSeekbar(ServoPacket.ServoType.AILERON, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.ELEVATOR, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.RUDDER, servoNeutral, SERVO_MIN, SERVO_MAX);
        configureSeekbar(ServoPacket.ServoType.THROTTLE, SERVO_MIN, SERVO_MIN, SERVO_MAX);
    }

    //Configures all EditTexts in the layout
    private void configureEditTexts() {
        //Configure output pin EditTexts
        configureOutputPinEditText(ServoPacket.ServoType.AILERON);
        configureOutputPinEditText(ServoPacket.ServoType.ELEVATOR);
        configureOutputPinEditText(ServoPacket.ServoType.RUDDER);
        configureOutputPinEditText(ServoPacket.ServoType.THROTTLE);

        //Configure servo value EditTexts
        configureServoValueEditText(ServoPacket.ServoType.AILERON);
        configureServoValueEditText(ServoPacket.ServoType.ELEVATOR);
        configureServoValueEditText(ServoPacket.ServoType.RUDDER);
        configureServoValueEditText(ServoPacket.ServoType.THROTTLE);
    }

    //Configures the SeekBar and OnSeekBarChangeListener
    private void configureSeekbar(final ServoPacket.ServoType servoType, int startValue,
                                  final int minValue, final int maxValue) {
        final EditText editText = getServoEditText(servoType);
        SeekBar seekBar = getSeekBar(servoType);
        if (editText != null && seekBar != null) {
            seekBar.setMax(maxValue - minValue);
            //Set editText here since onProgressChanged may not be called on setProgress
            editText.setText(String.valueOf(startValue));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int convertedValue = minValue + progress;
                    mCallback.setServoValue(servoType, convertedValue);
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

    //Configures the EditText field to be clickable and bring up a NumberPicker AlertDialog
    private void configureOutputPinEditText(final ServoPacket.ServoType servoType) {
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
    private void configureServoValueEditText(final ServoPacket.ServoType servoType) {
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
                    configureRangeAlertDialog(servoType);
                    return true;
                }
            });
        }
    }

    //Creates a customized AlertDialogBuilder that includes a NumberPicker
    private AlertDialog.Builder getNumPickAlertDialogBuilder(final EditText editText,
                                                             final ServoPacket.ServoType servoType,
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
        numberPicker.setValue(Integer.parseInt(editText.getText().toString()));
        numberPicker.setWrapSelectorWheel(false);
        //Create numPickFrameLayout to properly center numberPicker
        final FrameLayout numPickFrameLayout = new FrameLayout(getActivity());
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
                    //Determine whether to configure the input or output pins
                    mCallback.setServoOutputPin(servoType, value);
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

    //Prompts the user to set the value range for a given servo type
    private void configureRangeAlertDialog (final ServoPacket.ServoType servoType) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
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
                        mAileronMax = max;
                        mAileronMin = min;
                        break;
                    case ELEVATOR:
                        mElevatorMax = max;
                        mElevatorMin = min;
                        break;
                    case RUDDER:
                        mRudderMax = max;
                        mRudderMin = min;
                        break;
                    case THROTTLE:
                        mThrottleMax = max;
                        mThrottleMin = min;
                        break;
                }
                EditText editText = getServoEditText(servoType);
                if (editText != null) {
                    //If the current servo value is not within the new range, constrain it
                    int currentServoValue = Integer.parseInt(editText.getText().toString());
                    if (currentServoValue < min) currentServoValue = min;
                    if (currentServoValue > max) currentServoValue = max;
                    //Send the servo output range to the usb device
                    mCallback.setServoOutputRange(servoType, min, max);
                    //configure the corresponding SeekBar to use the new range and servo value
                    configureSeekbar(servoType, currentServoValue, min, max);
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //Takes a ServoType and returns the corresponding SeekBar
    @SuppressWarnings("ConstantConditions")
    private SeekBar getSeekBar(ServoPacket.ServoType servoType) {
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
    private EditText getOutputPinEditText(ServoPacket.ServoType servoType) {
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
    private EditText getServoEditText(ServoPacket.ServoType servoType) {
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

    //Takes a servo packet and returns the corresponding current max range value
    private int getServoMax(ServoPacket.ServoType servoType) {
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
    //Takes a servo packet and returns the corresponding current min range value
    private int getServoMin(ServoPacket.ServoType servoType) {
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

    /**
     * This is the callback class for ServoOutputFragment
     */
    public interface Callback {
        //Sets the pin number for a given ServoType
        void setServoOutputPin(ServoPacket.ServoType servoType, int pinValue);

        //Configures the servo output range for a given ServoType
        void setServoOutputRange(ServoPacket.ServoType servoType, int outputMin, int outputMax);

        //Sets the servo value for a given ServoType
        void setServoValue(ServoPacket.ServoType servoType, int servoValue);

    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {}

}