//Created by Ryan Staatz
//For RC receiver background, see: http://rcarduino.blogspot.com/2013/04/problem-reading-rc-channels-rcarduino.html

//Arduino JSON library: https://github.com/bblanchon/ArduinoJson
#include <ArduinoJson.h>
//PinChangeInt library: http://playground.arduino.cc/Main/PinChangeInt
#include <PinChangeInt.h>
#include <Servo.h>

//Pulse threshold values for receiver input for cutover between auto and manual control
const byte CUTOVER_BUFFER = 150;

//Byte flags used to store whether there is an update from a given input
const byte FLAG_AILERON = 1;
const byte FLAG_ELEVATOR = 2;
const byte FLAG_RUDDER = 4;
const byte FLAG_THROTTLE = 8;
const byte FLAG_CUTOVER = 16;

//Min and max values for the Servo.write() function
const byte SERVO_MIN = 0;
const byte SERVO_MAX = 180;

//Sets the max char array size for JSON serial input and output
const int CHAR_ARRAY_SIZE = 300;

//Default servo input value
//TODO use better calibration default
const int RECEIVER_INPUT_DEFAULT = 1500;

//Min and max possible values to filter for receiver servo input
const int RECEIVER_INPUT_MAX = 2300;
const int RECEIVER_INPUT_MIN = 700;

//Sets the baud rate of the Arduino
const long BAUD_RATE = 115200;

//Used to store the byte flag values above
//Note this byte MUST be volatile since it is used in the main code and in the Interrupt Service Routine (ISR) methods
volatile byte servoInputFlags;

//Unsigned ints used to store microsecond values
//Note: these ints MUST be volatile since they are used in the main code and in the ISR methods
volatile uint16_t aileronInValue;
volatile uint16_t cutoverInValue;
volatile uint16_t elevatorInValue;
volatile uint16_t rudderInValue;
volatile uint16_t throttleInValue;

//Stores the pin number for each receiver input
int aileronInPin = -1;
int cutoverInPin = -1;
int elevatorInPin = -1;
int rudderInPin = -1;
int throttleInPin = -1;

//Determines if the servos are in the process of calibration
boolean calibrationMode = false;

//Determines if the calibration procedure has been performed
boolean isCalibrated = false;

//Determines if new Json data has been collected from serial input for processing
boolean newData = false;

//Determines if the plane is controlled manually (remote control) or via autopilot (phone)
boolean manualControl = false;

//All valid input/output must start/end with the corresponding chars below
char endMarker = '#';
char startMarker = '@';

//This array stores Json characters read from serial input
char receivedJson[CHAR_ARRAY_SIZE];

//Calibrated min and max for all receiver input (in microseconds)
uint16_t aileronMax = RECEIVER_INPUT_DEFAULT;
uint16_t aileronMin = RECEIVER_INPUT_DEFAULT;
uint16_t cutoverMax = RECEIVER_INPUT_DEFAULT;
uint16_t cutoverMin = RECEIVER_INPUT_DEFAULT;
uint16_t elevatorMax = RECEIVER_INPUT_DEFAULT;
uint16_t elevatorMin = RECEIVER_INPUT_DEFAULT;
uint16_t rudderMax = RECEIVER_INPUT_DEFAULT;
uint16_t rudderMin = RECEIVER_INPUT_DEFAULT;
uint16_t throttleMax = RECEIVER_INPUT_DEFAULT;
uint16_t throttleMin = RECEIVER_INPUT_DEFAULT;

//Servos
Servo aileron;
Servo elevator;
Servo rudder;
Servo throttle;

