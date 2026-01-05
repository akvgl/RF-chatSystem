package com.example.wavelynx_minor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class DeviceListActivity extends AppCompatActivity {

    Button bluetoothBtn, pairedBtn;
    ListView deviceList;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] pairedDevicesArray;
    ArrayList<String> deviceNames = new ArrayList<>();

    // Common UUID for Bluetooth connection
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_page);

        bluetoothBtn = findViewById(R.id.bluetooth);
        pairedBtn = findViewById(R.id.paireddevice);
        deviceList = findViewById(R.id.listview);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothBtn.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            } else if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                Toast.makeText(this, "Bluetooth turned ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth is already ON", Toast.LENGTH_SHORT).show();
            }
        });

        pairedBtn.setOnClickListener(v -> showPairedDevices());

        deviceList.setOnItemClickListener((adapterView, view, position, id) -> {
            BluetoothDevice device = pairedDevicesArray[position];

            // ✅ Pass the Bluetooth device address to ChatActivity
            Intent intent = new Intent(DeviceListActivity.this, ChatActivity.class);
            intent.putExtra("device_address", device.getAddress());
            startActivity(intent);
        });
    }

    private void showPairedDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please turn ON Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        deviceNames.clear();

        if (devices.size() > 0) {
            pairedDevicesArray = new BluetoothDevice[devices.size()];
            int index = 0;
            for (BluetoothDevice device : devices) {
                pairedDevicesArray[index++] = device;
                deviceNames.add(device.getName());
            }
        } else {
            deviceNames.add("No paired devices found");
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        deviceList.setAdapter(adapter);
    }
}
