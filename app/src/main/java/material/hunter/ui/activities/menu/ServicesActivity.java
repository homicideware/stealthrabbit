package material.hunter.ui.activities.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import material.hunter.R;
import material.hunter.SQL.ServicesSQL;
import material.hunter.adapters.ServicesRecyclerViewAdapter;
import material.hunter.databinding.DialogMoveBinding;
import material.hunter.databinding.InputDialogBinding;
import material.hunter.databinding.ServicesActivityBinding;
import material.hunter.databinding.ServicesDialogAddBinding;
import material.hunter.models.ServicesModel;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.BottomNavigationCardBehavior;
import material.hunter.utils.FABBehavior;
import material.hunter.utils.PathsUtil;
import material.hunter.viewdata.ServicesData;
import material.hunter.viewmodels.ServicesViewModel;

public class ServicesActivity extends ThemedActivity {

    private static int targetPositionId;
    private ServicesActivityBinding binding;
    private Activity activity;
    private Context context;
    private ServicesRecyclerViewAdapter adapter;
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
        binding = ServicesActivityBinding.inflate(getLayoutInflater());

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

        ServicesViewModel servicesViewModel = new ViewModelProvider(this).get(ServicesViewModel.class);
        servicesViewModel.init(activity, context);
        servicesViewModel
                .getLiveDataServicesModelList()
                .observe(this, ServicesModelList -> adapter.notifyDataSetChanged());

        adapter = new ServicesRecyclerViewAdapter(
                activity, context, servicesViewModel.getLiveDataServicesModelList().getValue());
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
                binding.editText.setText(PathsUtil.APP_SD_SQLBACKUP_PATH + "/" + ServicesSQL.TAG);
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
                                ServicesData.getInstance()
                                        .backupData(
                                                ServicesSQL.getInstance(context),
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
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Failed to backup the DB.")
                                    .setMessage(returnedResult)
                                    .create()
                                    .show();
                        }
                        dialog.dismiss();
                    });
                });
                adBackup.show();
                break;
            case R.id.restore_db:
                binding.editText.setText(PathsUtil.APP_SD_SQLBACKUP_PATH + "/" + ServicesSQL.TAG);
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
                                ServicesData.getInstance()
                                        .restoreData(
                                                ServicesSQL.getInstance(context),
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
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Failed to restore the DB.")
                                    .setMessage(returnedResult)
                                    .create()
                                    .show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.reset_db:
                ServicesData.getInstance().resetData(ServicesSQL.getInstance(context));
                break;
            case R.id.move_item:
                moveItem();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        ServicesData.getInstance().refreshData();
    }

    private void addItem() {
        binding.add.setOnClickListener(v -> {
            List<ServicesModel> servicesModelList = ServicesData.getInstance().servicesModelListFull;
            if (servicesModelList == null) return;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ServicesDialogAddBinding binding = ServicesDialogAddBinding.inflate(inflater);

            ArrayList<String> serviceNameArrayList = new ArrayList<>();
            for (ServicesModel servicesModel : servicesModelList) {
                serviceNameArrayList.add(servicesModel.getLabel());
            }

            ArrayAdapter<String> arrayAdapter =
                    new ArrayAdapter<>(
                            context,
                            android.R.layout.simple_spinner_item,
                            serviceNameArrayList);
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
                        targetPositionId = servicesModelList.size() + 1;
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
                    if (binding.label.getText().toString().isEmpty()) {
                        PathsUtil.showToast(
                                context, "Label can't be empty!", false);
                    } else if (binding.commandStart
                            .getText()
                            .toString()
                            .isEmpty()) {
                        PathsUtil.showToast(
                                context,
                                "Start command can't be empty!",
                                false);
                    } else if (binding.commandStop
                            .getText()
                            .toString()
                            .isEmpty()) {
                        PathsUtil.showToast(
                                context,
                                "Stop command can't be empty",
                                false);
                    } else if (binding.commandCheck
                            .getText()
                            .toString()
                            .isEmpty()) {
                        PathsUtil.showToast(
                                context,
                                "Command for checking status can't be empty!",
                                false);
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(binding.label.getText().toString());
                        dataArrayList.add(binding.commandStart.getText().toString());
                        dataArrayList.add(binding.commandStop.getText().toString());
                        dataArrayList.add(binding.commandCheck.getText().toString());
                        dataArrayList.add(binding.runOnBootSwitch.isChecked() ? "1" : "0");
                        ServicesData.getInstance()
                                .addData(
                                        targetPositionId,
                                        dataArrayList,
                                        ServicesSQL.getInstance(context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
        });
    }

    private void moveItem() {
        List<ServicesModel> servicesModelList =
                ServicesData.getInstance().servicesModelListFull;
        if (servicesModelList == null) return;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DialogMoveBinding binding = DialogMoveBinding.inflate(inflater);

        ArrayList<String> commandLabelArrayList = new ArrayList<>();
        for (ServicesModel servicesModel : servicesModelList) {
            commandLabelArrayList.add(servicesModel.getLabel());
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
        adb.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        adb.setPositiveButton(android.R.string.ok, (dialog, which) -> {
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
                    ServicesData.getInstance()
                            .moveData(
                                    originalPositionIndex,
                                    targetPositionIndex,
                                    ServicesSQL.getInstance(context));
                    PathsUtil.showSnackBar(activity, this.binding.add, "Successfully moved item.", false);
                    ad.dismiss();
                }
            });
        });
        ad.show();
    }
}
