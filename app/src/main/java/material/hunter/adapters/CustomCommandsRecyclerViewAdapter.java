package material.hunter.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import material.hunter.R;
import material.hunter.SQL.CustomCommandsSQL;
import material.hunter.databinding.CustomCommandsDialogEditBinding;
import material.hunter.models.CustomCommandsModel;
import material.hunter.ui.activities.menu.CustomCommandsActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.viewdata.CustomCommandsData;

public class CustomCommandsRecyclerViewAdapter extends RecyclerView.Adapter<CustomCommandsRecyclerViewAdapter.ItemViewHolder> {

    private final Activity activity;
    private final Context context;
    private final List<CustomCommandsModel> list;
    private final ArrayList<Integer> selectedPositionList = new ArrayList<>();
    boolean isSelectingEnable = false;
    boolean isSelectAll = false;

    public CustomCommandsRecyclerViewAdapter(
            Activity activity, Context context, List<CustomCommandsModel> list) {
        this.activity = activity;
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public CustomCommandsRecyclerViewAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_commands_item, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final CustomCommandsRecyclerViewAdapter.ItemViewHolder itemViewHolder, int position) {
        itemViewHolder.label.setText(list.get(position).getLabel());
        itemViewHolder.description.setText(list.get(position).getEnv() + ", ");
        itemViewHolder.description.append(list.get(position).getMode() + ", ");
        itemViewHolder.description.append(list.get(position).getRunOnBoot().equals("1")
                ? "run on boot"
                : "don't run on boot");
        itemViewHolder.card.setChecked(isSelectAll);
        itemViewHolder.run.setEnabled(!isSelectAll);
        itemViewHolder.card.setOnLongClickListener(v -> {
            if (!isSelectingEnable) {
                ActionMode.Callback callback = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(@NonNull ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.recycler_multiselecting, menu);
                        CustomCommandsActivity.getAddButton().setEnabled(false);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        isSelectingEnable = true;
                        itemClickListener(itemViewHolder);
                        return true;
                    }

                    @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
                    @Override
                    public boolean onActionItemClicked(ActionMode mode, @NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.delete:
                                if (selectedPositionList.size() == 0) {
                                    PathsUtil.showSnackBar(activity, "Nothing to be deleted.", false);
                                } else {
                                    ArrayList<Integer> mSelectedPositionList = new ArrayList<>(selectedPositionList);
                                    ArrayList<Integer> selectedTargetIds = new ArrayList<>(mSelectedPositionList);
                                    selectedTargetIds.replaceAll(i -> i + 1);
                                    CustomCommandsData.getInstance().deleteData(
                                            mSelectedPositionList,
                                            selectedTargetIds,
                                            CustomCommandsSQL.getInstance(context));
                                }
                                mode.finish();
                                break;
                            case R.id.select_all:
                                if (selectedPositionList.size() == list.size()) {
                                    isSelectAll = false;
                                    selectedPositionList.clear();
                                } else {
                                    isSelectAll = true;
                                    selectedPositionList.clear();
                                    for (int i = 0; i < list.size(); i++) {
                                        selectedPositionList.add(i);
                                    }
                                }
                                notifyDataSetChanged();
                                break;
                            default:
                                return false;
                        }
                        return true;
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        isSelectingEnable = false;
                        isSelectAll = false;
                        selectedPositionList.clear();
                        itemViewHolder.run.setEnabled(true);
                        itemViewHolder.card.setChecked(false);
                        CustomCommandsActivity.getAddButton().setEnabled(true);
                    }
                };
                activity.startActionMode(callback);
            } else {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                CustomCommandsDialogEditBinding binding = CustomCommandsDialogEditBinding.inflate(inflater);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.mh_spinner_item,
                        context.getResources().getStringArray(R.array.custom_commands_send_to_array));
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(activity, R.layout.mh_spinner_item,
                        context.getResources().getStringArray(R.array.custom_commands_execute_mode_array));

