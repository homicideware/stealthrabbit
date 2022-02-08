package material.hunter.RecyclerViewAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        LayoutInflater.from(context)
            .inflate(R.layout.materialhunter_recyclerview_dialog_delete, viewGroup, false);
    return new ItemViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull final ItemViewHolder itemViewHolder, int i) {
    itemViewHolder.runOnChrootStartCheckBox.setText(
        customCommandsModelList.get(i).getCommandLabel());
  }

  @Override
  public int getItemCount() {
    return customCommandsModelList.size();
  }

  static class ItemViewHolder extends RecyclerView.ViewHolder {
    private final CheckBox runOnChrootStartCheckBox;

    private ItemViewHolder(View view) {
      super(view);
      runOnChrootStartCheckBox =
          view.findViewById(R.id.f_materialhunter_recyclerview_dialog_chkbox);
    }
  }
}