void setup() {
  Serial.begin(BAUD_RATE);  //Set the baud rate
  while (!Serial) {}  //Wait for serial port to connect
  sendJson("status", "ready");  //Inform serial interface that the Arduino is ready
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
  static int index = 0;  //Index for storing the most recently received serial input in the correct place in the input char array
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
          sendJson("error", "Serial buffer overflow");
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

//Example JSON strings below:
//@{"config":{"throttle":10, "elevator":9, "rudder":8, "aileron":7, "aileronIn":6, "throttleIn":5, "elevatorIn":4, "rudderIn":3, "cutoverIn":2}, "throttle": 32, "elevator": 90, "rudder": 90, "aileron": 90}#
//@{"throttle":0, "elevator":100, "aileron":120, "rudder":75}#
void processReceivedJson() {
  if (newData) {
    StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
    JsonObject& root = jsonBuffer.parseObject(receivedJson);

    //If the JSON object failed to parse, inform the serial interface and return
    if (!root.success()) {
      sendJson("error", "Parsing JSON failed");
      newData = false;
      return;
    }

    //If status is requested, send status check response
    if (root.containsKey("request")) {
      const char* request = root["request"];
      if (strcmp(request, "status") == 0) sendJson("status", "ready");      
    }

    //Enable or disable manual control
    if (root.containsKey("manualControl")) {
      if (isCalibrated) {
        manualControl = (boolean) root["manualControl"];
        sendJson("manualControl", manualControl);
      } else {
        sendJson("error", "Receiver inputs require calibration");
      }
    }

    //Enable or disable calibration mode
    if (root.containsKey("calibrationMode")) {
      calibrationMode = (boolean) root["calibrationMode"];
      sendJson("calibrationMode", calibrationMode);
      if (calibrationMode) {
        resetCalibration();
        isCalibrated = false;
      } else {
        if (calibrationComplete()) isCalibrated = true;
        sendCalibrationJsonData();
      }
    }

    //Assigns pin numbers to the motor, servos, and receiver inputs based on the received JSON
    if (root.containsKey("config")) {
      if (!manualControl) {
        JsonObject& config = root["config"];
        if (config.containsKey("aileron")) {
          if (aileron.attached()) aileron.detach();
          byte aileronOutPin = (byte) config["aileron"];
          aileron.attach(aileronOutPin);
        }
        if (config.containsKey("elevator")) {
          if (elevator.attached()) elevator.detach();
          byte elevatorOutPin = (byte) config["elevator"];
          elevator.attach(elevatorOutPin);
        }
        if (config.containsKey("rudder")) {
          if (rudder.attached()) rudder.detach();
          byte rudderOutPin = (byte) config["rudder"];
          rudder.attach(rudderOutPin);
        }
        if (config.containsKey("throttle")) {
          if (throttle.attached()) throttle.detach();
          byte throttleOutPin = (byte) config["throttle"];
          throttle.attach(throttleOutPin);
        }
        if (config.containsKey("aileronIn")) {
          if (aileronInPin != -1) PCintPort::detachInterrupt(aileronInPin);
          aileronInPin = (int) config["aileronIn"];
          PCintPort::attachInterrupt(aileronInPin, listenForAileron, CHANGE);
        }
        if (config.containsKey("cutoverIn")) {
          if (cutoverInPin != -1) PCintPort::detachInterrupt(cutoverInPin);
          cutoverInPin = (int) config["cutoverIn"];
          PCintPort::attachInterrupt(cutoverInPin, listenForCutover, CHANGE);
        }
        if (config.containsKey("elevatorIn")) {
          if (elevatorInPin != -1) PCintPort::detachInterrupt(elevatorInPin);
          elevatorInPin = (int) config["elevatorIn"];
          PCintPort::attachInterrupt(elevatorInPin, listenForElevator, CHANGE);
        }
        if (config.containsKey("rudderIn")) {
          if (rudderInPin != -1) PCintPort::detachInterrupt(rudderInPin);
          rudderInPin = (int) config["rudderIn"];
          PCintPort::attachInterrupt(rudderInPin, listenForRudder, CHANGE);
        }
        if (config.containsKey("throttleIn")) {
          if (throttleInPin != -1) PCintPort::detachInterrupt(throttleInPin);
          throttleInPin = (int) config["throttleIn"];
          PCintPort::attachInterrupt(throttleInPin, listenForThrottle, CHANGE);
        }
      } else {
        sendJson("error", "Manual control must be disabled to configure servos");
      }
    }

    //Set the servo values according to the received JSON
    if (root.containsKey("servos")) {
      if (!manualControl) {
        JsonObject& servos = root["servos"];
        if (servos.containsKey("aileron")) {
          if (aileron.attached()) {
            byte aileronValue = root["aileron"];
            aileron.write(aileronValue);
            sendJson("aileron", aileronValue);
          } else {
            sendJson("error", "Unassigned aileron pin");
          }
        }
        if (servos.containsKey("elevator")) {
          if (elevator.attached()) {
            byte elevatorValue = root["elevator"];
            elevator.write(elevatorValue);
            sendJson("elevator", elevatorValue);
          } else {
            sendJson("error", "Unassigned elevator pin");
          }
        }
        if (servos.containsKey("rudder")) {
          if (rudder.attached()) {
            byte rudderValue = root["rudder"];
            rudder.write(rudderValue);
            sendJson("rudder", rudderValue);
          } else {
            sendJson("error", "Unassigned rudder pin");
          }
        }
        if (servos.containsKey("throttle")) {
          if (throttle.attached()) {
            byte throttleValue = root["throttle"];
            throttle.write(throttleValue);
            sendJson("throttle", throttleValue);
          } else {
            sendJson("error", "Unassigned throttle pin");
          }
        }
      } else {
        sendJson("error", "Manual control must be disabled to use servos");
      }
    }
    //Reset newData to false so we can read new serial input
    newData = false;
  }
}

//Processes input from the receiver
void processReceiverInput() {
  //Static variables are used because they maintain their value over multiple function calls
  static byte servoInputFlagsLocal;
  static uint16_t aileronInValueLocal;
  static uint16_t elevatorInValueLocal;
  static uint16_t rudderInValueLocal;
  static uint16_t throttleInValueLocal;
  static uint16_t cutoverInValueLocal;

  //If any servo input byte flags were set in any of the ISR methods, process them here
  if (servoInputFlags != 0) {
    noInterrupts();
    //While interrupts are paused, copy new servo input values to the corresponding local static variables
    servoInputFlagsLocal = servoInputFlags;
    if (servoInputFlagsLocal & FLAG_CUTOVER) cutoverInValueLocal = cutoverInValue;
      if (manualControl || calibrationMode) {
        if (servoInputFlagsLocal & FLAG_AILERON) aileronInValueLocal = aileronInValue;
        if (servoInputFlagsLocal & FLAG_ELEVATOR) elevatorInValueLocal = elevatorInValue;
        if (servoInputFlagsLocal & FLAG_RUDDER) rudderInValueLocal = rudderInValue;
        if (servoInputFlagsLocal & FLAG_THROTTLE) throttleInValueLocal = throttleInValue;
      }
    servoInputFlags = 0;
    interrupts();
  }
  //If the cutover switch is flipped, set manualControl accordingly
  if (servoInputFlagsLocal & FLAG_CUTOVER) {
    //Only use cutover values within the set range for receiver input
    if (isValidReceiverInput(cutoverInValueLocal)) {
      if (calibrationMode) {
        if (cutoverInValueLocal > cutoverMax) cutoverMax = cutoverInValueLocal;
        if (cutoverInValueLocal < cutoverMin) cutoverMin = cutoverInValueLocal;
      } else if (isCalibrated) {
        if (manualControl && cutoverInValueLocal > (cutoverMax - CUTOVER_BUFFER)) {
          manualControl = false;
          sendJson("manualControl", manualControl);
        } else if (!manualControl && cutoverInValueLocal < (cutoverMin + CUTOVER_BUFFER)) {
          manualControl = true;
          sendJson("manualControl", manualControl);
        }
      }
    } else {
      sendJson("error", "Cutover input value out of range");
    }
  }
  //Process input for each servo that received new input from the receiver
  if (servoInputFlagsLocal & FLAG_AILERON) {
    //Only use aileron values within the set range for receiver input
    if (isValidReceiverInput(aileronInValueLocal)) {
      if (calibrationMode) {
        if (aileronInValueLocal > aileronMax) aileronMax = aileronInValueLocal;
        if (aileronInValueLocal < aileronMin) aileronMin = aileronInValueLocal;
      } else if (manualControl) {
        processServoInput(aileron, aileronInValueLocal, "aileron", "Unassigned aileron pin");
      }
    } else {
      sendJson("error", "Aileron input value out of range");
    }
  }
  if (servoInputFlagsLocal & FLAG_ELEVATOR) {
    //Only use elevator values within the set range for receiver input
    if (isValidReceiverInput(elevatorInValueLocal)) {
      if (calibrationMode) {
        if (elevatorInValueLocal > elevatorMax) elevatorMax = elevatorInValueLocal;
        if (elevatorInValueLocal < elevatorMin) elevatorMin = elevatorInValueLocal;
      } else if (manualControl) {
        processServoInput(elevator, elevatorInValueLocal, "elevator", "Unassigned elevator pin");
      }
    } else {
      sendJson("error", "Elevator input value out of range");
    }
  }
  if (servoInputFlagsLocal & FLAG_RUDDER) {
  //Only use rudder values within the set range for receiver input
    if (isValidReceiverInput(rudderInValueLocal)) {
      if (calibrationMode) {
        if (rudderInValueLocal > rudderMax) rudderMax = rudderInValueLocal;
        if (rudderInValueLocal < rudderMin) rudderMin = rudderInValueLocal;
      } else if (manualControl) {
        processServoInput(rudder, rudderInValueLocal, "rudder", "Unassigned rudder pin");
      }
    } else {
      sendJson("error", "Rudder input value out of range");
    }
  }
  if (servoInputFlagsLocal & FLAG_THROTTLE) {
    //Only use throttle values within the set range for receiver input
    if (isValidReceiverInput(throttleInValueLocal)) {
      if (calibrationMode) {
        if (throttleInValueLocal > throttleMax) throttleMax = throttleInValueLocal;
        if (throttleInValueLocal < throttleMin) throttleMin = throttleInValueLocal;
      } else if (manualControl) {
        processServoInput(throttle, throttleInValueLocal, "throttle", "Unassigned throttle pin");
      }
    } else {
      sendJson("error", "Throttle input value out of range");
    }
  }
}

//Takes a key (text) value (boolean) pair and sends them as JSON over the serial interface
void sendJson(const char key[], boolean value) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key value (char array) pair and sends them as JSON over the serial interface
void sendJson(const char key[], const char value[]) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key (text) value (int) pair and sends them as JSON over the serial interface
void sendJson(const char key[], byte value) {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Sends all calibrated receiver input max and min values as JSON over the serial interface
void sendCalibrationJsonData() {
  StaticJsonBuffer<CHAR_ARRAY_SIZE> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  JsonObject &calibrationData = jsonBuffer.createObject();;
  calibrationData["aileronMin"] = aileronMin;
  calibrationData["aileronMax"] = aileronMax;
  calibrationData["elevatorMin"] = elevatorMin;
  calibrationData["elevatorMax"] = elevatorMax;
  calibrationData["rudderMin"] = rudderMin;
  calibrationData["rudderMax"] = rudderMax;
  calibrationData["throttleMin"] = throttleMin;
  calibrationData["throttleMax"] = throttleMax;
  calibrationData["cutoverMin"] = cutoverMin;
  calibrationData["cutoverMax"] = cutoverMax;
  root["calibrationData"] = calibrationData;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Checks if the receiver inputs have been calibrated
//TODO use better verification method
boolean calibrationComplete() {
  if (aileronMax == RECEIVER_INPUT_DEFAULT) return false;
  if (aileronMin == RECEIVER_INPUT_DEFAULT) return false;
  if (elevatorMax == RECEIVER_INPUT_DEFAULT) return false;
  if (elevatorMin == RECEIVER_INPUT_DEFAULT) return false;
  if (rudderMax == RECEIVER_INPUT_DEFAULT) return false;
  if (rudderMin == RECEIVER_INPUT_DEFAULT) return false;
  if (throttleMax == RECEIVER_INPUT_DEFAULT) return false;
  if (throttleMin == RECEIVER_INPUT_DEFAULT) return false;
  if (cutoverMax == RECEIVER_INPUT_DEFAULT) return false;
  if (cutoverMin == RECEIVER_INPUT_DEFAULT) return false;
  return true;
}

//Reset all calibrated receiver input values to the default
void resetCalibration() {
  aileronMax = RECEIVER_INPUT_DEFAULT;
  aileronMin = RECEIVER_INPUT_DEFAULT;
  elevatorMax = RECEIVER_INPUT_DEFAULT;
  elevatorMin = RECEIVER_INPUT_DEFAULT;
  rudderMax = RECEIVER_INPUT_DEFAULT;
  rudderMin = RECEIVER_INPUT_DEFAULT;
  throttleMax = RECEIVER_INPUT_DEFAULT;
  throttleMin = RECEIVER_INPUT_DEFAULT;
  cutoverMax = RECEIVER_INPUT_DEFAULT;
  cutoverMin = RECEIVER_INPUT_DEFAULT;
}

//Interrupt Service Routine (ISR) for aileron receiver input
void listenForAileron() {
  static uint32_t aileronInStart;
  if (PCintPort::pinState) {
    aileronInStart = micros();
  } else {
    aileronInValue = (uint16_t) (micros() - aileronInStart);
    servoInputFlags |= FLAG_AILERON;
  }
}

//Interrupt Service Routine (ISR) for cutover input
void listenForCutover() {
  static uint32_t cutoverInStart;
  if (PCintPort::pinState) {
    cutoverInStart = micros();
  } else {
    cutoverInValue = (uint16_t) (micros() - cutoverInStart);
    servoInputFlags |= FLAG_CUTOVER;
  }
}

//Interrupt Service Routine (ISR) for elevator receiver input
void listenForElevator() {
  static uint32_t elevatorInStart;
  if (PCintPort::pinState) {
    elevatorInStart = micros();
  } else {
    elevatorInValue = (uint16_t) (micros() - elevatorInStart);
    servoInputFlags |= FLAG_ELEVATOR;
  }
}

//Interrupt Service Routine (ISR) for rudder receiver input
void listenForRudder() {
  static uint32_t rudderInStart;
  if (PCintPort::pinState) {
    rudderInStart = micros();
  } else {
    rudderInValue = (uint16_t) (micros() - rudderInStart);
    servoInputFlags |= FLAG_RUDDER;
  }
}

//Interrupt Service Routine (ISR) for throttle receiver input
void listenForThrottle() {
  static uint32_t throttleInStart;
  if (PCintPort::pinState) {
    throttleInStart = micros();
  } else {
    throttleInValue = (uint16_t) (micros() - throttleInStart);
    servoInputFlags |= FLAG_THROTTLE;
  }
}

//Determines whether a given value is valid receiver input
boolean isValidReceiverInput(uint16_t receiverInput) {
  if (receiverInput < RECEIVER_INPUT_MAX && receiverInput > RECEIVER_INPUT_MIN) return true;
  return false;
}

//Processes receiver input and writes the converted value out to the servo
void processServoInput(Servo servo, int receiverInput, const char servoType[], const char servoError[]) {
  if (servo.attached()) {
    //The servoInValue (in microseconds) is mapped to the min and max of the Servo.write() function
    byte convertedServoValue = map(receiverInput, getReceiverInputMin(servoType), getReceiverInputMax(servoType), SERVO_MIN, SERVO_MAX);
    servo.write(convertedServoValue);
    sendJson(servoType, convertedServoValue);
  } else {
    sendJson("error", servoError);
  }
}

//Takes a servoType and returns the corresponding calibrated receiver input max value
int getReceiverInputMax(const char servoType[]) {
  if (strcmp(servoType, "aileron") == 0) return aileronMax;
  if (strcmp(servoType, "elevator") == 0) return elevatorMax;
  if (strcmp(servoType, "rudder") == 0) return rudderMax;
  if (strcmp(servoType, "throttle") == 0) return throttleMax;
}

//Takes a servoType and returns the corresponding calibrated receiver input min value
int getReceiverInputMin(const char servoType[]) {
  if (strcmp(servoType, "aileron") == 0) return aileronMin;
  if (strcmp(servoType, "elevator") == 0) return elevatorMin;
  if (strcmp(servoType, "rudder") == 0) return rudderMin;
  if (strcmp(servoType, "throttle") == 0) return throttleMin;
}
