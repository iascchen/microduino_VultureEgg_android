#include "Arduino.h"

#define _DEBUG
#define FROM Serial1
#define SEND Serial
#define DEBUG Serial

//#define SET_RTC 16, 33, 33, 27, 8, 2015  //Hour,Minute,Second,Day,Month,Year

//-------------------------
#define egg_num_MAX 1

char* egg_MAC[egg_num_MAX] =
{
  "BC6A29BD0E6A"
};

