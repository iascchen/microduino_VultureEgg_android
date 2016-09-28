#include "I2Cdev.h"

//温湿度
#include <AM2321.h>                             //调用库  
AM2321 am2321;

//光照
#include <Adafruit_Sensor.h>                     //调用库  
#include <Adafruit_TSL2561_U.h>                 //调用库  
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified(TSL2561_ADDR_LOW, 12345);

float sensorTemp;       //温度值
float sensorHumi;       //湿度值
float sensorLight;      //光照强度

String dataTmp;
char buf[10];

String ble_update(float temp, float humi, float light) {
  dataTmp = "Pi;";
  dtostrf(temp, 1, 1, buf);
  dataTmp += String(buf) + ";";
  dtostrf(humi, 1, 1, buf);
  dataTmp += String(buf) + ";";
  dtostrf(light, 1, 1, buf);
  dataTmp += String(buf);
  dataTmp += "\n";

  return dataTmp;
}

void SensorInit() {
  //初始化-光照
  Serial.println(tsl.begin() ? "TSL2561 successful" : "TSL2561 failed");
  tsl.enableAutoGain(true);                                  // 在1x 与16x 增益中切换
  tsl.setIntegrationTime(TSL2561_INTEGRATIONTIME_13MS);      //13MS的采样速度
}

//更新传感器数据
void SensorUpdate() {
  //获取温度，湿度
  am2321.read();
  sensorTemp = am2321.temperature / 10.0;
  sensorHumi = am2321.humidity / 10.0;
  //获取光照强度
  sensors_event_t event;
  tsl.getEvent(&event);
  (event.light) ?  sensorLight = event.light : Serial.println("Sensor overload");
}
