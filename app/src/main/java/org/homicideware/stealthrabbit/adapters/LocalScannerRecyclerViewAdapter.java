package org.homicideware.stealthrabbit.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.ui.activities.menu.LocalScanner.Item;

public class LocalScannerRecyclerViewAdapter extends RecyclerView.Adapter<LocalScannerRecyclerViewAdapter.ViewHolder> {

    private Activity activity;
    private Context context;
    private ArrayList<Item> devices;

    public LocalScannerRecyclerViewAdapter(Activity activity, Context context, ArrayList<Item> devices) {
        this.activity = activity;
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView deviceType;
        public TextView dhcpName;
        public TextView ip;
        public TextView manufacturer;
        public TextView model;
        public ImageView more;

        public ViewHolder(View v) {
            super(v);
            deviceType = v.findViewById(R.id.device_type);
            dhcpName = v.findViewById(R.id.dhcp_name);
            ip = v.findViewById(R.id.ip);
            manufacturer = v.findViewById(R.id.manufacturer);
            model = v.findViewById(R.id.model);
            more = v.findViewById(R.id.more);
        }
    }
}
