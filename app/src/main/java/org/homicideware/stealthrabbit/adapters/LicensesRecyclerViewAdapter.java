package org.homicideware.stealthrabbit.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.models.LicenseModel;

public class LicensesRecyclerViewAdapter
        extends RecyclerView.Adapter<LicensesRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<LicenseModel> list;

    public LicensesRecyclerViewAdapter(
            Context context, List<LicenseModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.licenses_item,
                                parent,
                                false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        LicenseModel model = list.get(position);
        holder.title.setText(model.getTitle());
        holder.license.setText(model.getLicense());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView license;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            license = v.findViewById(R.id.license);
        }
    }
}
