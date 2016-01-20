Avigate
==================================
#### Android piloting software for RC model planes (work in progress)

[![License](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://github.com/baronomasia/avigate/blob/master/LICENSE)

#### Overview
Note that this software not yet completed, but is actively being worked on. Please do not use this software with the expectation that it will produce a fully (or even partially) functioning autonomous RC airplane (at least not yet). Currently, the test plane is capable of automatically maintaining roll stability while the rest is controlled by the radio controller. I am currently working on implementing GPS waypoints, and will then transition to altitude control (which is much more complex). If you have any feedback, feel free to post in the Issues section or open a Pull Request (PR).

#### Requirements

* 2 Android devices (4.1+) with cellular data, one with [USB-OTG](https://www.androidpit.com/usb-otg-what-it-means-and-how-to-use-it) capability
* 1 [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) (I use an Arduino Uno Rev3)
* 1 fully functional RC plane with control surfaces, servos, a motor (prop), an electronic speed controller (ESC), and a battery. This plane should be sufficiently large enough to carry the Android device with [USB-OTG](https://www.androidpit.com/usb-otg-what-it-means-and-how-to-use-it) during flight. I recommend a wingspan of at least 4 feet (48 inches).
* 1 radio controller and receiver with at least 5 channels
* 1 [Arduino protoboard](https://www.adafruit.com/products/2077) (recommended and includes headers)
* 1 [Micro-USB OTG cable](http://www.amazon.com/Cable-Matters-Micro-USB-Adapter-6-Inch/dp/B00GM0OZ4O/)
* 1 [USB Type A to USB Type B cable](http://www.amazon.com/gp/product/B000JC00UO) (e.g. USB printer cable)
* 5-10 [servo extension cables](http://www.amazon.com/dp/B00P6JJFIS/) (male to male)
* 2 reasonably heavy duty rubber bands (recommended) or zip ties
* A soldering iron, wirecutter, short spool of spare wire, and time on your hands
* The knowledge that I am not responsible WHATSOEVER for any harm incurred by using this software (see MIT license). Use at your own risk.

#### Soldering

The most difficult part of the setup is soldering everything you need to the [protoboard](https://www.adafruit.com/products/2077) of the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno).

1. Find the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) and [protoboard](https://www.adafruit.com/products/2077) components, and set up your soldering equipment
2. Use the headers (and only the headers) that came with the [Arduino protoboard](https://www.adafruit.com/products/2077) and align them on the board [as shown here](http://imgur.com/VkaCgbK).
3. Solder the aligned headers into place and then stack on top of the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) while it cools [as shown here](http://imgur.com/GiXsY0d).
4. Once cooled, it is most important that BEFORE you plug in the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno), that you bend the header pin labeled 5v such that it does NOT get plugged into the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) when the [protoboard](https://www.adafruit.com/products/2077) is stacked on top of it. This is so that a separate power supply (e.g. battery and ESC) can power the motor instead of using the Arduino's USB power (which is pretty weak).
5. Using your fingers, take the plain header strip and break it into 10, 3-pin pieces.
6. Solder these 10 3-pin plain header pieces to the center of the board such that two pins of each 3-pin piece are soldered to the parallel GND and 5V strips in the center of the [protoboard](https://www.adafruit.com/products/2077), and the the 3rd pin of each 3-pin piece is soldered to the first pin of the gold strip closest to the 5V strip. See [this diagram](http://imgur.com/KFwUxEb) for details. Be sure to insert the short side of the plain headers into the top of the [protoboard](https://www.adafruit.com/products/2077) such that it sticks out slightly below and solder that to the [protoboard](https://www.adafruit.com/products/2077).
7. Once cooled, solder a wire on the underside of the protobard from the hole/slot adjacent to each 3-pin header (farthest from the center) to a Digital I/O hole/slot towards the side of the board. See [this diagram](http://imgur.com/ZiNcXOC) for details. I used I/O slots/holes 2-11, but you should be able to use any of them (0, 1, 12, and 13 included).
8. Wait for it cool and you're done soldering the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno)!

#### Connecting the servos and receiver

This is fairly straightforward, but may require some finanging with wiring the servo extension cables.

1. Find the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) and soldered [protoboard](https://www.adafruit.com/products/2077) and stack them together. Be sure to bend the 5v header pin inwards so that it does not contact the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) board.
2. Connect 5 channels of the receiver to 5 of the 3-pin headers at the center of the [protoboard](https://www.adafruit.com/products/2077) using 5 [servo extension cables](). Make sure the black wire of each [servo extension cable] is plugged into the GND pin/strip of the each 3-pin header.
3. Document which receiver channel is connected to which Digital I/O pin on the [protoboard](https://www.adafruit.com/products/2077).
4. Connect the 3 servos and the ESC to the remaining 3-pin headers at the center of the [protoboard](https://www.adafruit.com/products/2077). 
5. Document which servo / ESC is connected to which Digital I/O pin on the [protoboard](https://www.adafruit.com/products/2077).

#### Programming the [Arduino]()

All you gotta do is plug in the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) and upload the sketch.

1. Download the [Arduino IDE](https://www.arduino.cc/en/Main/Software) and set it up on your computer.
2. Clone [this repo](https://github.com/baronomasia/avigate.git) on your computer.
3. Open arduino/arduino_sketch/arduino_sketch.ino (located in the root of the avigate directory) in the [Arduino IDE](https://www.arduino.cc/en/Main/Software).
4. Plug in the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) into the computer using the [USB Type A to USB Type B cable](http://www.amazon.com/gp/product/B000JC00UO).
5. In the [Arduino IDE](https://www.arduino.cc/en/Main/Software), select Tools > Board > Arduino/Genuino Uno. Then select Tools > Port > /dev/tty.usbmodemXXXX, where XXXX is a set of letters or numbers.
6. Click the upload button at the top of the window containing the arduino_sketch
7. If successful, you should see a "Done uploading" message at the bottom of the window containing the arduino_sketch

#### Wiring the plane

Now that the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) is programmed, it's time to put everything in the plane. Note: this is harder than it sounds.

1. Identify the location on the underside of the plane where the Android device with USB-OTG will be mounted. For weight distribution, I suggest placing it close to the center of mass or slightly behind it. You can use the rubber bands or zip ties to mount it in place later.
2. Identify where the [micro-USB OTG cable](http://www.amazon.com/Cable-Matters-Micro-USB-Adapter-6-Inch/dp/B00GM0OZ4O/) can snake through the underside of the plane to reach the micro-USB slot of the mounted Android device. If necessary, you can create an opening using a sharp implement.
3. Wire the micro-USB OTG cable from the interior of the plane through the underside of the plane such that can reach the micro-USB slot of the Android device that will be mounted on the underside of the plane.
4. Plug in the [USB Type A to USB Type B cable](http://www.amazon.com/gp/product/B000JC00UO) into the other end of the [micro-USB OTG cable](http://www.amazon.com/Cable-Matters-Micro-USB-Adapter-6-Inch/dp/B00GM0OZ4O/).
5. Plug in the Type B end of the cable into the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno), and tuck the [Arduino](https://www.arduino.cc/en/Main/ArduinoBoardUno) / receiver combination and their wires into the interior of the plane.
6. Ensure that the battery connector is easily accessible so that you can plug and unplug it while you are out in the field (no pun intended)
7. Balance the plane as you see fit.

#### Configuring the software

It's almost time to fly the plane. You just need to tell the software which pins on the Arduino go where.

1. Install the Avigate app on both Android devices (you can compile it using Android Studio and/or the Gradle command line tool).
2. Launch the Avigate app on the device that will be strapped to the plane and select the Craft Module.
3. Plug in the [micro-USB OTG cable](http://www.amazon.com/Cable-Matters-Micro-USB-Adapter-6-Inch/dp/B00GM0OZ4O/) into the device and press OK when prompted.
4. Launch the Avigate app on the other Android device and select the Controller Module.
5. Create a new Craft Profile and tap the Configure option when finished.
6. Using the documented information from earlier, set each servo/motor to the correct pin. 
7. Hold the plane while you plug in the battery. Then use the sliders to test whether you have correctly configured the Craft Profile servo outputs. Make adjustments as necessary.
8. If you would like to limit the range of any servo, tap and hold the box containing the current value for that servo and set the range in the dialog box that pops up.
9. In the Inputs tab, set the pin number for the receiver inputs. For the cutover, put the remaining pin NOT corresponding to the aileron, elevator, rudder, or throttle inputs/controls. The cutover should be a switch on the radio controller that acts as a failover if the software does not behave as exepcted.
10. Turn on the radio controller.
11. In the Calibration tab, tap the CALIBRATE button at the bottom of the screen. Then use the radio controller to simulate the full range of receiver input values (including the cutover) by pushing each stick / switch to the minimum and the maximum. When you are finished calibrating, tap the "STOP CALIBRATING" button at the bottom.
12. If successful, you will see value ranges listed in microseconds next to each receiver input. If these values look correct (e.g. around 500-2000us), tap the back button and hit the Save button when prompted. Congratulations, you've configured your plane!

#### Flying the plane

Time to fly the plane!

1. If you are confident that everything is configured correctly, take your plane to an appropriate open field and ensure both devices have cellular data reception
2. On the appropriate Android device, open the Avigate app and select the Craft Module
3. Connect the micro-USB cable to the Android device and click OK when prompted for permission.
4. Securely mount the appropriate Android device to the underside of the plane (I recommend using rubber bands twisted around the device at least once).
5. Open the Avigate app on the other Android device and select the Controller Module.
6. While standing away from the plane, press the Fly button on the Craft Profile you created in the previous section.
7. Using the radio controller, ensure that you have appropriate control over the plane.
8. Be sure to test the cutover to ensure that if something goes wrong, you can wrest control of the plane from the Android device strapped to the plane.
9. Once you are confident that everything is in working order, fly the plane! 
10. ???
11. Profit!

#### Attributions for third-party libraries

* [JSON.Simple](https://code.google.com/p/json-simple/)
* [Paho](http://www.eclipse.org/paho/)
* [Rajawali](https://github.com/Rajawali/Rajawali)
* [RangeSeekBar](https://github.com/anothem/android-range-seek-bar)
* [UsbSerial](https://github.com/felHR85/UsbSerial)