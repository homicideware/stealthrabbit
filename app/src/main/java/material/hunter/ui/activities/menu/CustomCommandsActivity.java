package material.hunter.ui.activities.menu;

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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import material.hunter.R;
import material.hunter.SQL.CustomCommandsSQL;
import material.hunter.adapters.CustomCommandsRecyclerViewAdapter;
import material.hunter.adapters.CustomCommandsRecyclerViewAdapterDeleteItems;
import material.hunter.models.CustomCommandsModel;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.viewdata.CustomCommandsData;
import material.hunter.viewmodels.CustomCommandsViewModel;

public class CustomCommandsActivity extends ThemedActivity {

    public static View _view;
    private static int targetPositionId;
    MaterialToolbar toolbar;
    private Activity activity;
    private Context context;
    private CustomCommandsRecyclerViewAdapter adapter;
    private Button addButton;
    private Button deleteButton;
    private Button moveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        activity = this;

        registerNotificationChannel();

        setContentView(R.layout.custom_commands_activity);

        _view = getWindow().getDecorView();
        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CustomCommandsViewModel customCommandsViewModel =
                new ViewModelProvider(this).get(CustomCommandsViewModel.class);
        customCommandsViewModel.init(context);
        customCommandsViewModel
                .getLiveDataCustomCommandsModelList()
                .observe(this, customCommandsModelList -> adapter.notifyDataSetChanged());

        adapter =
                new CustomCommandsRecyclerViewAdapter(
                        activity,
                        context,
                        customCommandsViewModel.getLiveDataCustomCommandsModelList().getValue());
        RecyclerView recyclerView = findViewById(R.id.f_customcommands_recyclerview);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        File usefulDirectory = new File(PathsUtil.APP_SD_SQLBACKUP_PATH);
        if (!usefulDirectory.exists()) {
            PathsUtil.showSnack(_view, "Creating directory for backups...", false);
            try {
                usefulDirectory.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnack(
                        _view,
                        "Failed to create directory: " + usefulDirectory,
                        false);
            }
        }

        addButton = findViewById(R.id.f_customcommands_addItemButton);
        deleteButton = findViewById(R.id.f_customcommands_deleteItemButton);
        moveButton = findViewById(R.id.f_customcommands_moveItemButton);

