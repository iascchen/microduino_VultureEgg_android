#include "Arduino.h"

//=============================
int p;

int16_t read16(byte* _Buf) {
  byte _Buf1 = _Buf[p++];
  byte _Buf2 = _Buf[p++];
  int16_t r = (_Buf1 & 0xff) | ((_Buf2 << 8) & 0xff00);
  return r;
}

int8_t read8(byte* _Buf) {
  return _Buf[p++];
}

char* c_Cache(float _f_Cache)
{
  char _c_Cache[20];
  dtostrf(_f_Cache, 4, 0, _c_Cache); //将获取的数值转换为字符型数组
  return _c_Cache;
}


unsigned int egg_num = 0;

unsigned long millis1 = millis();
int type;

float egg_mpu[7];
float egg_tem[16];
float egg_hum[1];


void read_data(int _num, byte* _buf)
{
  p = 0;
  _num = _num / 2;

  int16_t _bufin[_num];

  //  inBuf=_buf;
  for (int i = 0; i < _num; i++)
  {
    //    if(type==3)
    //      _bufin_u[i]=read16(_buf);
    //    else
    _bufin[i] = read16(_buf);
  }


#ifdef _DEBUG
  DEBUG.println("\n\r");
#endif

  switch (type)
  {
    case 1:
      {
#ifdef _DEBUG
        DEBUG.print("DATA_MPU: ");
#endif
        for (int a = 0; a < _num; a++)
        {
#ifdef _DEBUG
          DEBUG.print(_bufin[a]);
          DEBUG.print(",");
#endif
          egg_mpu[a] = _bufin[a];
        }
      }
      break;
    case 2:
      {
#ifdef _DEBUG
        DEBUG.print("DATA_TEM: ");
#endif
        for (int a = 0; a < _num; a++)
        {
          //        _bufin_u[a]=_bufin[a];
          //        egg_tem[a]=-6.0f + 125.0f * (float)((float)_bufin_u[a]/(float)65535);
          //        egg_tem[a]=_bufin[a];
          //          egg_tem[a] = int((((_bufin[(2*a)]) << 8) | ((_bufin[(2*a)+1])))) / 256.0F;
          egg_tem[a] = ((_bufin[a] >> 5) * 0.125);
#ifdef _DEBUG
          DEBUG.print(egg_tem[a]);
          if (a < _num - 1)
            DEBUG.print(",");
#endif
        }
      }
      break;
    case 3:
      {
        uint16_t _bufin_u[_num];
#ifdef _DEBUG
        DEBUG.print("DATA_HUM: ");
#endif
        for (int a = 0; a < _num; a++)
        {
          _bufin_u[a] = _bufin[a];
          egg_hum[a] = -6.0f + 125.0f * (float)((float)_bufin_u[a] / (float)65535);
#ifdef _DEBUG
          DEBUG.print(egg_hum[a]);
          if (a < _num - 1)
            DEBUG.print(",");
#endif
        }
      }
      break;
    default:
      {
#ifdef _DEBUG
        DEBUG.print("DATA_ERROR: ");
#endif
      }
  }

#ifdef _DEBUG
  DEBUG.println(" ");
#endif
}


//================
//static char sendBuf[500];
boolean sendDATA(int _sendDATA, String _egg_MAC)
{
  boolean _Weather = false;
  switch (_sendDATA)
  {
    case 1:
      SEND.print("Egg;");
      SEND.print(_egg_MAC);
      SEND.print(';');
      SEND.print(TimeStamp);
      SEND.print(";");
      SEND.print(egg_mpu[0]);
      SEND.print(",");
      SEND.print(egg_mpu[1]);
      SEND.print(",");
      SEND.print(egg_mpu[2]);
      SEND.print(";");
      SEND.print(egg_mpu[3]);
      SEND.print(",");
      SEND.print(egg_mpu[4]);
      SEND.print(",");
      SEND.print(egg_mpu[5]);
      SEND.print(",");
      SEND.print(egg_mpu[6]);
      SEND.print(";;");
      SEND.println("");
      break;
    case 2:
      //----------
      SEND.print("Egg;");
      SEND.print(_egg_MAC);
      SEND.print(';');
      SEND.print(TimeStamp);
      SEND.print(";;;");
      for (int a = 0; a < 16; a++)
      {
        SEND.print(egg_tem[a]);
        if (a < 16 - 1)
          SEND.print(",");
      }
      SEND.print(";");
      SEND.println("");

      //----------
      _Weather = true;
      break;

    case 3:
      //----------
      SEND.print("Egg;");
      SEND.print(_egg_MAC);
      SEND.print(';');
      SEND.print(TimeStamp);
      SEND.print(";;;;");
      SEND.print(egg_hum[0]);
      SEND.println("");

      //----------
      _Weather = true;
      break;

    default:
      return false;

  }

  if (_Weather)
  {
    SEND.print("Weather;");
    SEND.print(TimeStamp);
    SEND.print(";");
    SEND.print(sensor_tem);
    SEND.print(";");
    SEND.print(sensor_hum);
    SEND.print(";");
    SEND.print(sensor_lux, 0);
    SEND.print(";");
    SEND.print(sensor_pre, 0);
    SEND.println("");
  }

  return true;
}

