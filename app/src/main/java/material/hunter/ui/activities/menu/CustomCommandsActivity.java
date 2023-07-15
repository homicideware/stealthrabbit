package material.hunter.ui.activities.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import material.hunter.R;
import material.hunter.SQL.CustomCommandsSQL;
import material.hunter.adapters.CustomCommandsRecyclerViewAdapter;
import material.hunter.databinding.CustomCommandsActivityBinding;
import material.hunter.databinding.CustomCommandsDialogAddBinding;
import material.hunter.databinding.DialogMoveBinding;
import material.hunter.databinding.InputDialogBinding;
import material.hunter.models.CustomCommandsModel;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.BottomNavigationCardBehavior;
import material.hunter.utils.FABBehavior;
import material.hunter.utils.PathsUtil;
import material.hunter.viewdata.CustomCommandsData;
import material.hunter.viewmodels.CustomCommandsViewModel;

public class CustomCommandsActivity extends ThemedActivity {

    private static int targetPositionId;
    private CustomCommandsActivityBinding binding;
    private Activity activity;
    private Context context;
    private CustomCommandsRecyclerViewAdapter adapter;
    private static FloatingActionButton add;

    public static FloatingActionButton getAddButton() {
        return add;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        context = this;
        binding = CustomCommandsActivityBinding.inflate(getLayoutInflater());

        registerNotificationChannel();

        setContentView(binding.getRoot());

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        File sql_folder = new File(PathsUtil.APP_SD_SQLBACKUP_PATH);
        if (!sql_folder.exists()) {
            try {
                if (sql_folder.mkdir()) ;
                PathsUtil.showSnackBar(
                        this, binding.add, "Created directory for backing up config.", false);
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnackBar(
                        this, binding.add, "Failed to create directory " + PathsUtil.APP_SD_SQLBACKUP_PATH, false);
            }
        }

        CustomCommandsViewModel viewModel = new ViewModelProvider(this).get(CustomCommandsViewModel.class);
        viewModel.init(context);
        viewModel
                .getLiveDataCustomCommandsModelList()
                .observe(this, customCommandsModelList -> adapter.notifyDataSetChanged());

        adapter = new CustomCommandsRecyclerViewAdapter(activity, context,
                viewModel.getLiveDataCustomCommandsModelList().getValue());
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(adapter);

        add = binding.add;

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) add.getLayoutParams();
        layoutParams.setBehavior(new FABBehavior());

