//Created by Ryan Staatz
//For RC receiver background, see: http://rcarduino.blogspot.com/2013/04/problem-reading-rc-channels-rcarduino.html

//Arduino JSON library: https://github.com/bblanchon/ArduinoJson
#include <ArduinoJson.h>
//PinChangeInt library: http://playground.arduino.cc/Main/PinChangeInt
#include <PinChangeInt.h>
#include <Servo.h>

//Max value for variable type byte
const byte BYTE_MAX = 255;

//Sets the max char array size for JSON serial input and output
const byte CHAR_ARRAY_SIZE = BYTE_MAX;

//The allowed margin from the cutover min/max input
const byte CUTOVER_MARGIN = 100;

//Number of cutover samples to use for cutoverInAverage
const byte CUTOVER_SAMPLE_SIZE = 10;

//Byte flags used to store whether there is an update from a given input
const byte FLAG_AILERON = 1;
const byte FLAG_ELEVATOR = 2;
const byte FLAG_RUDDER = 4;
const byte FLAG_THROTTLE = 8;
const byte FLAG_CUTOVER = 16;

//Min and max values for the Servo.write() function
const byte SERVO_MIN = 0;
const byte SERVO_MAX = 180;

//Cutover threshold in milliseconds
const int CUTOVER_THRESHOLD = 1500;

//Default servo input value
//TODO use better calibration default
const int RECEIVER_INPUT_DEFAULT = 1500;

//Allowed distance from the default for a valid receiver input value
const int RECEIVER_INPUT_DISTANCE = 800;

//Min and max possible values to filter for receiver servo input
const int RECEIVER_INPUT_MAX = RECEIVER_INPUT_DEFAULT + RECEIVER_INPUT_DISTANCE;
const int RECEIVER_INPUT_MIN = RECEIVER_INPUT_DEFAULT - RECEIVER_INPUT_DISTANCE;

//Sets the baud rate of the Arduino
const long BAUD_RATE = 115200;

//Used to store the byte flag values above
//Note this byte MUST be volatile since it is used in the main code and in the Interrupt Service Routine (ISR) methods
volatile byte receiverInputFlags;

//Unsigned ints used to store microsecond values
//Note: these ints MUST be volatile since they are used in the main code and in the ISR methods
volatile uint16_t aileronInValue;
volatile uint16_t cutoverInValue;
volatile uint16_t elevatorInValue;
volatile uint16_t rudderInValue;
volatile uint16_t throttleInValue;

//Determines if the servos are in the process of calibration
boolean calibrationMode = false;

//Determines if the calibration procedure has been performed
boolean isCalibrated = false;

//Determines if new Json data has been collected from serial input for processing
boolean newData = false;

//Determines if the plane is controlled only by the receiver
boolean receiverControl = false;

//Determines if the plane control is shared between the phone and the receiver
boolean sharedControl = false;

//All valid input/output must start/end with the corresponding chars below
char endMarker = '#';
char startMarker = '@';

//This array stores Json characters read from serial input
char receivedJson[CHAR_ARRAY_SIZE];

//The running average for cutover receiver input
uint16_t cutoverInputAverage = RECEIVER_INPUT_DEFAULT;

//The start time for determining the cutover double switch flip for emergency input shutoff
uint32_t receiverControlStartTime = millis();

struct ServoData {
  byte outputMax = SERVO_MAX;
  byte outputMin = SERVO_MIN;
  byte inputPin = BYTE_MAX;  //Receiver input pin for servo
  boolean receiverInputOnly = false; //Determines whether this servo only takes receiver input
  uint16_t inputMax = RECEIVER_INPUT_MIN;  //Calibrated receiver input max for servo
  uint16_t inputMin = RECEIVER_INPUT_MAX;  //Calibrated receiver input min for servo
  Servo servo;
} aileron, cutover, elevator, rudder, throttle;

void setup() {
  Serial.begin(BAUD_RATE);  //Set the baud rate
  while (!Serial) {}  //Wait for serial port to connect
  sendJsonString("status", "ready");  //Inform serial interface that the Arduino is ready
}

