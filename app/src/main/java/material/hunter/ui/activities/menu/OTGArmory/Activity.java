package material.hunter.ui.activities.menu.OTGArmory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import material.hunter.adapters.OTGArmoryRecyclerViewAdapter;
import material.hunter.databinding.OtgArmoryActivityBinding;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;

public class Activity extends ThemedActivity {

    private final ArrayList<Item> devices = new ArrayList<>();
    private OtgArmoryActivityBinding binding;
    private OTGArmoryRecyclerViewAdapter adapter;
    private Timer timer;
    private static JSONArray usbIds;
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
        try {
            timer.cancel();
        } catch (IllegalStateException ignored) {
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void scanDevices() {
        if (!isScanning) {
            isScanning = true;
            if (usbIds == null || usbIds.length() == 0) {
                try {
                    loadUsbIds();
                    isScanning = false;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    PathsUtil.showToast(this, "Something broken, report this.", true);
                    finish();
                }
            } else {
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                ArrayList<Item> newDevices = new ArrayList<>();
                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();
                    StringBuilder vendorId = new StringBuilder(Integer.toHexString(device.getVendorId()));
                    while (vendorId.length() < 4) {
                        vendorId.insert(0, "0");
                    }
                    StringBuilder productId = new StringBuilder(Integer.toHexString(device.getProductId()));
                    while (productId.length() < 4) {
                        productId.insert(0, "0");
                    }
                    Item item = new Item(
                            device.getDeviceName(),
                            vendorId.toString(),
                            device.getManufacturerName(),
                            productId.toString(),
                            device.getProductName());
                    newDevices.add(item);
                }
                if (devices.size() != newDevices.size()) {
                    devices.clear();
                    devices.addAll(newDevices);
                    newDevices.clear();
                    if (devices.size() == 0) {
                        binding.progressIndicator.setVisibility(View.VISIBLE);
                        binding.status.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    } else {
                        binding.progressIndicator.setVisibility(View.INVISIBLE);
                        binding.status.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                }
                isScanning = false;
            }
        }
    }

    private void loadUsbIds() throws IOException, JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        JSONObject devices;
        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("extensions/usb.ids.json")));
        int value;
        while ((value = reader.read()) != -1) {
            stringBuilder.append((char) value);
        }
        devices = new JSONObject(stringBuilder.toString());
        usbIds = devices.getJSONArray("devices");
        binding.status.setText("No USB devices connected!");
    }

    public static String getDeviceNameByVendorAndProductId(String vendorAndProductId) {
        String deviceName = "Unknown";
        try {
            for (int i = 0; i < usbIds.length(); i++) {
                JSONObject temp = usbIds.getJSONObject(i);
                if (temp.has(vendorAndProductId)) {
                    deviceName = temp.getString(vendorAndProductId);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceName;
    }
}
