//Created by Ryan Staatz

//Arduino JSON library: https://github.com/bblanchon/ArduinoJson
#include <ArduinoJson.h>
#include <Servo.h>

//Sets the baud rate of the Arduino
const long BAUD_RATE = 115200;

//Receiver input timeout in microseconds
const int PULSE_TIMEOUT = 44100;

//Pulse threshold values for receiver input for cutover between auto and manual control
const int CUTOVER_MIN = 900;
const int CUTOVER_MAX = 1500;

//Pulse threshold values for receiver input for servos and motor
const int RECEIVER_MIN = 750;
const int RECEIVER_MAX = 1800;

//Sets the character array (max) length for serial input
const int numChars = 300;

//This array stores Json characters read from serial input
char receivedJson[numChars];

//All valid input/output must start/end with the corresponding char
char startMarker = '@';  
char endMarker = '#';

//Determines if new Json data has been collected from serial input for processing
boolean newData = false;

//Determines if the plane is controlled manually (remote control) or via autopilot (phone)
boolean manualControl = false;

//Determines if the cutover pin can use interrupts
boolean cutoverInterrupt = false;

//Stores the pin value for each receiver input
int aileronIn = -1;
int elevatorIn = -1;
int rudderIn = -1;
int throttleIn = -1;
int cutoverIn = -1;

//Servos (including the motor)
Servo aileron;
Servo elevator;
Servo rudder;
Servo throttle;

void setup() {
  Serial.begin(BAUD_RATE);  //Set the baud rate
  while (!Serial) {}  //Wait for serial port to connect
  sendJson("status", "ready");  //Inform serial interface that Arduino is ready
}

void loop() {
  readSerialJsonText();
  processJsonText();
  readReceiverInput();
} 

