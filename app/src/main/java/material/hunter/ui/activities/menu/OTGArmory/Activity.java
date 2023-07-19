package material.hunter.ui.activities.menu.OTGArmory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import material.hunter.adapters.OTGArmoryRecyclerViewAdapter;
import material.hunter.databinding.OtgArmoryActivityBinding;
import material.hunter.ui.activities.ThemedActivity;

public class Activity extends ThemedActivity {

    private final ArrayList<Item> devices = new ArrayList<>();
    private OtgArmoryActivityBinding binding;
    private OTGArmoryRecyclerViewAdapter adapter;
    private Timer timer;
    private boolean isScanning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = OtgArmoryActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new OTGArmoryRecyclerViewAdapter(this, this, devices);
        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(adapter);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isScanning)
                    runOnUiThread(() -> scanDevices());
            }
        }, 0, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void scanDevices() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        devices.clear();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Item item = new Item(
                    device.getDeviceName(),
                    device.getVendorId(),
                    device.getManufacturerName(),
                    device.getProductId(),
                    device.getProductName());
            devices.add(item);
        }
        adapter.notifyDataSetChanged();
    }
}
