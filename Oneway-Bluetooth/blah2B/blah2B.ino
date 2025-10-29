#include <SPI.h>
#include <SoftwareSerial.h> // Include for HC-05
#include "printf.h"
#include "RF24.h"

// --- HC-05 Bluetooth Setup ---
// Connect HC-05 TX to D2 (RX in code) and HC-05 RX to D3 (TX in code)
// NOTE: Use a 1k Ohm resistor on the D3 -> HC-05 RX connection for safety!
#define BT_RX_PIN 2
#define BT_TX_PIN 3
// NOTE: Adjust the baud rate to match your HC-05 (e.g., 9600, 38400, 57600)
SoftwareSerial bluetooth(BT_RX_PIN, BT_TX_PIN); // RX, TX

// --- nRF24L01 Radio Setup ---
#define CE_PIN 9
#define CSN_PIN 10
RF24 radio(CE_PIN, CSN_PIN);

// Radio Addresses
uint8_t addresses[][6] = { "1Node", "2Node" };

// Set this to 0 for Node A, and 1 for Node B
bool radioNumber = 1; // <<--- SET TO 1 FOR THE SECOND ARDUINO

// Use a character array for the text message payload
char textMessage[32] = ""; 
#define MAX_MSG_SIZE 31 // Max characters (32 total including null terminator)

void setup() {
  Serial.begin(115200);
  bluetooth.begin(38400); // Set HC-05 baud rate (Change if necessary!)

  while (!Serial) {}

  // --- nRF24L01 Initialization ---
  if (!radio.begin()) {
    Serial.println(F("Radio hardware is not responding!!"));
    while (1) {}
  }

  Serial.println(F("RF24/Bluetooth Chat Initialized"));
  Serial.print(F("This is Radio: "));
  Serial.println(radioNumber == 0 ? "Node A (0)" : "Node B (1)");

  // Set the PA Level
  radio.setPALevel(RF24_PA_LOW);

  // Set payload size to fit the textMessage array (32 bytes)
  radio.setPayloadSize(MAX_MSG_SIZE + 1); 
  
  // Node 0 transmits to 1Node and listens on 2Node, and vice versa.
  // The addresses are intentionally flipped from the original example for simple two-way chat.
  radio.openWritingPipe(addresses[!radioNumber]); // TX pipe to the other node
  radio.openReadingPipe(1, addresses[radioNumber]); // RX pipe on this node's address

  // Start listening for incoming messages (Always start as RX)
  radio.startListening();

  bluetooth.print(F("\n-- Chat Ready (Address: "));
  bluetooth.print((char*)addresses[radioNumber]);
  bluetooth.println(F(") --"));
}

void loop() {
  
  // 1. CHECK FOR INCOMING RADIO MESSAGES (RX)
  if (radio.available()) {
    uint8_t len = radio.getPayloadSize();
    radio.read(&textMessage, len);
    
    // Output the received message to the Bluetooth chat interface
    bluetooth.print(F("Remote: "));
    bluetooth.println(textMessage);
  }

  // 2. CHECK FOR OUTGOING MESSAGES FROM BLUETOOTH (TX)
  if (bluetooth.available()) {
    // Read the incoming characters from Bluetooth into the textMessage buffer
    static uint8_t char_count = 0;
    char incoming_char = bluetooth.read();

    if (incoming_char != '\n' && char_count < MAX_MSG_SIZE) {
      // Append character to buffer
      textMessage[char_count++] = incoming_char;
    } else {
      // Line end detected (or buffer full), terminate string and transmit
      textMessage[char_count] = '\0'; 
      
      if (char_count > 0) {
        // Stop listening to switch to Transmit mode
        radio.stopListening(); 
        
        // Transmit the message
        if (radio.write(&textMessage, strlen(textMessage) + 1)) {
          // Transmission successful, echo to sender's screen via Bluetooth
          bluetooth.print(F("You: "));
          bluetooth.println(textMessage);
        } else {
          // Transmission failed
          bluetooth.println(F("Failed to send message."));
        }
        
        // Resume listening for incoming messages immediately
        radio.startListening(); 
      }
      // Reset the buffer index for the next message
      char_count = 0;
    }
  }
}