//Reads json serial input and stores input in a char array
//Most of this method code is taken from: https://forum.arduino.cc/index.php?topic=288234.0
void readSerialJsonText() {
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
        if (index >= numChars) {
          index = numChars - 1;
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
//@{"manualControl":false, "config":{"throttle":10, "elevator":9, "aileron":8, "rudder":7, "aileronIn":6, "elevatorIn":5, "rudderIn":4, "throttleIn":3, "cutoverIn":2, "cutoverInterrupt":true}, "throttle":120, "elevator":100, "aileron":90, "rudder":75}# 
//@{"aileron": 90}#
void processJsonText() {
  if (newData == true) {
    StaticJsonBuffer<numChars> jsonBuffer;
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
      manualControl = (boolean) root["manualControl"];
      if (manualControl) {
        sendJson("manualControl", true);
      } else {
        sendJson("manualControl", false);
      }
    }

    //Assigns pin numbers to the motor, servos, and receiver inputs based on the received JSON
    if (root.containsKey("config")) {
      JsonObject& config = root["config"];
      if (config.containsKey("aileron")) {
        int aileronPin = (int) config["aileron"];
        aileron.attach(aileronPin);
      }
      if (config.containsKey("elevator")) {
        int elevatorPin = (int) config["elevator"];
        elevator.attach(elevatorPin);
      }
      if (config.containsKey("rudder")) {
        int rudderPin = (int) config["rudder"];
        rudder.attach(rudderPin);
      }
      if (config.containsKey("throttle")) {
        int throttlePin = (int) config["throttle"];
        throttle.attach(throttlePin);
      }
      if (config.containsKey("aileronIn")) {
        aileronIn = (int) config["aileronIn"];
        pinMode(aileronIn, INPUT);
      }
      if (config.containsKey("elevatorIn")) {
        elevatorIn = (int) config["elevatorIn"];
        pinMode(elevatorIn, INPUT);
      }
      if (config.containsKey("rudderIn")) {
        rudderIn = (int) config["rudderIn"];
        pinMode(rudderIn, INPUT);
      }
      if (config.containsKey("throttleIn")) {
        throttleIn = (int) config["throttleIn"];
        pinMode(throttleIn, INPUT);
      }
      if (config.containsKey("cutoverIn")) {
        cutoverIn = (int) config["cutoverIn"];
        pinMode(cutoverIn, INPUT);
      }
      //Configures an interrupt service routine for listening for the cutover signal from the receiver
      if (config.containsKey("cutoverInterrupt")) {
        if (cutoverIn != -1) {
          cutoverInterrupt = (boolean) config["cutoverInterrupt"];
          attachInterrupt(digitalPinToInterrupt(cutoverIn), listenForCutover, CHANGE);
        } else {
          sendJson("error", "Unassigned cutover pin");
        }
      }
    }

    //If autopilot is enabled, set the servo values according to the received JSON
    if (!manualControl) {
      if (root.containsKey("aileron")) {
        if (aileron.attached()) {
          int aileronValue = root["aileron"];
          aileron.write(aileronValue);
          sendJson("aileron", aileronValue);
        } else {
          sendJson("error", "Unassigned aileron pin");
        }
      }

      if (root.containsKey("elevator")) {
        if (elevator.attached()) {
          int elevatorValue = root["elevator"];
          elevator.write(elevatorValue);
          sendJson("elevator", elevatorValue);
        } else {
          sendJson("error", "Unassigned elevator pin");
        }
      }
     
      if (root.containsKey("rudder")) {
        if (rudder.attached()) {
          int rudderValue = root["rudder"];
          rudder.write(rudderValue);
          sendJson("rudder", rudderValue);
        } else {
          sendJson("error", "Unassigned rudder pin");
        }
      }

      if (root.containsKey("throttle")) {
        if (throttle.attached()) {
          int throttleValue = root["throttle"];
          throttle.write(throttleValue);
          sendJson("throttle", throttleValue);
        } else {
          sendJson("error", "Unassigned throttle pin");
        }
      }
    }
    newData = false;
  }
}

//TODO prevent process from hanging if receiver is out of range and no interrupt is in place
//Listens for cutover input to switch between manual (receiver) and autopilot (phone) mode
void listenForCutover() {
  if (cutoverIn != -1) {
    long pulse = pulseIn(cutoverIn, HIGH, PULSE_TIMEOUT);
    if (pulse != 0) {
      if (pulse < CUTOVER_MIN) {
        manualControl = false;
        sendJson("manualControl", false);
      } else if (pulse > CUTOVER_MAX) {
        manualControl = true;
        sendJson("manualControl", true);
      }
    }
  }
}

//Reads receiver input and writes translated values to servos
void readReceiverInput() {
  if (!cutoverInterrupt) listenForCutover();  //If there is no cutover interrupt already running, listen for cutover without an interrupt
  //Only read servo inputs if manual control is enabled
  if (manualControl) {
    if (throttle.attached()) {
      if (throttleIn != -1) {
        long pulse = pulseIn(throttleIn, HIGH, PULSE_TIMEOUT);
        if (pulse != 0) {
          int servoPosition = constrain(map(pulse, RECEIVER_MIN, RECEIVER_MAX, 0, 180), 0, 180);
          throttle.write(servoPosition);
          sendJson("rudder", servoPosition);         }
      }
    } else {
      sendJson("error", "Unassigned throttle pin");
    }
    
    if (elevator.attached()) {
      if (elevatorIn != -1) {
        long pulse = pulseIn(elevatorIn, HIGH, PULSE_TIMEOUT);
        if (pulse != 0) {
          int servoPosition = constrain(map(pulse, RECEIVER_MIN, RECEIVER_MAX, 0, 180), 0, 180);
          elevator.write(servoPosition);
          sendJson("rudder", servoPosition);         }
      }
    } else {
      sendJson("error", "Unassigned elevator pin");
    }
    
    if (aileron.attached()) {
      if (aileronIn != -1) {
        long pulse = pulseIn(aileronIn, HIGH, PULSE_TIMEOUT);
        if (pulse != 0) {
          int servoPosition = constrain(map(pulse, RECEIVER_MIN, RECEIVER_MAX, 0, 180), 0, 180);
          aileron.write(servoPosition);
          sendJson("rudder", servoPosition);         }
      }
    } else {
      sendJson("error", "Unassigned aileron pin");
    }

    if (rudder.attached()) {
      if (rudderIn != -1) {
        long pulse = pulseIn(rudderIn, HIGH, PULSE_TIMEOUT);
        if (pulse != 0) {
          int servoPosition = constrain(map(pulse, RECEIVER_MIN, RECEIVER_MAX, 0, 180), 0, 180);
          rudder.write(servoPosition);
          sendJson("rudder", servoPosition); 
        }
      }
    } else {
      sendJson("error", "Unassigned rudder pin");
    }
  }
}

//Takes a key (text) value (boolean) pair and sends them as JSON over the serial interface
void sendJson(const char key[], boolean value) {
  StaticJsonBuffer<numChars> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key value (char array) pair and sends them as JSON over the serial interface
void sendJson(const char key[], const char value[]) {
  StaticJsonBuffer<numChars> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}

//Takes a key (text) value (int) pair and sends them as JSON over the serial interface
void sendJson(const char key[], int value) {
  StaticJsonBuffer<numChars> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root[key] = value;
  Serial.print(startMarker);
  root.printTo(Serial);
  Serial.println(endMarker);
}
