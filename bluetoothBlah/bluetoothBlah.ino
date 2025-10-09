#include <SPI.h>
#include <SoftwareSerial.h> // REQUIRED for HC-05
#include "RF24.h"

// --- HC-05 Bluetooth Setup ---
#define BT_RX_PIN 2
#define BT_TX_PIN 3
SoftwareSerial bluetooth(BT_RX_PIN, BT_TX_PIN); 

// --- nRF24L01 Radio Setup ---
#define CE_PIN 9
#define CSN_PIN 10
RF24 radio(CE_PIN, CSN_PIN);

uint8_t address[][6] = { "1Node", "2Node" };

// Set this to 0 for the RECEIVING Node (RX)
// Set this to 1 for the TRANSMITTING Node (TX)
bool radioNumber = 1; 

// Fixed role: false = RX (default), true = TX
bool role = (radioNumber == 1); 

char textMessage[32] = ""; 
#define MAX_MSG_SIZE 32 

void setup() {
  Serial.begin(115200);     // For PC debugging (optional)
  bluetooth.begin(9600);    // START BT COMMUNICATION HERE (CHECK YOUR BAUD RATE!)

  while (!Serial) {} 

  if (!radio.begin()) {
    Serial.println(F("Radio hardware FAILED!!"));
    bluetooth.println(F("Radio hardware FAILED!!"));
    while (1) {}
  }

  // Determine the fixed role for this device
  if (radioNumber == 0) {
      // Node 0 is the Receiver
      bluetooth.println(F("--- Fixed RECEIVER (RX) Node ---"));
      radio.setPALevel(RF24_PA_LOW);
      radio.setPayloadSize(MAX_MSG_SIZE); 
      // Only open the reading pipe on its address
      radio.openReadingPipe(1, address[radioNumber]); 
      radio.startListening();
  } else {
      // Node 1 is the Transmitter
      bluetooth.println(F("--- Fixed TRANSMITTER (TX) Node ---"));
      bluetooth.println(F("*** Type message and press ENTER to send. ***"));
      radio.setPALevel(RF24_PA_LOW);
      radio.setPayloadSize(MAX_MSG_SIZE); 
      // Only open the writing pipe to the other node's address
      radio.openWritingPipe(address[!radioNumber]); 
      radio.stopListening(); // Ensure TX mode
  }
  
  bluetooth.print(F("Node Address: "));
  bluetooth.println((char*)address[radioNumber]);
} 

void loop() {
  
  if (role) {
    // ------------------------------------
    // 1. TRANSMITTER (TX) ROLE
    // ------------------------------------
    if (bluetooth.available()) {
      static uint8_t char_count = 0;
      char incoming_char = bluetooth.read();

      // Collect the message character by character
      if (incoming_char != '\n' && incoming_char != '\r' && char_count < MAX_MSG_SIZE - 1) {
        textMessage[char_count++] = incoming_char;
      } 
      // Transmission trigger: Newline received
      else if ((incoming_char == '\n' || incoming_char == '\r') && char_count > 0) {
        
        textMessage[char_count] = '\0'; 
        
        // Transmit the message
        bool report = radio.write(&textMessage, strlen(textMessage) + 1); 
        
        if (report) {
          bluetooth.print(F("Sent: "));
          bluetooth.println(textMessage);
        } else {
          bluetooth.println(F("Transmission failed (No ACK)."));
        }
        
        // Reset for the next message
        char_count = 0; 
        memset(textMessage, 0, MAX_MSG_SIZE);
      }
    }
  } else {
    // ------------------------------------
    // 2. RECEIVER (RX) ROLE
    // ------------------------------------
    uint8_t pipe;
    if (radio.available(&pipe)) {
      uint8_t bytes = radio.getPayloadSize();
      radio.read(&textMessage, bytes); 

      // Print received message to Bluetooth terminal
      bluetooth.print(F("Received: "));
      bluetooth.println(textMessage);
      
      memset(textMessage, 0, MAX_MSG_SIZE);
    }
  }
} // loop
