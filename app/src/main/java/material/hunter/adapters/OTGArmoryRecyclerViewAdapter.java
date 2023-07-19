package material.hunter.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import material.hunter.R;
import material.hunter.ui.activities.menu.OTGArmory.Item;

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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = devices.get(position);
        holder.deviceName.setText(item.getDeviceName());
        holder.vendorInfo.setText("Vendor: " + item.getVendorName() + " (ID: " + item.getVendorId() + ")");
        holder.productInfo.setText("Product: " + item.getProductName() + " (ID: " + item.getProductId() + ")");
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