                binding.runOnBoot.setOnClickListener(v1 -> binding.runOnBootSwitch.toggle());
                binding.label.setText(
                        CustomCommandsData.getInstance()
                                .customCommandsModelListFull
                                .get(
                                        CustomCommandsData.getInstance()
                                                .customCommandsModelListFull
                                                .indexOf(list.get(position)))
                                .getLabel());
                binding.command.setText(
                        CustomCommandsData.getInstance()
                                .customCommandsModelListFull
                                .get(
                                        CustomCommandsData.getInstance()
                                                .customCommandsModelListFull
                                                .indexOf(list.get(position)))
                                .getCommand());
                binding.sendTo.setAdapter(adapter);
                binding.sendTo.setText(
                        CustomCommandsData.getInstance()
                                .customCommandsModelListFull
                                .get(
                                        CustomCommandsData.getInstance()
                                                .customCommandsModelListFull
                                                .indexOf(list.get(position)))
                                .getEnv(),
                        false);
                binding.executeMode.setAdapter(adapter1);
                binding.executeMode.setText(
                        CustomCommandsData.getInstance()
                                .customCommandsModelListFull
                                .get(
                                        CustomCommandsData.getInstance()
                                                .customCommandsModelListFull
                                                .indexOf(list.get(position)))
                                .getMode(),
                        false);
                binding.runOnBootSwitch.setChecked(
                        CustomCommandsData.getInstance()
                                .customCommandsModelListFull
                                .get(
                                        CustomCommandsData.getInstance()
                                                .customCommandsModelListFull
                                                .indexOf(list.get(position)))
                                .getRunOnBoot()
                                .equals("1"));

                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                adb.setTitle("Edit");
                adb.setView(binding.getRoot());
                adb.setCancelable(true);
                adb.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                AlertDialog ad = adb.create();
                ad.setOnShowListener(dialog -> {
                    Button positiveButton = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(v1 -> {
                        if (binding.label
                                .getText()
                                .toString()
                                .isEmpty()) {
                            PathsUtil.showToast(context, "Label can't be empty!", false);
                        } else if (binding.command
                                .getText()
                                .toString()
                                .isEmpty()) {
                            PathsUtil.showToast(
                                    context,
                                    "Command string can't be empty!",
                                    false);
                        } else {
                            ArrayList<String> dataArrayList = new ArrayList<>();
                            dataArrayList.add(binding.label.getText().toString());
                            dataArrayList.add(binding.command.getText().toString());
                            dataArrayList.add(binding.sendTo.getText().toString());
                            dataArrayList.add(binding.executeMode.getText().toString());
                            dataArrayList.add(binding.runOnBootSwitch.isChecked() ? "1" : "0");
                            CustomCommandsData.getInstance()
                                    .editData(
                                            CustomCommandsData.getInstance()
                                                    .customCommandsModelListFull
                                                    .indexOf(list.get(position)),
                                            dataArrayList,
                                            CustomCommandsSQL.getInstance(context));
                            selectedPositionList.remove(Integer.valueOf(itemViewHolder.getAdapterPosition()));
                            ad.dismiss();
                        }
                    });
                });
                ad.show();
            }
            return true;
        });
        itemViewHolder.card.setOnClickListener(v -> {
            if (isSelectingEnable) {
                itemClickListener(itemViewHolder);
            }
        });
        itemViewHolder.run.setOnClickListener(
                v -> CustomCommandsData.getInstance().runCommandForItem(activity, context, position));
    }

    private void itemClickListener(@NonNull ItemViewHolder holder) {
        if (holder.card.isChecked()) {
            holder.card.setChecked(false);
            holder.run.setEnabled(true);
            selectedPositionList.remove(Integer.valueOf(holder.getAdapterPosition()));
        } else {
            holder.card.setChecked(true);
            holder.run.setEnabled(false);
            selectedPositionList.add(holder.getAdapterPosition());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView label;
        private final TextView description;
        private final Button run;

        private ItemViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.card);
            label = view.findViewById(R.id.label);
            description = view.findViewById(R.id.description);
            run = view.findViewById(R.id.run);
        }
    }
}
