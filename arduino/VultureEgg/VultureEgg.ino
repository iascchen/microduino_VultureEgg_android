
#include "def.h"
#include "rtc.h"
#include "sensor.h"
#include "data.h"
#include "ble.h"
//======================

/*
AA BB AA 1A 01 2A 01 3A 01 4A 01 5A 01 6A 01 FF FF
 */

/*
AA BB BB 1A 01 2A 01 3A 01 4A 01 5A 01 6A 01 7A 01 8A 01 9A 01 1B 02 2B 02 3B 02 4B 02 5B 02 FF FF
 */

/*
AA BB CC 1A 01 FF FF
 */

unsigned long num = 0;

byte pkj[512];

byte inChar, inCache;

boolean sta = false;
boolean error = false;

//================
#include <Wire.h>
#include <Rtc_Pcf8563.h>

//================
#include <Wire.h>                                 //调用库  
#include "I2Cdev.h"                             //调用库  
//温度
#include <lm75.h>

//湿度
#include <SHT2x.h>
//气压
#include "BMP085.h"                             //调用库  

//光照
#include <Adafruit_TSL2561_U.h>                 //调用库  



//===================
void setup()
{
#ifdef _DEBUG
  DEBUG.begin(115200);
#endif
  FROM.begin(115200);
  SEND.begin(115200);


#ifdef SET_RTC
  getDateStamp(getTimeStamp(SET_RTC));
  Serial.println(getTimeStamp(Hour, Minute, Second, Day, Month, Year));
  setRTC();
#endif

#ifdef _DEBUG
  DEBUG.print("\n\rUnix TimeStamp:");
  getRTC();
  DEBUG.println(getTimeStamp(Hour, Minute, Second, Day, Month, Year));
#endif


  //  Wire.begin();

  //初始化-温度
#ifdef _DEBUG
  DEBUG.println("LM75 Set resolution to ten bits ");
#endif
  termo.setResolution(TempI2C_LM75::ten_bits);

  //初始化-气压
  barometer.initialize();
  if (barometer.testConnection())
  {
#ifdef _DEBUG
    DEBUG.println("BMP085 successful");
#endif
  }
  else
  {
#ifdef _DEBUG
    DEBUG.println("BMP085 failed");
#endif
  }

  //初始化-光照
  if (tsl.begin())
  {
#ifdef _DEBUG
    DEBUG.println("TSL2561 successful");
#endif
  }
  else
  {
#ifdef _DEBUG
    DEBUG.println("TSL2561 failed");
#endif
  }
  tsl.enableAutoGain(true);                                  // 在1x 与16x 增益中切换
  tsl.setIntegrationTime(TSL2561_INTEGRATIONTIME_13MS);      //13MS的采样速度

  //======================
  /*
  //======================
  if (bleBaud_speed(115200))
  {
  #ifdef _DEBUG
    DEBUG.println("bleBaud_speed ok");
    #endif
  }
  else
  {
  #ifdef _DEBUG
    DEBUG.println("bleBaud_speed error");
    #endif
  }
  */

  /*
    if (bleRenew())
    {
  #ifdef _DEBUG
      DEBUG.println("bleReset ok");
    #endif
    }
    else
    {
  #ifdef _DEBUG
      DEBUG.println("bleReset error");
    #endif
    }
  */

  if (bleCon(true, egg_MAC[0]))
  {
#ifdef _DEBUG
    DEBUG.println("bleCon ok");
#endif
  }
  else
  {
#ifdef _DEBUG
    DEBUG.println("bleCon error");
#endif
  }

  while (SEND.available())
  {
    DEBUG.print(SEND.read());
  }
  delay(10);
}