void loop() {
  readSerialJsonInput();
  processReceivedJson();
  processReceiverInput();
}

//Reads json serial input and stores input in a char array
//Most of this method code is taken from: https://forum.arduino.cc/index.php?topic=288234.0
void readSerialJsonInput() {
  static boolean receiveInProgress = false;  //Determines if a char received via serial input belongs to the current json input char array
  static byte index = 0;  //Index for storing the most recently received serial input in the correct place in the input char array
  char charIn;  //Char being currently read from serial input

  //Only attempt to read data when serial input is available
  while (Serial.available() > 0 && newData == false) {
    charIn = Serial.read();

    if (receiveInProgress == true) {
      if (charIn != endMarker) {
        receivedJson[index] = charIn;
        index++;
        if (index >= CHAR_ARRAY_SIZE) {
          index = CHAR_ARRAY_SIZE - 1;
          sendJsonString("error", "Serial buffer overflow");
        }
      }
      else {
        receivedJson[index] = '\0'; // terminate the string
        receiveInProgress = false;
        index = 0;
        newData = true;
      }

    } else if (charIn == startMarker) {
      receiveInProgress = true;
    }
  }
}

//Example JSON input below
//Complete configuration
//@{"aileron":{"inputConfig":{"max":1688,"min":1084,"pin":6,"receiverOnly":false},"outputConfig":{"max":180,"min":0,"pin":7},"value":90}}#
//@{"elevator":{"inputConfig":{"max":1752,"min":964,"pin":4,"receiverOnly":true},"outputConfig":{"max":180,"min":0,"pin":9},"value":90}}#
//@{"rudder":{"inputConfig":{"max":1800,"min":1044,"pin":3,"receiverOnly":true},"outputConfig":{"max":180,"min":0,"pin":8},"value":90}}#
//@{"throttle":{"inputConfig":{"max":1688,"min":1084,"pin":5,"receiverOnly":true},"outputConfig":{"max":180,"min":0,"pin":10},"value":90}}#
//@{"cutover":{"inputConfig":{"max":1844,"min":776,"pin":2}}}#

//Configuration without calibration or receiverOnly configuration
//@{"aileron":{"inputConfig":{"pin":6},"outputConfig":{"max":135,"min":45,"pin":7},"value":90}}#
//@{"elevator":{"inputConfig":{"pin":4},"outputConfig":{"max":180,"min":0,"pin":9},"value":90}}#
//@{"rudder":{"inputConfig":{"pin":3},"outputConfig":{"max":180,"min":0,"pin":8},"value":90}}#
//@{"throttle":{"inputConfig":{"pin":5},"outputConfig":{"max":124,"min":32,"pin":10},"value":32}}#
//@{"cutover":{"inputConfig":{"pin":2}}}#

//Receiver calibration data (sent or received)
//@{"aileron":{"inputConfig":{"max":1832,"min":780}},"cutover":{"inputConfig":{"max":1836,"min":756}},"elevator":{"inputConfig":{"max":1644,"min":1248}},"rudder":{"inputConfig":{"max":1612,"min":980}},"throttle":{"inputConfig":{"max":1708,"min":1016}}}#

//Neutral servo command
//@{"aileron":{"value":90},"elevator":{"value":90},"rudder":{"value":90},"throttle":{"value":32}}#

