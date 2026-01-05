package com.example.wavelynx_minor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    Toolbar tb;
    BluetoothSocket socket;
    OutputStream outputStream;
    InputStream inputStream;
    BluetoothAdapter bluetoothAdapter;

    EditText messageInput;
    Button sendButton;
    ListView chatList;

    ArrayList<String> chatMessages;
    chatAdapter adapter;

    boolean isConnected = false;
    boolean isActivityRunning = true;
    String currentDeviceAddress = null;

    static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_page);

        Log.e("Lifecycle", "onCreate started");

        tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        Log.e("UI", "Toolbar set");

        messageInput = findViewById(R.id.editTextText);
        sendButton = findViewById(R.id.button);
        chatList = findViewById(R.id.listView);
        chatMessages = new ArrayList<>();
        adapter = new chatAdapter(this, chatMessages);
        chatList.setAdapter(adapter);
        Log.e("UI", "Views initialized and adapter set");

        chatList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        chatList.setStackFromBottom(true);

        sendButton.setEnabled(false);
        messageInput.setEnabled(false);
        Log.e("Bluetooth", "Send and input fields disabled until connection");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.e("Bluetooth", "Bluetooth adapter obtained");

        currentDeviceAddress = getIntent().getStringExtra("device_address");
        Log.e("Bluetooth", "Received device address: " + currentDeviceAddress);

        if (currentDeviceAddress == null || currentDeviceAddress.isEmpty()) {
            Log.e("Error", "No device selected. Finishing activity.");
            Toast.makeText(this, "No device selected to connect", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initial connection
        new Thread(() -> {
            Log.e("Thread", "Connection thread started");
            connectToDevice(currentDeviceAddress);
            if (isConnected) {
                Log.e("Thread", "Connected successfully. Starting receiver...");
                startReceiving();
            } else {
                Log.e("Thread", "Connection failed, not starting receiver.");
            }
        }).start();

        sendButton.setOnClickListener(v -> {
            Log.e("ButtonClick", "Send button pressed");
            if (!isConnected) {
                Log.e("ButtonClick", "Device not connected");
                Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
                return;
            }

            String msg = messageInput.getText().toString().trim();
            Log.e("ButtonClick", "Message to send: " + msg);

            if (!msg.isEmpty()) {
                sendMessage(msg);
                chatMessages.add("Me: " + msg);
                adapter.notifyDataSetChanged();
                scrollToBottom();
                messageInput.setText("");
                Log.e("ButtonClick", "Message sent and added to chat list");
            } else {
                Log.e("ButtonClick", "Empty message ignored");
            }
        });

        Log.e("Lifecycle", "onCreate completed");
    }

    void scrollToBottom() {
        Log.e("UI", "Scrolling to bottom");
        chatList.post(() -> chatList.setSelection(adapter.getCount() - 1));
    }

    void connectToDevice(String address) {
        Log.e("Bluetooth", "Attempting to connect to address: " + address);
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();

            try {
                Thread.sleep(1000);
                Log.e("Bluetooth", "Connecting socket...");
                socket.connect();
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                isConnected = true;

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                    messageInput.setEnabled(true);
                });
                Log.e("Bluetooth", "✅ Connected to " + device.getName());

            } catch (IOException e) {
                Log.e("Bluetooth", "❌ Connection failed", e);
                handleDisconnect("❌ Connection failed. Try again.");
                try {
                    socket.close();
                    Log.e("Bluetooth", "Socket closed after failure");
                } catch (IOException closeEx) {
                    Log.e("Bluetooth", "Error closing socket", closeEx);
                }
            }
        } catch (Exception e) {
            Log.e("Bluetooth", "Invalid Bluetooth address", e);
            handleDisconnect("Invalid Bluetooth address");
        }
    }

    void sendMessage(String message) {
        Log.e("SendMessage", "Sending: " + message);
        try {
            if (outputStream != null && isConnected) {
                outputStream.write((message + "\n").getBytes());
                Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
                Log.e("SendMessage", "✅ Message sent successfully");
            } else {
                Log.e("SendMessage", "❌ Not connected to any device");
                handleDisconnect("Not connected to any device");
            }
        } catch (IOException e) {
            Log.e("SendMessage", "❌ Connection lost while sending", e);
            handleDisconnect("Connection lost while sending");
        }
    }

    void startReceiving() {
        Log.e("ReceiveThread", "Receiver thread started");
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String received = new String(buffer, 0, bytes).trim();
                    Log.e("ReceiveThread", "📩 Received: " + received);

                    runOnUiThread(() -> {
                        if(!received.startsWith("You:")) {
                            if(received.startsWith("Remote:"))
                            {
                                String removedRemote = received.replace("Remote:", "");
                                chatMessages.add("Friend: " + removedRemote);
                                adapter.notifyDataSetChanged();
                                scrollToBottom();
                            }
                            else {
                                chatMessages.add("Friend: " + received);
                                adapter.notifyDataSetChanged();
                                scrollToBottom();
                            }
                        }
                    });

                } catch (IOException e) {
                    Log.e("ReceiveThread", "⚠️ Connection lost while receiving", e);
                    handleDisconnect("⚠️ Connection lost");
                    break;
                }
            }
        }).start();
    }

    void handleDisconnect(String reason) {
        Log.e("Disconnect", reason);
        isConnected = false;

        if (!isActivityRunning) {
            Log.e("Disconnect", "Activity is not running — stopping reconnect attempts");
            return;
        }

        runOnUiThread(() -> {
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(false);
            messageInput.setEnabled(false);
        });

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (!isActivityRunning) {
                    Log.e("Reconnect", "App closed, skipping reconnect");
                    return;
                }
                Log.e("Reconnect", "Attempting to reconnect...");
                connectToDevice(currentDeviceAddress);
                if (isConnected) {
                    Log.e("Reconnect", "Reconnected successfully, restarting receiver");
                    startReceiving();
                } else {
                    Log.e("Reconnect", "Reconnect failed");
                }
            } catch (InterruptedException ignored) {
                Log.e("Reconnect", "Reconnect thread interrupted");
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        Log.e("Lifecycle", "Options menu created");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.e("Menu", "Menu item selected: " + item.getItemId());
        if (item.getItemId() == R.id.bluetooth) {
            Log.e("Menu", "Bluetooth menu clicked");

            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                Log.e("Permissions", "Requested BLUETOOTH_CONNECT permission");
                return true;
            }

            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                Toast.makeText(this, "🔵 Bluetooth turned ON", Toast.LENGTH_SHORT).show();
                Log.e("Bluetooth", "Bluetooth turned ON via menu");
            } else {
                Toast.makeText(this, "Bluetooth is already ON", Toast.LENGTH_SHORT).show();
                Log.e("Bluetooth", "Bluetooth already ON");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.e("Lifecycle", "onDestroy called — closing socket and stopping threads");
        super.onDestroy();
        isActivityRunning = false;

        try {
            if (socket != null) {
                socket.close();
                Log.e("Lifecycle", "Socket closed successfully");
            }
        } catch (IOException e) {
            Log.e("Lifecycle", "Error closing socket", e);
        }
    }
}