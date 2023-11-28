package org.homicideware.stealthrabbit.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.ui.activities.menu.OTGArmory.Item;

public class OTGArmoryRecyclerViewAdapter extends RecyclerView.Adapter<OTGArmoryRecyclerViewAdapter.ViewHolder> {

    private Activity activity;
    private Context context;
    private List<Item> devices;

    public OTGArmoryRecyclerViewAdapter(Activity activity, Context context, List<Item> devices) {
        this.activity = activity;
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.otg_armory_item,
                                parent,
                                false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = devices.get(position);
        holder.deviceName.setText("Loading...");
        new Thread(() -> {
            String deviceName = org.homicideware.stealthrabbit.ui.activities.menu.OTGArmory.Activity.getDeviceNameByVendorAndProductId(
                    item.getVendorId() + ":" + item.getProductId()
            );
            activity.runOnUiThread(() -> holder.deviceName.setText(deviceName));
        }).start();
        holder.deviceName.setSelected(true);
        holder.vendorInfo.setText("Vendor: " + item.getVendorName() + " (ID: " + item.getVendorId() + ")");
        holder.vendorInfo.setSelected(true);
        holder.productInfo.setText("Product: " + item.getProductName() + " (ID: " + item.getProductId() + ")");
        holder.productInfo.setSelected(true);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView deviceName;
        private TextView vendorInfo;
        private TextView productInfo;

        public ViewHolder(View v) {
            super(v);
            deviceName = v.findViewById(R.id.device_name);
            vendorInfo = v.findViewById(R.id.vendor_info);
            productInfo = v.findViewById(R.id.product_info);
        }
    }
}