//Processes JSON received in the readSerialJsonInput() method
void processReceivedJson() {
  if (newData) {
    StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
    JsonObject& root = jsonBuffer.parseObject(receivedJson);

    //If the JSON object failed to parse, inform the serial interface and return
    if (!root.success()) {
      sendJsonString("error", "Parsing JSON failed");
      newData = false;
      return;
    }

    //If status is requested, send status check response
    if (root.containsKey("request")) {
      const char* request = root["request"];
      if (strcmp(request, "status") == 0) sendJsonString("status", "ready");
    }

    //Enable or disable receiver control
    if (root.containsKey("receiverControl")) {
      if (isCalibrated) {
        receiverControl = (boolean) root["receiverControl"];
        sendJsonBoolean("receiverControl", receiverControl);
      } else {
        sendJsonString("error", "Calibration required");
      }
    }

    //Enable or disable calibration mode
    if (root.containsKey("calibrationMode")) {
      if (!receiverControl) {
        calibrationMode = (boolean) root["calibrationMode"];
        sendJsonBoolean("calibrationMode", calibrationMode);
        if (!calibrationMode) {
          if (calibrationComplete()){
            sendCalibrationJsonData();
          } else {
            sendJsonString("error", "Calibration not complete");
          }
        }
        //Reset the calibration values (they will later be sent back from the attached USB device)
        resetCalibration();
      } else {
        sendJsonString("error", "Disable receiver control to change calibration mode");
      }
    }

    //Assigns pin numbers to the motor, servos, and receiver inputs based on the received JSON
    if (root.containsKey("aileron")) {
      processServoJson(root["aileron"], aileron, "aileron", listenForAileron, "Unassigned aileron pin");
    }
    if (root.containsKey("elevator")) {
      processServoJson(root["elevator"], elevator, "elevator", listenForElevator, "Unassigned elevator pin");
    }
    if (root.containsKey("rudder")) {
      processServoJson(root["rudder"], rudder, "rudder", listenForRudder, "Unassigned rudder pin");
    }
    if (root.containsKey("throttle")) {
      processServoJson(root["throttle"], throttle, "throttle", listenForThrottle, "Unassigned throttle pin");
    }
    if (root.containsKey("cutover")) {
      //Cutover is input only, so output-only function parameters can be null
      processServoJson(root["cutover"], cutover, "", listenForCutover, "");
    }

    //If the receiver inputs are not yet calibrated, but calibration values were just received, set isCalibrated to true
    if (!isCalibrated && calibrationComplete()) isCalibrated = true;

    //Reset newData to false so we can read new serial input
    newData = false;
  }
}

