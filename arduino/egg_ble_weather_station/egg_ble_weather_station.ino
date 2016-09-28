//1,屏幕显示部分=============================
#include <Wire.h>
#include "U8glib.h"
//2,传感器部分================================
//4，自定义部分=============================
#include "oled.h"
#include "sensor.h"

#define INTERVAL_LCD             500        //OLED及传感器刷新时间间隔   
#define INTERVAL_BLE             2000      //传感器数据上传时间间隔  

#define softSerial Serial1    //通讯软串口

unsigned long lcd_time = millis();          //OLED及传感器更新计时器
unsigned long net_time = millis();          //传感器数据上传更新计时器

String dataToSend;

void setup(void) {
  Serial.begin(115200);     //初始化波特率
  softSerial.begin(115200);    //首先启动软串口

  SensorInit();
}

void loop(void) {
  if (lcd_time > millis()) lcd_time = millis();
  if (millis() - lcd_time > INTERVAL_LCD) {
    SensorUpdate();         //更新传感器
    volcd(sensorTemp, sensorHumi, sensorLight);   //更新OLED显示
    lcd_time = millis();    //更新计时器
  }

  if (net_time > millis()) net_time = millis();
  if (millis() - net_time > INTERVAL_BLE) {
    dataToSend = ble_update(sensorTemp, sensorHumi, sensorLight);   //上传传感器数据

    softSerial.print(dataToSend);
    Serial.print(dataToSend);

    net_time = millis();    //更新计时器
  }
}




