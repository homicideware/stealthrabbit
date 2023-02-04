package material.hunter.RecyclerViewAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

import material.hunter.R;
import material.hunter.models.CustomCommandsModel;

public class CustomCommandsRecyclerViewAdapterDeleteItems
        extends RecyclerView.Adapter<CustomCommandsRecyclerViewAdapterDeleteItems.ItemViewHolder> {

    private final Context context;
    private final List<CustomCommandsModel> customCommandsModelList;

    public CustomCommandsRecyclerViewAdapterDeleteItems(
            Context context, List<CustomCommandsModel> customCommandsModelList) {
        this.context = context;
        this.customCommandsModelList = customCommandsModelList;
    }

    @NonNull
    @Override
    public CustomCommandsRecyclerViewAdapterDeleteItems.ItemViewHolder onCreateViewHolder(
            @NonNull ViewGroup viewGroup, int i) {
        View view =
                LayoutInflater.from(context).inflate(R.layout.dialog_delete_item, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder itemViewHolder, int i) {
        itemViewHolder.itemCheckBox.setText(customCommandsModelList.get(i).getLabel());
    }

    @Override
    public int getItemCount() {
        return customCommandsModelList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox itemCheckBox;

        private ItemViewHolder(View view) {
            super(view);
            itemCheckBox = view.findViewById(R.id.itemCheckBox);
        }
    }
}