//Processes input from the receiver
void processReceiverInput() {
  //Static variables are used because they maintain their value over multiple function calls
  static byte receiverInputFlagsLocal;
  static uint16_t aileronInValueLocal;
  static uint16_t cutoverInValueLocal;
  static uint16_t elevatorInValueLocal;
  static uint16_t rudderInValueLocal;
  static uint16_t throttleInValueLocal;

  //If any servo input byte flags were set in any of the ISR methods, process them here
  if (receiverInputFlags != 0) {
    noInterrupts();
    //While interrupts are paused, copy new servo input values to the corresponding local static variables
    receiverInputFlagsLocal = receiverInputFlags;
    if (receiverInputFlagsLocal & FLAG_CUTOVER) cutoverInValueLocal = cutoverInValue;
      if (receiverControl || calibrationMode) {
        if (receiverInputFlagsLocal & FLAG_AILERON) aileronInValueLocal = aileronInValue;
        if (receiverInputFlagsLocal & FLAG_ELEVATOR) elevatorInValueLocal = elevatorInValue;
        if (receiverInputFlagsLocal & FLAG_RUDDER) rudderInValueLocal = rudderInValue;
        if (receiverInputFlagsLocal & FLAG_THROTTLE) throttleInValueLocal = throttleInValue;
      }
    receiverInputFlags = 0;
    interrupts();
  }
  //If the cutover switch is flipped, set receiverControl accordingly
  if (receiverInputFlagsLocal & FLAG_CUTOVER) {
    //Only use cutover values within the set range for receiver input
    if (calibrationMode && isValidReceiverInput(cutoverInValueLocal)) {
      if (cutoverInValueLocal > cutover.inputMax) cutover.inputMax = cutoverInValueLocal;
      if (cutoverInValueLocal < cutover.inputMin) cutover.inputMin = cutoverInValueLocal;
    } else if (isCalibrated) {
      //Calculate weighted average for cutover
      cutoverInputAverage = (cutoverInValueLocal + (CUTOVER_SAMPLE_SIZE - 1)*cutoverInputAverage)/CUTOVER_SAMPLE_SIZE;
      if (isValidReceiverInput(cutoverInputAverage)) {
        if (receiverControl && cutoverInputAverage > (cutover.inputMax - CUTOVER_MARGIN)) {
          receiverControl = false;
          //Checks if the cutover switched back and forth slower than the threshold (e.g. not emergency input stop)
          if ((millis() - receiverControlStartTime) > CUTOVER_THRESHOLD) {
            sharedControl = true;
            sendJsonBoolean("sharedControl", true);
          }
          sendJsonBoolean("receiverControl", receiverControl);
        } else if (!receiverControl && cutoverInputAverage < (cutover.inputMin + CUTOVER_MARGIN)) {
          sharedControl = false;
          receiverControl = true;
          sendJsonBoolean("receiverControl", receiverControl);
          sendJsonBoolean("sharedControl", false);
          receiverControlStartTime = millis();
        }
      } else if (receiverControl) {
        sendJsonString("error", "Cutover input out of range");
      }
    }
  }

  //Only process servo input if receiverControl or calibrationMode are enabled
  if (receiverControl || calibrationMode) {
    //Process input for each servo that received new input from the receiver
    if (receiverInputFlagsLocal & FLAG_AILERON) {
      processServoInput(aileron, aileronInValueLocal, "aileron", "Unassigned aileron pin", "Aileron input out of range");
    }
    if (receiverInputFlagsLocal & FLAG_ELEVATOR) {
      processServoInput(elevator, elevatorInValueLocal, "elevator", "Unassigned elevator pin", "Elevator input out of range");
    }
    if (receiverInputFlagsLocal & FLAG_RUDDER) {
      processServoInput(rudder, rudderInValueLocal, "rudder", "Unassigned rudder pin", "Rudder input out of range");
    }
    if (receiverInputFlagsLocal & FLAG_THROTTLE) {
      processServoInput(throttle, throttleInValueLocal, "throttle", "Unassigned throttle pin", "Throttle input out of range");
    }
  }
}

//Takes a key (text) value (boolean) pair and sends them as JSON over the serial interface
void sendJsonBoolean(const char key[], boolean value) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key value (char array) pair and sends them as JSON over the serial interface
void sendJsonString(const char key[], const char value[]) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key (text) value (int) pair and sends them as JSON over the serial interface
void sendJsonServoOutput(const char key[], byte value) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  JsonObject& servoJson = jsonBuffer.createObject();
  servoJson["value"] = value;
  root[key] = servoJson;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Sends all calibrated receiver input max and min values as JSON over the serial interface