        addItem();
        deleteItems();
        moveItems();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.custom_commands, menu);
        final MenuItem searchItem = menu.findItem(R.id.f_customcommands_action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.getFilter().filter(newText);
                        return false;
                    }
                });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View promptView = inflater.inflate(R.layout.input_dialog, null);
        final EditText storedpathEditText = promptView.findViewById(R.id.editText);

        switch (item.getItemId()) {
            case R.id.f_customcommands_menu_backupDB:
                storedpathEditText.setText(
                        PathsUtil.APP_SD_SQLBACKUP_PATH + "/FragmentCustomCommands");
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity);
                adbBackup.setTitle("Save database");
                adbBackup.setMessage("Full path to where you want to save the database:");
                adbBackup.setView(promptView);
                adbBackup.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                final AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(
                        dialog -> {
                            final Button buttonOK =
                                    adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                            buttonOK.setOnClickListener(
                                    v -> {
                                        String returnedResult =
                                                CustomCommandsData.getInstance()
                                                        .backupData(
                                                                CustomCommandsSQL.getInstance(
                                                                        context),
                                                                storedpathEditText
                                                                        .getText()
                                                                        .toString());
                                        if (returnedResult == null) {
                                            PathsUtil.showSnack(
                                                    _view,
                                                    "db is successfully backup to "
                                                            + storedpathEditText
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
            case R.id.f_customcommands_menu_restoreDB:
                storedpathEditText.setText(
                        PathsUtil.APP_SD_SQLBACKUP_PATH + "/FragmentCustomCommands");
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity);
                adbRestore.setTitle("Full path of the db file from where you want to restore:");
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                });
                final AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(
                        dialog -> {
                            final Button buttonOK =
                                    adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                            buttonOK.setOnClickListener(
                                    v -> {
                                        String returnedResult =
                                                CustomCommandsData.getInstance()
                                                        .restoreData(
                                                                CustomCommandsSQL.getInstance(
                                                                        context),
                                                                storedpathEditText
                                                                        .getText()
                                                                        .toString());
                                        if (returnedResult == null) {
                                            PathsUtil.showSnack(
                                                    _view,
                                                    "db is successfully restored to "
                                                            + storedpathEditText
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
            case R.id.f_customcommands_menu_ResetToDefault:
                CustomCommandsData.getInstance().resetData(CustomCommandsSQL.getInstance(context));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addItem() {
        addButton.setOnClickListener(
                v -> {
                    List<CustomCommandsModel> customCommandsModelList =
                            CustomCommandsData.getInstance().customCommandsModelListFull;
                    if (customCommandsModelList == null) return;
                    final LayoutInflater inflater =
                            (LayoutInflater)
                                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View promptViewAdd =
                            inflater.inflate(R.layout.custom_commands_dialog_add, null);
                    final EditText commandLabelEditText =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_et_label);
                    final EditText commandEditText =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_et_command);
                    final Spinner sendToSpinner =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_sendto);
                    final Spinner execModeSpinner =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_execmode);
                    final SwitchMaterial runOnBootSwitch =
                            promptViewAdd.findViewById(
                                    R.id.f_customcommands_add_adb_switch_runonboot);
                    final Spinner insertPositions =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_positions);
                    final Spinner insertLabels =
                            promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_labels);

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

                    insertPositions.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {

                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent, View view, int position, long id) {
                                    // if Insert to Top
                                    if (position == 0) {
                                        insertLabels.setVisibility(View.INVISIBLE);
                                        targetPositionId = 1;
                                        // if Insert to Bottom
                                    } else if (position == 1) {
                                        insertLabels.setVisibility(View.INVISIBLE);
                                        targetPositionId = customCommandsModelList.size() + 1;
                                        // if Insert Before
                                    } else if (position == 2) {
                                        insertLabels.setVisibility(View.VISIBLE);
                                        insertLabels.setAdapter(arrayAdapter);
                                        insertLabels.setOnItemSelectedListener(
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
                                                    public void onNothingSelected(
                                                            AdapterView<?> parent) {
                                                    }
                                                });
                                        // if Insert After
                                    } else {
                                        insertLabels.setVisibility(View.VISIBLE);
                                        insertLabels.setAdapter(arrayAdapter);
                                        insertLabels.setOnItemSelectedListener(
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
                                                    public void onNothingSelected(
                                                            AdapterView<?> parent) {
                                                    }
                                                });
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });

                    MaterialAlertDialogBuilder adbAdd = new MaterialAlertDialogBuilder(activity);
                    adbAdd.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    });
                    final AlertDialog adAdd = adbAdd.create();
                    adAdd.setView(promptViewAdd);
                    adAdd.setCancelable(true);
                    // If you want the dialog to stay open after clicking OK, you need to do it this
                    // way...
                    adAdd.setOnShowListener(
                            dialog -> {
                                final Button buttonAdd =
                                        adAdd.getButton(DialogInterface.BUTTON_POSITIVE);
                                buttonAdd.setOnClickListener(
                                        v1 -> {
                                            if (commandLabelEditText
                                                    .getText()
                                                    .toString()
                                                    .isEmpty()) {
                                                PathsUtil.showToast(
                                                        context, "Label cannot be empty", false);
                                            } else if (commandEditText
                                                    .getText()
                                                    .toString()
                                                    .isEmpty()) {
                                                PathsUtil.showToast(
                                                        context,
                                                        "Command String cannot be empty",
                                                        false);
                                            } else {
                                                ArrayList<String> dataArrayList = new ArrayList<>();
                                                dataArrayList.add(
                                                        commandLabelEditText.getText().toString());
                                                dataArrayList.add(
                                                        commandEditText.getText().toString());
                                                dataArrayList.add(
                                                        sendToSpinner.getSelectedItem().toString());
                                                dataArrayList.add(
                                                        execModeSpinner
                                                                .getSelectedItem()
                                                                .toString());
                                                dataArrayList.add(
                                                        runOnBootSwitch.isChecked() ? "1" : "0");
                                                CustomCommandsData.getInstance()
                                                        .addData(
                                                                targetPositionId,
                                                                dataArrayList,
                                                                CustomCommandsSQL.getInstance(
                                                                        context));
                                                adAdd.dismiss();
                                            }
                                        });
                            });
                    adAdd.show();
                });
    }

    private void deleteItems() {
        deleteButton.setOnClickListener(
                v -> {
                    List<CustomCommandsModel> customCommandsModelList =
                            CustomCommandsData.getInstance().customCommandsModelListFull;
                    if (customCommandsModelList == null) return;
                    final LayoutInflater inflater =
                            (LayoutInflater)
                                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View promptViewDelete =
                            inflater.inflate(R.layout.dialog_delete, null, false);
                    final RecyclerView recyclerViewDeleteItem =
                            promptViewDelete.findViewById(R.id.recyclerview);
                    CustomCommandsRecyclerViewAdapterDeleteItems
                            customCommandsRecyclerViewAdapterDeleteItems =
                            new CustomCommandsRecyclerViewAdapterDeleteItems(
                                    context, customCommandsModelList);
                    LinearLayoutManager linearLayoutManagerDelete =
                            new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
                    recyclerViewDeleteItem.setLayoutManager(linearLayoutManagerDelete);
                    recyclerViewDeleteItem.setAdapter(customCommandsRecyclerViewAdapterDeleteItems);

                    MaterialAlertDialogBuilder adbDelete = new MaterialAlertDialogBuilder(activity);
                    adbDelete.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                    adbDelete.setPositiveButton("Delete", (dialog, which) -> {
                    });
                    final AlertDialog adDelete = adbDelete.create();
                    adDelete.setView(promptViewDelete);
                    adDelete.setCancelable(true);
                    adDelete.setOnShowListener(
                            dialog -> {
                                final Button buttonDelete =
                                        adDelete.getButton(DialogInterface.BUTTON_POSITIVE);
                                buttonDelete.setOnClickListener(
                                        v1 -> {
                                            RecyclerView.ViewHolder viewHolder;
                                            ArrayList<Integer> selectedPosition = new ArrayList<>();
                                            ArrayList<Integer> selectedTargetIds =
                                                    new ArrayList<>();
                                            for (int i = 0;
                                                 i < recyclerViewDeleteItem.getChildCount();
                                                 i++) {
                                                viewHolder =
                                                        recyclerViewDeleteItem
                                                                .findViewHolderForAdapterPosition(
                                                                        i);
                                                if (viewHolder != null) {
                                                    CheckBox box =
                                                            viewHolder.itemView.findViewById(
                                                                    R.id.itemCheckBox);
                                                    if (box.isChecked()) {
                                                        selectedPosition.add(i);
                                                        selectedTargetIds.add(i + 1);
                                                    }
                                                }
                                            }
                                            if (selectedPosition.size() != 0) {
                                                CustomCommandsData.getInstance()
                                                        .deleteData(
                                                                selectedPosition,
                                                                selectedTargetIds,
                                                                CustomCommandsSQL.getInstance(
                                                                        context));
                                                PathsUtil.showSnack(
                                                        _view,
                                                        "Successfully deleted "
                                                                + selectedPosition.size()
                                                                + " items.",
                                                        false);
                                                adDelete.dismiss();
                                            } else {
                                                PathsUtil.showToast(
                                                        context, "Nothing to be deleted.", false);
                                            }
                                        });
                            });
                    adDelete.show();
                });
    }

    private void moveItems() {
        moveButton.setOnClickListener(
                v -> {
                    List<CustomCommandsModel> customCommandsModelList =
                            CustomCommandsData.getInstance().customCommandsModelListFull;
                    if (customCommandsModelList == null) return;
                    final LayoutInflater inflater =
                            (LayoutInflater)
                                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View promptViewMove = inflater.inflate(R.layout.dialog_move, null, false);
                    final Spinner moveTarget = promptViewMove.findViewById(R.id.move_target);
                    final Spinner moveActions = promptViewMove.findViewById(R.id.move_actions);
                    final Spinner moveTargetTo = promptViewMove.findViewById(R.id.move_targetTo);

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
                    moveTarget.setAdapter(arrayAdapter);
                    moveTargetTo.setAdapter(arrayAdapter);

                    MaterialAlertDialogBuilder adbMove = new MaterialAlertDialogBuilder(activity);
                    adbMove.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                    adbMove.setPositiveButton("Move", (dialog, which) -> {
                    });
                    final AlertDialog adMove = adbMove.create();
                    adMove.setView(promptViewMove);
                    adMove.setCancelable(true);
                    adMove.setOnShowListener(
                            dialog -> {
                                final Button buttonMove =
                                        adMove.getButton(DialogInterface.BUTTON_POSITIVE);
                                buttonMove.setOnClickListener(
                                        v1 -> {
                                            int originalPositionIndex =
                                                    moveTarget.getSelectedItemPosition();
                                            int targetPositionIndex =
                                                    moveTargetTo.getSelectedItemPosition();
                                            if (originalPositionIndex == targetPositionIndex
                                                    || (moveActions.getSelectedItemPosition() == 0
                                                    && targetPositionIndex
                                                    == (originalPositionIndex + 1))
                                                    || (moveActions.getSelectedItemPosition() == 1
                                                    && targetPositionIndex
                                                    == (originalPositionIndex
                                                    - 1))) {
                                                PathsUtil.showToast(
                                                        context,
                                                        "You are moving the item to the same"
                                                                + " position, nothing to be moved.",
                                                        false);
                                            } else {
                                                /*if (moveActions.getSelectedItemPosition() == 1)
                                                    targetPositionIndex += 1;*/
                                                CustomCommandsData.getInstance()
                                                        .moveData(
                                                                originalPositionIndex,
                                                                targetPositionIndex,
                                                                CustomCommandsSQL.getInstance(
                                                                        context));
                                                PathsUtil.showSnack(
                                                        _view, "Successfully moved item.", false);
                                                adMove.dismiss();
                                            }
                                        });
                            });
                    adMove.show();
                });
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