# Vulture Egg App

## Background

This Android App developed for Eggduino Project. 

It should be work on Android **4.4.2** or latter version. we had tested it on Android 4.4.2 and 5.1.0 .

## Related Projects

### Firmware in Vulture Egg

Firmware project : [https://github.com/lixianyu/VultureEgg141](https://github.com/lixianyu/VultureEgg141)

This Android App tested on VultureEgg [0.2.228_01 Hex](https://github.com/lixianyu/VultureEgg141/blob/master/Release/VultureEgg_0.2.228_01.hex)

### mCotton Cloud Service

mCotton project : [https://github.com/iascchen/mCotton](https://github.com/iascchen/mCotton)

This Android App tested on v 0.3.2.

## System Architecture

This Android App collect data via BLE from Vulture Egg, and send them to mCotton for store and display.

    Firmware  <== BLE 4 ==>  APP  <== Meteor DDP ==>  mCotton Service
    
## How to use it

1. Tap menu to "Setting" mCotton server and account. 

2. Tap menu to "Add device" to select "Vulture Egg" or "Egg Weather Station". 
One Android device/APP can add about 5 BLE devices ( It is limited by android).

3. Tap the device displayed on device list，you should set the device on mCotton Server, device type, and save it. 
In this view, App can display all three types of data : temperature, humidity, and quaternions.
But now, the data will **NOT** send to Server.

4. After you setting up all eggs and weather stations, You can click "Start" to collect data and send to mCotton. 
This action will **still work when screens off**.
You can change the data collection interval, and click "Apply" to reset of Vulture Egg. 
You can cheng this three interval value from mCotton, after click "Sent" button, mCooton will push message to this APP, can apply to Vulture Egg.
According reset vulture egg will use about 6 seconds one time, so please wait a moments after click the "Sent" button on mCotton.
