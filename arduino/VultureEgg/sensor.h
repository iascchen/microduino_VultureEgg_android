#include "Arduino.h"

//================
#include <Wire.h>                                 //调用库  
#include "I2Cdev.h"                             //调用库  
//温度
#include <lm75.h>
TempI2C_LM75 termo = TempI2C_LM75(0x48, TempI2C_LM75::nine_bits);
//湿度
#include <SHT2x.h>
//气压
#include "BMP085.h"                             //调用库  
BMP085 barometer;
//光照
#include <Adafruit_TSL2561_U.h>                 //调用库  
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified(TSL2561_ADDR_LOW, 12345);

//3,传感器值的设置
float sensor_tem, sensor_hum, sensor_lux, sensor_pre, sensor_alt; //温度 湿度 光照 气压 海拔

float getTem()
{
  return termo.getTemp();
}

float getHum()
{
  return SHT2x.GetHumidity();
}

float getLux()
{
  //获取光照===============================================
  sensors_event_t event;
  tsl.getEvent(&event);
  if (event.light)
    return event.light;
  else
    return 0;
}

float getPre()
{
  //获取气压、海拔==========================================
  barometer.setControl(BMP085_MODE_TEMPERATURE);
  unsigned long lastMicros = micros();
  //先获取温度，用来补偿气压值
  while (micros() - lastMicros < barometer.getMeasureDelayMicroseconds());
  barometer.getTemperatureC();
  barometer.setControl(BMP085_MODE_PRESSURE_3);
  //得出气压值
  while (micros() - lastMicros < barometer.getMeasureDelayMicroseconds());
  return barometer.getPressure();
}

float getAlt()
{
  return barometer.getAltitude(getPre()); //结合气压值，以标准大气压得出海拔值
}

