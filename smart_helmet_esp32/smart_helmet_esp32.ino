#include "smart_helmet_esp32.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

struct Force_Readings forces; 

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint8_t value[20];

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};
 
void setup(void) {
    Serial.begin(115200);

    // Create the BLE Device
    BLEDevice::init("ESP32");
  
    // Create the BLE Server
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
  
    // Create the BLE Service
    BLEService *pService = pServer->createService(SERVICE_UUID);
  
    // Create a BLE Characteristic
    pCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID,
                        BLECharacteristic::PROPERTY_READ   |
                        BLECharacteristic::PROPERTY_WRITE  |
                        BLECharacteristic::PROPERTY_NOTIFY |
                        BLECharacteristic::PROPERTY_INDICATE
                      );
  
  
    pCharacteristic->addDescriptor(new BLE2902());
  
    // Start the service
    pService->start();
  
    // Start advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(false);
    pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
    BLEDevice::startAdvertising();
    Serial.println("Waiting a client connection to notify...");
}
 
void loop(void) {
  get_force_readings(&forces);
  // impact_analysis(&forces);
   // notify changed value
    if (deviceConnected) {
        value[0] = forces.force1;
        value[1] = forces.force2;
        value[2] = forces.force3;
        value[3] = forces.force4;
        Serial.print("Value[0]: ");
        Serial.println(value[0]);
        Serial.print("Value[1]: ");
        Serial.println(value[1]);
        Serial.print("Value[2]: ");
        Serial.println(value[2]);
        Serial.print("Value[3]: ");
        Serial.println(value[3]);
        pCharacteristic->setValue(value, 4);
        pCharacteristic->notify();
        
        delay(3000); // bluetooth stack will go into congestion, if too many packets are sent, in 6 hours test i was able to go as low as 3ms
    }
    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
    }
}

void get_force_readings(struct Force_Readings *forces) {
  int fsr1_reading = analogRead(FSR1_PIN);  // the analog reading from the FSR resistor divider
  int fsr2_reading = analogRead(FSR2_PIN);
  int fsr3_reading = analogRead(FSR3_PIN);
  int fsr4_reading = analogRead(FSR4_PIN); 

  float v1_out = (fsr1_reading * 3.3) / 4095;
  float v2_out = (fsr2_reading * 3.3) / 4095;
  float v3_out = (fsr3_reading * 3.3) / 4095;
  float v4_out = (fsr4_reading * 3.3) / 4095;
  v1_out = v1_out * cf;
  v2_out = v2_out * cf;
  v3_out = v3_out * cf;
  v4_out = v4_out * cf;

  forces->force1 = v1_out;  // LEFT SENSOR
  forces->force2 = v2_out;  // RIGHT SENSOR
  forces->force3 = v3_out;  // FRONT SENSOR
  forces->force4 = v4_out;  // BACK SENSOR
}

void impact_analysis(struct Force_Readings *forces) {
  Serial.print("Impact 1 = ");
  Serial.print(forces->force1);     // print the force reading in kg
  Serial.print(", Impact 2 = ");
  Serial.print(forces->force2);
  Serial.print(", Impact 3 = ");
  Serial.print(forces->force3);
  Serial.print(", Impact 4 = ");
  Serial.print(forces->force4);

  int min_horizontal_force;
  int max_horizontal_force;
  int min_vertical_force;
  int max_vertical_force;
  int max_force;
  int impact_location[2];
  
  if(forces->force1 <= forces->force2) {
    min_horizontal_force = forces->force1;
    max_horizontal_force = forces->force2; 
  } else {
    min_horizontal_force = forces->force2;
    max_horizontal_force = forces->force1;
  }
  if(forces->force3 <= forces->force4) {
    min_vertical_force = forces->force3;
    max_vertical_force = forces->force4; 
  } else {
    min_vertical_force = forces->force4;
    max_vertical_force = forces->force3;
  }
  max_force = max(max_horizontal_force, max_vertical_force);
  
  impact_location[0] = forces->force2 - forces->force1;
  impact_location[1] = forces->force3 - forces->force4;
 
  if ((forces->force1 < 0.5) && (forces->force2 < 0.5) && (forces->force3 < 0.5) && (forces->force4 < 0.5)) {
    Serial.println(" - No impact");
  } else {
      if (max_force < 1.0) {
        Serial.print(" - Light impact at (");
      } else if (max_force < 1.5) {
        Serial.print(" - Medium impact at (");
      } else if (max_force < 2.5) {
        Serial.print(" - Heavy impact at (");
      } else {
        Serial.print(" - ALERT DISPATCH: KNOCKOUT at ");
      }
      Serial.print(impact_location[0]);
      Serial.print(", ");
      Serial.print(impact_location[1]);
      Serial.println(")");
 }
}

void ble_comms(struct Force_Readings *forces) {
  uint8_t write_buffer[1];
  write_buffer[0] = forces->force1;
  
}
