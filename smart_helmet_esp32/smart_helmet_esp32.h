#ifndef _SMART_HELMET_ESP32_H
#define _SMART_HELMET_ESP32_H

#include <stdint.h>

struct Force_Readings {
  float force1; //kg
  float force2; //kg
  float force3; //kg
  float force4; //kg
};

float cf = 10;   //CHANGE FOR TEST  //calibration factor

#define FSR1_PIN 36     // the FSR and 10K pulldown are connected to GPIO36
#define FSR2_PIN 39
#define FSR3_PIN 34
#define FSR4_PIN 35


#endif // _SMART_HELMET_ESP32_H