void sendCalibrationJsonData() {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();

  JsonObject& aileronJson = jsonBuffer.createObject();
  JsonObject& cutoverJson = jsonBuffer.createObject();
  JsonObject& elevatorJson = jsonBuffer.createObject();
  JsonObject& rudderJson = jsonBuffer.createObject();
  JsonObject& throttleJson = jsonBuffer.createObject();
  
  JsonObject& aileronInputJson = jsonBuffer.createObject();
  JsonObject& cutoverInputJson = jsonBuffer.createObject();
  JsonObject& elevatorInputJson = jsonBuffer.createObject();
  JsonObject& rudderInputJson = jsonBuffer.createObject();
  JsonObject& throttleInputJson = jsonBuffer.createObject();

  aileronInputJson["max"] = aileron.inputMax;
  cutoverInputJson["max"] = cutover.inputMax;
  elevatorInputJson["max"] = elevator.inputMax;
  rudderInputJson["max"] = rudder.inputMax;
  throttleInputJson["max"] = throttle.inputMax;

  aileronInputJson["min"] = aileron.inputMin;
  cutoverInputJson["min"] = cutover.inputMin;
  elevatorInputJson["min"] = elevator.inputMin;
  rudderInputJson["min"] = rudder.inputMin;
  throttleInputJson["min"] = throttle.inputMin;

  aileronJson["inputConfig"] = aileronInputJson;
  cutoverJson["inputConfig"] = cutoverInputJson;
  elevatorJson["inputConfig"] = elevatorInputJson;
  rudderJson["inputConfig"] = rudderInputJson;
  throttleJson["inputConfig"] = throttleInputJson;

  root["aileron"] = aileronJson;
  root["cutover"] = cutoverJson;
  root["elevator"] = elevatorJson;
  root["rudder"] = rudderJson;
  root["throttle"] = throttleJson;

  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Checks if the receiver inputs have been calibrated
boolean calibrationComplete() {
  if (aileron.inputMax == RECEIVER_INPUT_MIN) return false;
  if (aileron.inputMin == RECEIVER_INPUT_MAX) return false;
  if (elevator.inputMax == RECEIVER_INPUT_MIN) return false;
  if (elevator.inputMin == RECEIVER_INPUT_MAX) return false;
  if (rudder.inputMax == RECEIVER_INPUT_MIN) return false;
  if (rudder.inputMin == RECEIVER_INPUT_MAX) return false;
  if (throttle.inputMax == RECEIVER_INPUT_MIN) return false;
  if (throttle.inputMin == RECEIVER_INPUT_MAX) return false;
  if (cutover.inputMax == RECEIVER_INPUT_MIN) return false;
  if (cutover.inputMin == RECEIVER_INPUT_MAX) return false;
  return true;
}

//Reset all calibrated receiver input values to the default
void resetCalibration() {
  isCalibrated = false;
  aileron.inputMax = RECEIVER_INPUT_MIN;
  aileron.inputMin = RECEIVER_INPUT_MAX;
  elevator.inputMax = RECEIVER_INPUT_MIN;
  elevator.inputMin = RECEIVER_INPUT_MAX;
  rudder.inputMax = RECEIVER_INPUT_MIN;
  rudder.inputMin = RECEIVER_INPUT_MAX;
  throttle.inputMax = RECEIVER_INPUT_MIN;
  throttle.inputMin = RECEIVER_INPUT_MAX;
  cutover.inputMax = RECEIVER_INPUT_MIN;
  cutover.inputMin= RECEIVER_INPUT_MAX;
}

//Processes received JSON for a given servo (e.g. ServoData struct)
//The inputISR is the interrupt service routine function for triggering action upon receiver input
void processServoJson(JsonObject& servoJson, ServoData& servoData, const char servoType[], void inputISR(), const char servoError[]) {
  if (!receiverControl) {
    //Check for any output configuration parameters for this servo
    if (servoJson.containsKey("outputConfig")) {
      JsonObject& outputConfig = servoJson["outputConfig"];
      if (outputConfig.containsKey("max")) {
        servoData.outputMax = (byte) outputConfig["max"];
      }
      if (outputConfig.containsKey("min")) {
        servoData.outputMin = (byte) outputConfig["min"];
      }
      if (outputConfig.containsKey("pin")) {
        if (servoData.servo.attached()) servoData.servo.detach();
        byte servoOutPin = (byte) outputConfig["pin"];
        servoData.servo.attach(servoOutPin);
      }
    }
    //Check for any input configuration parameters for this servo
    if (servoJson.containsKey("inputConfig")) {
      JsonObject& inputConfig = servoJson["inputConfig"];
      if (inputConfig.containsKey("max")) {
        servoData.inputMax = (uint16_t) inputConfig["max"];
      }
      if (inputConfig.containsKey("min")) {
        servoData.inputMin = (uint16_t) inputConfig["min"];
      }
      if (inputConfig.containsKey("pin")) {
        if (servoData.inputPin != BYTE_MAX) PCintPort::detachInterrupt(servoData.inputPin);
        servoData.inputPin = (byte) inputConfig["pin"];
        PCintPort::attachInterrupt(servoData.inputPin, inputISR, CHANGE);
      }
      if (inputConfig.containsKey("receiverOnly")) {
        servoData.receiverInputOnly = (boolean) inputConfig["receiverOnly"];
      }
    }
    //Only write to servo if the phone input for this servo has permission to do so
    if (!servoData.receiverInputOnly) {
      //Check for any servo output values to write to the servo
      if (servoJson.containsKey("value")) {
        if (servoData.servo.attached()) {
          byte value = (byte) servoJson["value"];
          servoData.servo.write(value);
          sendJsonServoOutput(servoType, value);
        } else {
          sendJsonString("error", servoError);
        }
      }
    }
  } else {
    sendJsonString("error", "Disable receiver control to configure servos");
  }
}

//Interrupt Service Routine (ISR) for aileron receiver input
void listenForAileron() {
  static uint32_t aileronInStart;
  if (PCintPort::pinState) {
    aileronInStart = micros();
  } else {
    aileronInValue = (uint16_t) (micros() - aileronInStart);
    receiverInputFlags |= FLAG_AILERON;
  }
}

//Interrupt Service Routine (ISR) for cutover input
void listenForCutover() {
  static uint32_t cutoverInStart;
  if (PCintPort::pinState) {
    cutoverInStart = micros();
  } else {
    cutoverInValue = (uint16_t) (micros() - cutoverInStart);
    receiverInputFlags |= FLAG_CUTOVER;
  }
}

//Interrupt Service Routine (ISR) for elevator receiver input
void listenForElevator() {
  static uint32_t elevatorInStart;
  if (PCintPort::pinState) {
    elevatorInStart = micros();
  } else {
    elevatorInValue = (uint16_t) (micros() - elevatorInStart);
    receiverInputFlags |= FLAG_ELEVATOR;
  }
}

//Interrupt Service Routine (ISR) for rudder receiver input
void listenForRudder() {
  static uint32_t rudderInStart;
  if (PCintPort::pinState) {
    rudderInStart = micros();
  } else {
    rudderInValue = (uint16_t) (micros() - rudderInStart);
    receiverInputFlags |= FLAG_RUDDER;
  }
}

//Interrupt Service Routine (ISR) for throttle receiver input
void listenForThrottle() {
  static uint32_t throttleInStart;
  if (PCintPort::pinState) {
    throttleInStart = micros();
  } else {
    throttleInValue = (uint16_t) (micros() - throttleInStart);
    receiverInputFlags |= FLAG_THROTTLE;
  }
}

//Determines whether a given value is valid receiver input
boolean isValidReceiverInput(uint16_t receiverInput) {
  return receiverInput < RECEIVER_INPUT_MAX && receiverInput > RECEIVER_INPUT_MIN;
}

//Processes receiver input for a given servo and writes the converted output to the servo
void processServoInput(ServoData& servoData, uint16_t servoInputValue, const char servoType[], const char servoError[], const char servoInputError[]) {
  //Only use receiver values within the set range for receiver input
  if (isValidReceiverInput(servoInputValue)) {
    if (calibrationMode) {
      if (servoInputValue > servoData.inputMax) servoData.inputMax = servoInputValue;
      if (servoInputValue < servoData.inputMin) servoData.inputMin = servoInputValue;
    //Only write to servo if the receiver input for this servo has permission to do so
    } else if (receiverControl || (sharedControl && servoData.receiverInputOnly)) {
      if (servoData.servo.attached()) {
        //Constrain the servo input value to be within the calibration range (since mapping won't constrain it)
        servoInputValue = constrain(servoInputValue, servoData.inputMin, servoData.inputMax);
        //The receiverInput (in microseconds) is mapped to the min and max for the Servo.write function (degrees)
        byte convertedServoValue = map(servoInputValue, servoData.inputMin, servoData.inputMax, servoData.outputMin, servoData.outputMax);
        servoData.servo.write(convertedServoValue);
        sendJsonServoOutput(servoType, convertedServoValue);
      } else {
        sendJsonString("error", servoError);
      }
    }
  } else {
    sendJsonString("error", servoInputError);
  }
}
