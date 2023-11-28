package org.homicideware.stealthrabbit.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.SQL.ServicesSQL;
import org.homicideware.stealthrabbit.databinding.ServicesDialogEditBinding;
import org.homicideware.stealthrabbit.models.ServicesModel;
import org.homicideware.stealthrabbit.ui.activities.menu.ServicesActivity;
import org.homicideware.stealthrabbit.utils.PathsUtil;
import org.homicideware.stealthrabbit.viewdata.ServicesData;

public class ServicesRecyclerViewAdapter extends RecyclerView.Adapter<ServicesRecyclerViewAdapter.ItemViewHolder> {

    private final Context context;
    private final List<ServicesModel> list;
    private final ArrayList<Integer> selectedPositionList = new ArrayList<>();
    private final Activity activity;
    boolean isSelectingEnable = false;
    boolean isSelectAll = false;

    public ServicesRecyclerViewAdapter(
            Activity activity, Context context, List<ServicesModel> list) {
        this.activity = activity;
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ServicesRecyclerViewAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.services_item, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder itemViewHolder, int position) {
        itemViewHolder.label.setText(list.get(position).getLabel());
        Spannable tempStatusTextView = new SpannableString(list.get(position).getStatus());
        tempStatusTextView.setSpan(
                new ForegroundColorSpan(
                        list.get(position).getStatus().startsWith("[+]")
                                ? Color.GREEN
                                : Color.parseColor("#D81B60")),
                0,
                3,
                0);
        itemViewHolder.status.setText(tempStatusTextView);
        itemViewHolder.card.setChecked(isSelectAll);
        itemViewHolder.toggle.setEnabled(!isSelectAll);
        itemViewHolder.card.setOnLongClickListener(v -> {
            if (!isSelectingEnable) {
                ActionMode.Callback callback = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(@NonNull ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.recycler_multiselecting, menu);
                        ServicesActivity.getAddButton().setEnabled(false);
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
                                    PathsUtil.showSnackBar(activity, ServicesActivity.getAddButton(), "Nothing to be deleted.", false);
                                } else {
                                    ArrayList<Integer> mSelectedPositionList = new ArrayList<>(selectedPositionList);
                                    ArrayList<Integer> selectedTargetIds = new ArrayList<>(mSelectedPositionList);
                                    selectedTargetIds.replaceAll(i -> i + 1);
                                    ServicesData.getInstance().deleteData(
                                            mSelectedPositionList,
                                            selectedTargetIds,
                                            ServicesSQL.getInstance(context));
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
                        itemViewHolder.toggle.setEnabled(true);
                        itemViewHolder.card.setChecked(false);
                        ServicesActivity.getAddButton().setEnabled(true);
                    }
                };
                activity.startActionMode(callback);
            } else {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ServicesDialogEditBinding binding = ServicesDialogEditBinding.inflate(inflater);

                binding.runOnBoot.setOnClickListener(v1 -> binding.runOnBootSwitch.toggle());
                binding.label.setText(
                        ServicesData.getInstance()
                                .servicesModelListFull
                                .get(
                                        ServicesData.getInstance()
                                                .servicesModelListFull
                                                .indexOf(list.get(position)))
                                .getLabel());
                binding.commandStart.setText(
                        ServicesData.getInstance()
                                .servicesModelListFull
                                .get(
                                        ServicesData.getInstance()
                                                .servicesModelListFull
                                                .indexOf(list.get(position)))
                                .getCommandForStartingService());
                binding.commandStop.setText(
                        ServicesData.getInstance()
                                .servicesModelListFull
                                .get(
                                        ServicesData.getInstance()
                                                .servicesModelListFull
                                                .indexOf(list.get(position)))
                                .getCommandForStoppingService());
                binding.commandCheck.setText(
                        ServicesData.getInstance()
                                .servicesModelListFull
                                .get(
                                        ServicesData.getInstance()
                                                .servicesModelListFull
                                                .indexOf(list.get(position)))
                                .getCommandForCheckingService());
                binding.runOnBootSwitch.setChecked(
                        ServicesData.getInstance()
                                .servicesModelListFull
                                .get(
                                        ServicesData.getInstance()
                                                .servicesModelListFull
                                                .indexOf(list.get(position)))
                                .getRunOnChrootStart()
                                .equals("1"));
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                adb.setTitle("Edit");
                adb.setView(binding.getRoot());
                adb.setCancelable(true);
                adb.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                AlertDialog ad = adb.create();
                ad.setOnShowListener(dialog -> {
                    Button buttonEdit = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonEdit.setOnClickListener(v1 -> {
                        if (binding.label.getText().toString().isEmpty()) {
                            PathsUtil.showToast(context, "Label cannot be empty!", false);
                        } else if (binding.commandStart
                                .getText()
                                .toString()
                                .isEmpty()) {
                            PathsUtil.showToast(
                                    context,
                                    "Start command cannot be empty!",
                                    false);
                        } else if (binding.commandStop
                                .getText()
                                .toString()
                                .isEmpty()) {
                            PathsUtil.showToast(
                                    context,
                                    "Stop command cannot be empty!",
                                    false);
                        } else if (binding.commandCheck
                                .getText()
                                .toString()
                                .isEmpty()) {
                            PathsUtil.showToast(
                                    context, "Command for checking status cannot be empty!", false);
                        } else {
                            ArrayList<String> dataArrayList = new ArrayList<>();
                            dataArrayList.add(binding.label.getText().toString());
                            dataArrayList.add(binding.commandStart.getText().toString());
                            dataArrayList.add(binding.commandStop.getText().toString());
                            dataArrayList.add(binding.commandCheck.getText().toString());
                            dataArrayList.add(binding.runOnBootSwitch.isChecked() ? "1" : "0");
                            ServicesData.getInstance()
                                    .editData(
                                            ServicesData.getInstance()
                                                    .servicesModelListFull
                                                    .indexOf(list.get(position)),
                                            dataArrayList,
                                            ServicesSQL.getInstance(context));
                            selectedPositionList.remove(Integer.valueOf(itemViewHolder.getAdapterPosition()));
                            ad.dismiss();
                        }
                    });
                });
                ad.show();
            }
            return false;
        });
        itemViewHolder.card.setOnClickListener(v -> {
            if (isSelectingEnable) {
                itemClickListener(itemViewHolder);
            }
        });
        itemViewHolder.toggle.setChecked(list.get(position).getStatus().startsWith("[+]"));
        itemViewHolder.toggle.setOnClickListener(v -> {
            if (((MaterialSwitch) v).isChecked()) {
                ServicesData.getInstance().startServiceForItem(position, itemViewHolder.toggle);
            } else {
                ServicesData.getInstance().stopServiceForItem(position, itemViewHolder.toggle, context);
            }
        });
    }

    private void itemClickListener(@NonNull ItemViewHolder holder) {
        if (holder.card.isChecked()) {
            holder.card.setChecked(false);
            holder.toggle.setEnabled(true);
            selectedPositionList.remove(Integer.valueOf(holder.getAdapterPosition()));
        } else {
            holder.card.setChecked(true);
            holder.toggle.setEnabled(false);
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
        private final TextView status;
        private final MaterialSwitch toggle;

        private ItemViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.card);
            label = view.findViewById(R.id.label);
            status = view.findViewById(R.id.status);
            toggle = view.findViewById(R.id.toggle);
        }
    }
}