        addItem();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.database, menu);
        return true;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        InputDialogBinding binding = InputDialogBinding.inflate(inflater);

        switch (item.getItemId()) {
            case R.id.backup_db:
                binding.editText.setText(PathsUtil.APP_SD_SQLBACKUP_PATH + "/" + CustomCommandsSQL.TAG);
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity);
                adbBackup.setTitle("Full path to where you want to save the database:");
                adbBackup.setView(binding.getRoot());
                adbBackup.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                adbBackup.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                });
                AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult =
                                CustomCommandsData.getInstance()
                                        .backupData(
                                                CustomCommandsSQL.getInstance(
                                                        context),
                                                binding.editText
                                                        .getText()
                                                        .toString());
                        if (returnedResult == null) {
                            PathsUtil.showSnackBar(
                                    activity,
                                    this.binding.add,
                                    "db is successfully backup to "
                                            + binding.editText
                                            .getText()
                                            .toString(),
                                    false);
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(activity)
                                    .setTitle("Failed to backup the DB.")
                                    .setMessage(returnedResult)
                                    .show();
                        }
                        dialog.dismiss();
                    });
                });
                adBackup.show();
                break;
            case R.id.restore_db:
                binding.editText.setText(PathsUtil.APP_SD_SQLBACKUP_PATH + "/" + CustomCommandsSQL.TAG);
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity);
                adbRestore.setTitle("Full path of the db file from where you want to restore:");
                adbRestore.setView(binding.getRoot());
                adbRestore.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                adbRestore.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                });
                AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult =
                                CustomCommandsData.getInstance()
                                        .restoreData(
                                                CustomCommandsSQL.getInstance(
                                                        context),
                                                binding.editText
                                                        .getText()
                                                        .toString());
                        if (returnedResult == null) {
                            PathsUtil.showSnackBar(
                                    activity,
                                    this.binding.add,
                                    "db is successfully restored to "
                                            + binding.editText
                                            .getText()
                                            .toString(),
                                    false);
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(activity)
                                    .setTitle("Failed to restore the DB.")
                                    .setMessage(returnedResult)
                                    .show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.reset_db:
                CustomCommandsData.getInstance().resetData(CustomCommandsSQL.getInstance(context));
                break;
            case R.id.move_item:
                moveItem();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addItem() {
        binding.add.setOnClickListener(v -> {
            List<CustomCommandsModel> customCommandsModelList = CustomCommandsData.getInstance().customCommandsModelListFull;
            if (customCommandsModelList == null) return;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            CustomCommandsDialogAddBinding binding = CustomCommandsDialogAddBinding.inflate(inflater);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.mh_spinner_item,
                    getResources().getStringArray(R.array.custom_commands_send_to_array));
            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(activity, R.layout.mh_spinner_item,
                    getResources().getStringArray(R.array.custom_commands_execute_mode_array));

            binding.sendTo.setAdapter(adapter);
            binding.executeMode.setAdapter(adapter1);
            binding.sendTo.setText(adapter.getItem(0), false);
            binding.executeMode.setText(adapter1.getItem(0), false);
            binding.runOnBoot.setOnClickListener(v1 -> binding.runOnBootSwitch.toggle());

            ArrayList<String> commandLabelArrayList = new ArrayList<>();
            for (CustomCommandsModel customCommandsModel : customCommandsModelList) {
                commandLabelArrayList.add(customCommandsModel.getLabel());
            }

            ArrayAdapter<String> arrayAdapter =
                    new ArrayAdapter<>(
                            context,
                            android.R.layout.simple_spinner_item,
                            commandLabelArrayList);
            arrayAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);

            binding.position.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        binding.positionOf.setVisibility(View.INVISIBLE);
                        targetPositionId = 1;
                    } else if (position == 1) {
                        binding.positionOf.setVisibility(View.INVISIBLE);
                        targetPositionId = customCommandsModelList.size() + 1;
                    } else if (position == 2) {
                        binding.positionOf.setVisibility(View.VISIBLE);
                        binding.positionOf.setAdapter(arrayAdapter);
                        binding.positionOf.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id) {
                                        targetPositionId = position + 1;
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                    }
                                });
                    } else {
                        binding.positionOf.setVisibility(View.VISIBLE);
                        binding.positionOf.setAdapter(arrayAdapter);
                        binding.positionOf.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id) {
                                        targetPositionId = position + 2;
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                    }
                                });
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity);
            adb.setTitle("Add");
            adb.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            adb.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            });
            AlertDialog ad = adb.create();
            ad.setView(binding.getRoot());
            ad.setCancelable(true);
            ad.setOnShowListener(dialog -> {
                Button button = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(v1 -> {
                    if (binding.label
                            .getText()
                            .toString()
                            .isEmpty()) {
                        PathsUtil.showToast(
                                context, "Label can't be empty!", false);
                    } else if (binding.label
                            .getText()
                            .toString()
                            .isEmpty()) {
                        PathsUtil.showToast(
                                context,
                                "Command can't be empty!",
                                false);
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(
                                binding.label.getText().toString());
                        dataArrayList.add(
                                binding.command.getText().toString());
                        dataArrayList.add(
                                binding.sendTo.getText().toString());
                        dataArrayList.add(
                                binding.executeMode.getText().toString());
                        dataArrayList.add(binding.runOnBootSwitch.isChecked() ? "1" : "0");
                        CustomCommandsData.getInstance()
                                .addData(
                                        targetPositionId,
                                        dataArrayList,
                                        CustomCommandsSQL.getInstance(
                                                context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
        });
    }

    private void moveItem() {
        List<CustomCommandsModel> customCommandsModelList =
                CustomCommandsData.getInstance().customCommandsModelListFull;
        if (customCommandsModelList == null) return;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DialogMoveBinding binding = DialogMoveBinding.inflate(inflater);

        ArrayList<String> commandLabelArrayList = new ArrayList<>();
        for (CustomCommandsModel customCommandsModel : customCommandsModelList) {
            commandLabelArrayList.add(customCommandsModel.getLabel());
        }

        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_item,
                        commandLabelArrayList);
        arrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        binding.moveTarget.setAdapter(arrayAdapter);
        binding.moveTargetTo.setAdapter(arrayAdapter);

        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity);
        adb.setTitle("Move");
        adb.setPositiveButton(android.R.string.ok, (dialog, which) -> {
        });
        adb.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
        });
        AlertDialog ad = adb.create();
        ad.setView(binding.getRoot());
        ad.setCancelable(true);
        ad.setOnShowListener(dialog -> {
            Button button = ad.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setOnClickListener(v1 -> {
                int originalPositionIndex =
                        binding.moveTarget.getSelectedItemPosition();
                int targetPositionIndex =
                        binding.moveTargetTo.getSelectedItemPosition();
                if (originalPositionIndex == targetPositionIndex
                        || (binding.moveActions.getSelectedItemPosition() == 0
                        && targetPositionIndex
                        == (originalPositionIndex + 1))
                        || (binding.moveActions.getSelectedItemPosition() == 1
                        && targetPositionIndex
                        == (originalPositionIndex
                        - 1))) {
                    PathsUtil.showToast(
                            context,
                            "You are moving the item to the same"
                                    + " position, nothing to be moved.",
                            false);
                } else {
                    CustomCommandsData.getInstance()
                            .moveData(
                                    originalPositionIndex,
                                    targetPositionIndex,
                                    CustomCommandsSQL.getInstance(context));
                    PathsUtil.showSnackBar(activity, this.binding.add,  "Successfully moved item.", false);
                    ad.dismiss();
                }
            });
        });
        ad.show();
    }

    private void registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel =
                    new NotificationChannel(
                            "base",
                            "Base notifications",
                            NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
