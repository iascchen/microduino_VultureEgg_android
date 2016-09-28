#include "Arduino.h"

//==================
#define timeSwitch 100
unsigned long millisSwitch = millis();

boolean ble_check(char* _check)
{
  unsigned long _timeout = millis();
  while (1)
  {
    if (millis() - _timeout > 1000)
    {
#ifdef _DEBUG
      DEBUG.println("DATA ERROR");
#endif
      return false;
    }
    if (FROM.available()) {
      char c = FROM.read();
      //      DEBUG.write(c);
      if (FROM.find(_check))
      {
#ifdef _DEBUG
        DEBUG.println("DATA OK");
#endif
        return true;
      }
    }
  }
}

int baud_speed[5] =
{
  9600, 19200, 38400, 57600, 115200
};

bool bleRenew()
{
  FROM.print("AT+RENEW");
  delay(1000);
  return ble_check("RENEW");
}

bool bleReset()
{
  FROM.print("AT+RESET");
  delay(1000);
  return ble_check("RESET");
}

bool bleBaud_speed(int _speed)
{
  int  _v;
  for (int b = 0; b < 5; b++)
  {
    if (baud_speed[b] == _speed)
    {
      _v = b;
#ifdef _DEBUG
      DEBUG.println(_v);
#endif
      break;
    }
  }

  for (int a = 0; a < 5; a++)
  {
    FROM.begin(baud_speed[a]);
    delay(1000);
    FROM.print("AT+BAUD");
    FROM.print(_v);

    if (ble_check("Set"))
    {
      if (bleReset())
      {
#ifdef _DEBUG
        DEBUG.println("bleReset ok");
#endif
        FROM.begin(_speed);
        return true;
      }
      else
      {
#ifdef _DEBUG
        DEBUG.println("bleReset error");
#endif
        FROM.begin(_speed);
        return false;
      }
    }
  }

  FROM.begin(_speed);
  return false;
}

bool bleCon(bool _STA, char* _MAC)
{
  if (_STA)
  {
    FROM.print("AT+CONNL");
    return true;
  }
  else
  {
    FROM.print("AT+ROLE1");
    if (ble_check("Set:1"))
    {
      FROM.print("AT+IMME1");
      if (ble_check("Set:1"))
      {
        if (bleReset())
        {
          FROM.print("AT+CON");
          FROM.print(_MAC);
          if (ble_check("CONNA"))
            return true;
        }
        else
          return false;
      }
      else
        return false;


      delay(500);
      FROM.print("AT+RESET");
      delay(2000);
    }
    else
      return false;
  }
}

void bleInit()
{
  FROM.print("AT+IMME0");
  delay(500);
}

void bleClear()
{
  FROM.print("AT+CLEAR");
  delay(500);
}


