package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Ryan on 11/30/15.
 * A data model class to communicate data to and from the Arduino
 * Can be constructed from a Bundle and converted into an Intent
 * This class stores Arduino configuration and commands as a JSON object & can return a JSON string
 **/
public class ArduinoPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    //Intent actions
    public static final String INTENT_ACTION_INPUT = PACKAGE_NAME + ".action.ARDUINO_INPUT";
    public static final String INTENT_ACTION_OUTPUT = PACKAGE_NAME + ".action.ARDUINO_OUTPUT";

    //Key for the entire root JSON String of the ArduinoPacket (value) when stored as an Intent extra
    private static final String KEY_ROOT = "json";

    //JSON keys for key value pairs
    private static final String KEY_CALIBRATION_MODE = "calibrationMode";
    private static final String KEY_ERROR = "error";
    private static final String KEY_INPUT_CONFIG = "inputConfig";
    private static final String KEY_MAX = "max";
    private static final String KEY_MIN = "min";
    private static final String KEY_OUTPUT_CONFIG = "outputConfig";
    private static final String KEY_PIN = "pin";
    private static final String KEY_RECEIVER_CONTROL = "receiverControl";
    private static final String KEY_RECEIVER_ONLY = "receiverOnly";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_STATUS = "status";
    private static final String KEY_VALUE ="value";

    //JSON value(s) for key value pairs
    private static final String VALUE_STATUS_READY = "ready";

    //Denotes the type of servo
    public enum ServoType {
        AILERON, ELEVATOR, RUDDER, THROTTLE, CUTOVER;

        //Returns the corresponding JSON String key
        public String getStringValue() {
            switch (this) {
                case AILERON:
                    return "aileron";
                case ELEVATOR:
                    return "elevator";
                case RUDDER:
                    return "rudder";
                case THROTTLE:
                    return "throttle";
                case CUTOVER:
                    return "cutover";
            }
            return null;
        }
    }

    private JSONObject rootJson;  //The JSON root object where all JSON data is stored

    public ArduinoPacket() {
        rootJson = new JSONObject();
    }

    //Creates a ArduinoPacket from a JSON String
    public ArduinoPacket(String jsonString) {
        rootJson = new JSONObject();
        try {
            rootJson = (JSONObject) new JSONParser().parse(jsonString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //Creates a ArduinoPacket from a bundle (e.g. Intent Extra)
    public ArduinoPacket(Bundle bundle) {
        rootJson = new JSONObject();
        try {
            rootJson = (JSONObject) new JSONParser().parse(bundle.getString(KEY_ROOT));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //Returns an Intent with the ArduinoPacket contents as an Intent Extra
    public Intent toIntent(String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.putExtra(KEY_ROOT, rootJson.toJSONString());
        return intent;
    }

    //Returns the stored JSON data as a string
    public String toJsonString() {
        return rootJson.toJSONString();
    }

    //Adds an arduino status request to the rootJson object
    @SuppressWarnings("unchecked")
    public void addStatusRequest() {
        rootJson.put(KEY_REQUEST, KEY_STATUS);
    }


    //Takes a ArduinoPacket and compares configurations with the rootJson object
    @Override
    public boolean equals(Object arduinoPacketObject) {
        if (arduinoPacketObject == null) return false;
        if (!(arduinoPacketObject instanceof ArduinoPacket)) return false;
        ArduinoPacket arduinoPacket = (ArduinoPacket) arduinoPacketObject;
        String arduinoPacketJsonString = arduinoPacket.toJsonString();
        if (arduinoPacketJsonString == null) return false;
        JSONObject otherRootJson = new JSONObject();
        try {
            otherRootJson = (JSONObject) new JSONParser().parse(arduinoPacketJsonString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return servoEquals(otherRootJson, ServoType.AILERON)
                && servoEquals(otherRootJson, ServoType.ELEVATOR)
                && servoEquals(otherRootJson, ServoType.RUDDER)
                && servoEquals(otherRootJson, ServoType.THROTTLE)
                && servoEquals(otherRootJson, ServoType.CUTOVER);
    }

    //Returns a JSON String containing all config values
    //Takes a boolean that determines whether to include the input min and max for that servo
    //Returns null if no configuration for that servo exists
    @SuppressWarnings("unchecked")
    public String getConfigJson(ServoType servoType, boolean includeInputRanges) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            //Don't include input range values from the input config
            JSONObject newInputConfigJson = new JSONObject();
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                if (includeInputRanges) {
                    if (inputConfigJson.containsKey(KEY_MAX)) {
                        newInputConfigJson.put(KEY_MAX, inputConfigJson.get(KEY_MAX));
                    }
                    if (inputConfigJson.containsKey(KEY_MIN)) {
                        newInputConfigJson.put(KEY_MIN, inputConfigJson.get(KEY_MIN));
                    }
                }
                if (inputConfigJson.containsKey(KEY_PIN)) {
                    newInputConfigJson.put(KEY_PIN, inputConfigJson.get(KEY_PIN));
                }
                if (inputConfigJson.containsKey(KEY_RECEIVER_ONLY)) {
                    newInputConfigJson.put(KEY_RECEIVER_ONLY,
                            inputConfigJson.get(KEY_RECEIVER_ONLY));
                }
            }
            //Keep all output config values
            JSONObject newOutputConfigJson = new JSONObject();
            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                JSONObject outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
                if (outputConfigJson.containsKey(KEY_MAX)) {
                    newOutputConfigJson.put(KEY_MAX, outputConfigJson.get(KEY_MAX));
                }
                if (outputConfigJson.containsKey(KEY_MIN)) {
                    newOutputConfigJson.put(KEY_MIN, outputConfigJson.get(KEY_MIN));
                }
                if (outputConfigJson.containsKey(KEY_PIN)) {
                    newOutputConfigJson.put(KEY_PIN, outputConfigJson.get(KEY_PIN));
                }
            }
            JSONObject newServoJson = new JSONObject();
            newServoJson.put(KEY_INPUT_CONFIG, newInputConfigJson);
            newServoJson.put(KEY_OUTPUT_CONFIG, newOutputConfigJson);
            JSONObject newRootJson = new JSONObject();
            newRootJson.put(servoType.getStringValue(), newServoJson);
            return newRootJson.toJSONString();
        }
        return null;
    }

    //Retrieves any error message if present
    public String getErrorMessage() {
        if (rootJson.containsKey(KEY_ERROR)) return (String) rootJson.get(KEY_ERROR);
        return null;
    }

    //Returns a JsonObject that contains the min and max receiver input values for a given ServoType
    //Returns null if no min and max values for that ServoType exists
    @SuppressWarnings("unchecked")
    public String getInputRangeJson(ServoType servoType) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                if (inputConfigJson.containsKey(KEY_MAX) && inputConfigJson.containsKey(KEY_MIN)) {
                    JSONObject inputRangeJson = new JSONObject();
                    inputRangeJson.put(KEY_MAX, inputConfigJson.get(KEY_MAX));
                    inputRangeJson.put(KEY_MIN, inputConfigJson.get(KEY_MIN));
                    JSONObject newServoJson = new JSONObject();
                    newServoJson.put(KEY_INPUT_CONFIG, inputRangeJson);
                    JSONObject newRootJson = new JSONObject();
                    newRootJson.put(servoType.getStringValue(), newServoJson);
                    return newRootJson.toJSONString();
                }
            }
        }
        return null;
    }

    //Returns the max receiver input value for a given ServoType
    //Returns -1 if said value does not exist
    public int getInputMax(ServoType servoType) {
        Number inputMax = (Number) getInputConfigValue(servoType, KEY_MAX);
        if (inputMax == null) return -1;
        return inputMax.intValue();
    }

    //Returns the min receiver input value for a given ServoType
    //Returns -1 if said value does not exist
    public int getInputMin(ServoType servoType) {
        Number inputMin = (Number) getInputConfigValue(servoType, KEY_MIN);
        if (inputMin == null) return -1;
        return inputMin.intValue();
    }

    //Returns the input pin
    //Returns -1 if said value does not exist
    public int getInputPin(ServoType servoType) {
        Number inputPin = (Number) getInputConfigValue(servoType, KEY_PIN);
        if (inputPin == null) return -1;
        return inputPin.intValue();
    }

    //Returns the max servo output value for a given ServoType
    //Returns -1 if said value does not exist
    public int getOutputMax(ArduinoPacket.ServoType servoType) {
        Number outputMax = getOutputConfigValue(servoType, KEY_MAX);
        if (outputMax == null) return -1;
        return outputMax.intValue();
    }

    //Returns the min servo output value for a given ServoType
    //Returns -1 if said value does not exist
    public int getOutputMin(ArduinoPacket.ServoType servoType) {
        Number outputMin = getOutputConfigValue(servoType, KEY_MIN);
        if (outputMin == null) return -1;
        return outputMin.intValue();
    }

    //Returns the input pin of a given ServoType
    //Returns -1 if said value does not exist
    public int getOutputPin(ServoType servoType) {
        Number outputPin =  getOutputConfigValue(servoType, KEY_PIN);
        if (outputPin == null) return -1;
        return outputPin.intValue();
    }

    //Returns the value of a given ServoType
    public int getServoValue(ServoType servoType) {
        //Returns -1 if said value does not exist
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_VALUE)) {
                Number value = (Number) servoJson.get(KEY_VALUE);
                if (value != null) return value.intValue();
            }
        }
        return -1;
    }

    //Checks whether rootJson contains the calibrationMode JSON key
    public boolean hasCalibrationMode() {
        return rootJson.containsKey(KEY_CALIBRATION_MODE);
    }

    //Checks whether rootJson contains duplicate pin numbers (input and output)
    public boolean hasDuplicatePins() {
        List<Integer> pinList = new ArrayList<>();
        //Add output pin values
        if (getOutputPin(ServoType.AILERON) != -1) pinList.add(getOutputPin(ServoType.AILERON));
        if (getOutputPin(ServoType.ELEVATOR) != -1) pinList.add(getOutputPin(ServoType.ELEVATOR));
        if (getOutputPin(ServoType.RUDDER) != -1) pinList.add(getOutputPin(ServoType.RUDDER));
        if (getOutputPin(ServoType.THROTTLE) != -1) pinList.add(getOutputPin(ServoType.THROTTLE));

        //Add input pin values
        if (getInputPin(ServoType.AILERON) != -1) pinList.add(getInputPin(ServoType.AILERON));
        if (getInputPin(ServoType.ELEVATOR) != -1) pinList.add(getInputPin(ServoType.ELEVATOR));
        if (getInputPin(ServoType.RUDDER) != -1) pinList.add(getInputPin(ServoType.RUDDER));
        if (getInputPin(ServoType.THROTTLE) != -1) pinList.add(getInputPin(ServoType.THROTTLE));
        if (getInputPin(ServoType.CUTOVER) != -1) pinList.add(getInputPin(ServoType.CUTOVER));

        //Since sets do not allow duplicates, there are duplicates if the size of the set is smaller
        Set<Integer> pinSet = new HashSet<>(pinList);
        return pinSet.size() < pinList.size();
    }

    //Checks whether rootJson contains the error JSON key
    public boolean hasErrorMessage() {
        return rootJson.containsKey(KEY_ERROR);
    }

    //Checks whether rootJson contains a receiver input max value for a given ServoType
    public boolean hasInputMax(ServoType servoType) {
        return hasInputConfigValue(servoType, KEY_MAX);
    }

    //Checks whether rootJson contains a receiver input min value for a given ServoType
    public boolean hasInputMin(ServoType servoType) {
        return hasInputConfigValue(servoType, KEY_MIN);
    }

    //Checks whether rootJson contains a receiver input pin for a given ServoType
    public boolean hasInputPin(ServoType servoType) {
        return hasInputConfigValue(servoType, KEY_PIN);
    }

    //Checks whether rootJson contains all of the receiver input values set via calibration
    public boolean hasInputRanges() {
        return (hasInputRange(ServoType.AILERON) && hasInputRange(ServoType.CUTOVER)
                && hasInputRange(ServoType.ELEVATOR) && hasInputRange(ServoType.RUDDER)
                && hasInputRange(ServoType.THROTTLE));
    }

    //Checks whether rootJson contains an output max value for a given ServoType
    public boolean hasOutputMax(ServoType servoType) {
        return hasOutputConfigValue(servoType, KEY_MAX);
    }

    //Checks whether rootJson contains an output min value for a given ServoType
    public boolean hasOutputMin(ServoType servoType) {
        return hasOutputConfigValue(servoType, KEY_MIN);
    }

    //Checks whether rootJson contains an output pin for a given ServoType
    public boolean hasOutputPin(ServoType servoType) {
        return hasOutputConfigValue(servoType, KEY_PIN);
    }

    //Checks whether rootJson contains the receiverControl JSON key
    public boolean hasReceiverControl() {
        return rootJson.containsKey(KEY_RECEIVER_CONTROL);
    }

    public boolean hasReceiverOnly(ServoType servoType) {
        return hasInputConfigValue(servoType, KEY_RECEIVER_ONLY);
    }

    //Checks whether rootJson contains a value for a given ServoType
    public boolean hasServoValue(ServoType servoType) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            return servoJson.containsKey(KEY_VALUE);
        }
        return false;
    }

    //Checks whether rootJson contains any servo output values
    public boolean hasServoValue() {
        return (hasServoValue(ServoType.AILERON) || hasServoValue(ServoType.ELEVATOR)
                || hasServoValue(ServoType.RUDDER) || hasServoValue(ServoType.THROTTLE));
    }

    //Returns the value of the calibrationMode JSON key
    //Use hasCalibrationMode method to determine whether to use this method
    public boolean isCalibrationMode() {
        return (boolean) rootJson.get(KEY_CALIBRATION_MODE);
    }

    //Returns the value of the receiverControl JSON key
    //Use hasReceiverControl method to determine whether to use this method
    public boolean isReceiverControl() {
        return (boolean) rootJson.get(KEY_RECEIVER_CONTROL);
    }

    public boolean isReceiverOnly(ServoType servoType) {
        return (boolean) getInputConfigValue(servoType, KEY_RECEIVER_ONLY);
    }

    //Returns true if the ready status key value pair is present in the stored JSON object
    public boolean isStatusReady() {
        return rootJson.containsKey(KEY_STATUS)
                && rootJson.get(KEY_STATUS).equals(VALUE_STATUS_READY);
    }

    //Sets the calibrationMode JSON boolean via boolean parameter calibrationMode
    //If calibrationMode is true, the USB device listens for receiver input to determine input range
    //If calibrationMode is false, the USB device stops listening for input
    @SuppressWarnings("unchecked")
    public void setCalibrationMode(boolean calibrationMode) {
        rootJson.put(KEY_CALIBRATION_MODE, calibrationMode);
    }

    //Sets the control type for the given ServoType based the value of the receiverOnly parameter
    //If receiverOnly is true, only the transmitter/receiver can send commands to the servos
    // If receiveronly is false, the phone and transmitter/receiver share control of the plane
    @SuppressWarnings("unchecked")
    public void setInputControl(ServoType servoType, boolean receiverOnly) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_RECEIVER_ONLY, receiverOnly);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the receiver input pin for the given ServoType
    @SuppressWarnings("unchecked")
    public void setInputPin(ServoType servoType, int pinNumber) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_PIN, (long) pinNumber);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the min and max receiver input values for the given ServoType (in microseconds)
    @SuppressWarnings("unchecked")
    public void setInputRange(ServoType servoType, int intputMin, int inputMax) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_MAX, (long) inputMax);
        inputConfigJson.put(KEY_MIN, (long) intputMin);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the min and max output values for the given ServoType (in degrees)
    @SuppressWarnings("unchecked")
    public void setOutputRange(ServoType servoType, int outputMin, int outputMax) {
        JSONObject servoJson = new JSONObject();
        JSONObject outputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
            }
        }
        outputConfigJson.put(KEY_MAX, (long) outputMax);
        outputConfigJson.put(KEY_MIN, (long) outputMin);
        servoJson.put(KEY_OUTPUT_CONFIG, outputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the servo output pin for the given ServoType
    @SuppressWarnings("unchecked")
    public void setOutputPin(ServoType servoType, int pinNumber) {
        JSONObject servoJson = new JSONObject();
        JSONObject outputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
            }
        }
        outputConfigJson.put(KEY_PIN, (long) pinNumber);
        servoJson.put(KEY_OUTPUT_CONFIG, outputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the output value of the servo (in degrees)
    @SuppressWarnings("unchecked")
    public void setServoValue(ServoType servoType, int value) {
        JSONObject servoJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
        }
        servoJson.put(KEY_VALUE, (long) value);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Returns the input min, max, or pin (as specified by jsonMinMaxKey)
    private Object getInputConfigValue(ServoType servoType, String jsonKey) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                if (inputConfigJson.containsKey(jsonKey)) {
                    return inputConfigJson.get(jsonKey);
                }
            }
        }
        return null;
    }

    //Returns the output min, max, or pin (as specified by jsonMinMaxKey)
    private Number getOutputConfigValue(ServoType servoType, String jsonKey) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                JSONObject outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
                if (outputConfigJson.containsKey(jsonKey)) {
                    return (Number) outputConfigJson.get(jsonKey);
                }
            }
        }
        return null;
    }

    //Checks whether rootJson contains an inputConfig that contains a jsonKey for a given ServoType
    private boolean hasInputConfigValue(ServoType servoType, String jsonKey) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                return inputConfigJson.containsKey(jsonKey);
            }
        }
        return false;
    }

    //Checks whether rootJson contains receiver input min and max values for a given ServoType
    private boolean hasInputRange(ServoType servoType) {
        return hasInputConfigValue(servoType, KEY_MIN) && hasInputConfigValue(servoType, KEY_MAX);
    }

    //Checks whether rootJson contains an outputConfig that contains a jsonKey for a given ServoType
    private boolean hasOutputConfigValue(ServoType servoType, String jsonKey) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                JSONObject outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
                return outputConfigJson.containsKey(jsonKey);
            }
        }
        return false;
    }

    //Takes a ServoType and another (separate) rootJson object
    //Returns true if the input JSON object has the same config as the ServoType JSON of rootJSON
    private boolean servoEquals(JSONObject otherRootJson, ServoType servoType) {
        if (otherRootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) otherRootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                if (inputConfigJson.containsKey(KEY_MAX)) {
                    Number inputMax1 = (Number) inputConfigJson.get(KEY_MAX);
                    Number inputMax2 = (Number) getInputConfigValue(servoType, KEY_MAX);
                    if (inputMax1 != null && !inputMax1.equals(inputMax2)) return false;
                } else if (hasInputMax(servoType)) {
                    return false;
                }
                if (inputConfigJson.containsKey(KEY_MIN)) {
                    Number inputMin1 = (Number) inputConfigJson.get(KEY_MIN);
                    Number inputMin2 = (Number) getInputConfigValue(servoType, KEY_MIN);
                    if (inputMin1 != null && !inputMin1.equals(inputMin2)) return false;
                } else if (hasInputMin(servoType)) {
                    return false;
                }
                if (inputConfigJson.containsKey(KEY_PIN)) {
                    Number inputPin1 = (Number) inputConfigJson.get(KEY_PIN);
                    Number inputPin2 = (Number) getInputConfigValue(servoType, KEY_PIN);
                    if (inputPin1 != null && !inputPin1.equals(inputPin2)) return false;
                } else if (hasInputPin(servoType)) {
                    return false;
                }
                if (inputConfigJson.containsKey(KEY_RECEIVER_ONLY)) {
                    boolean receiverOnly1 = (boolean) inputConfigJson.get(KEY_RECEIVER_ONLY);
                    boolean receiverOnly2 = (boolean) getInputConfigValue(servoType,
                            KEY_RECEIVER_ONLY);
                    if (receiverOnly1 != receiverOnly2) return false;
                } else if (hasReceiverOnly(servoType)) {
                    return false;
                }
            } else if (hasInputRange(servoType) || hasInputPin(servoType)
                    || hasReceiverOnly(servoType)) {
                return false;
            }

            if (servoJson.containsKey(KEY_OUTPUT_CONFIG)) {
                JSONObject outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
                if (outputConfigJson.containsKey(KEY_MAX)) {
                    Number outputMax1 = (Number) outputConfigJson.get(KEY_MAX);
                    Number outputMax2 = getOutputConfigValue(servoType, KEY_MAX);
                    if (outputMax1 != null && !outputMax1.equals(outputMax2)) return false;
                } else if (hasOutputMax(servoType)) {
                    return false;
                }
                if (outputConfigJson.containsKey(KEY_MIN)) {
                    Number outputMin1 = (Number) outputConfigJson.get(KEY_MIN);
                    Number outputMin2 = getOutputConfigValue(servoType, KEY_MIN);
                    if (outputMin1 != null && !outputMin1.equals(outputMin2)) return false;
                } else if (hasOutputMin(servoType)) {
                    return false;
                }
                if (outputConfigJson.containsKey(KEY_PIN)) {
                    Number outputPin1 = (Number) outputConfigJson.get(KEY_PIN);
                    Number outputPin2 = getOutputConfigValue(servoType, KEY_PIN);
                    if (outputPin1 != null && !outputPin1.equals(outputPin2)) return false;
                } else if (hasOutputPin(servoType)) {
                    return false;
                }
            } else if (hasOutputMax(servoType) || hasOutputMin(servoType)
                    || hasOutputPin(servoType)) {
                return false;
            }

            if (servoJson.containsKey(KEY_VALUE)) {
                Number value1 = (Number) servoJson.get(KEY_VALUE);
                Number value2 = getServoValue(servoType);
                if (value1 != null && !value1.equals(value2)) return false;
            } else if (hasServoValue(servoType)) {
                return false;
            }

        } else if (rootJson.containsKey(servoType.getStringValue())) {
            return false;
        }
        return true;
    }

}