// the loop routine runs over and over again forever:
void loop()
{
  {
    if (FROM.available() > 0) {
      inCache = inChar;
      inChar = FROM.read();

      pkj[num] = inChar;
#ifdef _DEBUG
      //DEBUG.print(inCache,HEX);
      //DEBUG.print(inChar,HEX);
#endif
      num++;
      delayMicroseconds(200);
    }

    if (sta)
    {
      sta = false;

      error = false;
      switch (inChar)
      {
        case 0xAA:
          type = 1;
#ifdef _DEBUG
          DEBUG.println("\n\rType 1 RAW: ");
#endif
          break;
        case 0xBB:
          type = 2;
#ifdef _DEBUG
          DEBUG.println("\n\rType 2 RAW: ");
#endif
          break;
        case 0xCC:
          type = 3;
#ifdef _DEBUG
          DEBUG.println("\n\rType 3 RAW: ");
#endif
          break;
        default:
          error = true;
          type = 0;
#ifdef _DEBUG
          DEBUG.println("\n\rType ERROR ");
#endif
      }
      num = 0;
    }

    if (inChar == 0xbb && inCache == 0xaa)
    {
      sta = true;
#ifdef _DEBUG
      DEBUG.println("\n\r");
      DEBUG.println("\n\r----START----");
#endif
    }

    if (inChar == 0x0a && inCache == 0x0d)
    {
      inChar = NULL;
      inCache = NULL;

      num -= 2;

#ifdef _DEBUG
      DEBUG.print("NUM[");
      DEBUG.print(num);
      DEBUG.print("]:");
      for (long a = 0; a < num; a++)
      {
        DEBUG.print(pkj[a], HEX);
        DEBUG.print(" ");
      }
      DEBUG.println(" ");
#endif

      if (error)
      {
#ifdef _DEBUG
        DEBUG.println("DATA ERROR");
#endif
      }
      else
      {
#ifdef _DEBUG
        DEBUG.println("DATA OK");
#endif
        read_data(num, pkj);

        //---------------------
        int16_t millis2 = millis();

#ifdef _DEBUG
        DEBUG.println("\n\rTIME---------------");
#endif

        getRTC();
        TimeStamp = getTimeStamp(Hour, Minute, Second, Day, Month, Year);
#ifdef _DEBUG
        DEBUG.print("TimeStamp: ");
        DEBUG.print(TimeStamp);
        DEBUG.println(" \n\r");
#endif

        //---------------------
        if (type != 1) //not mpu
        {
          sensor_tem = getTem();
          sensor_hum = getHum();
          sensor_lux = getLux();
          sensor_pre = getPre();

#ifdef _DEBUG
          DEBUG.print("\n\rTEM: ");
          DEBUG.print(sensor_tem);
          DEBUG.print(",HUM: ");
          DEBUG.print(sensor_hum);
          DEBUG.print(",LUX: ");
          DEBUG.print(sensor_lux);
          DEBUG.print(",Pre: ");
          DEBUG.println(sensor_pre);
#endif
        }


#ifdef _DEBUG
        millis2 = millis() - millis2;
        DEBUG.print("[");
        DEBUG.print(millis2, DEC);
        DEBUG.print("]ms");
        DEBUG.println("------------");


        //---------------------
        DEBUG.println("\n\rsendDATA READY");
#endif

        if (sendDATA(type, egg_MAC[egg_num]))
        {
#ifdef _DEBUG
          DEBUG.println("sendDATA OK");
#endif
        }
        else
        {
#ifdef _DEBUG
          DEBUG.println("sendDATA ERROR");
#endif
        }
      }
#ifdef _DEBUG
      //DEBUG.print("\n\r");
      //DEBUG.println(num);
#endif
      num = 0;

#ifdef _DEBUG
      DEBUG.println("\n\r----END----");
#endif
    }
  }

  //===========================
  if (egg_num_MAX > 1) //egg_num_MAX>1
  {
    if (millis() < millisSwitch) millisSwitch = millis();
    if (millis() - millisSwitch > timeSwitch)
    {
      if (egg_num < egg_num_MAX)
        egg_num++;
      else
        egg_num = 0;

      if (!bleCon(true, egg_MAC[egg_num]))
      {
#ifdef _DEBUG
        DEBUG.print("\n\r[");
        DEBUG.print(egg_MAC[egg_num]);
        DEBUG.println("\n\r] connect ERROR");
#endif
        egg_num++;
      }

      delay(10);

      millisSwitch = millis();
    }
  }
}
