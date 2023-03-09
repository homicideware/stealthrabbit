package material.hunter.ui.activities.menu;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.NetworkingActivityBinding;
import material.hunter.services.BluetoothD;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;

public class NetworkingActivity extends ThemedActivity {

    private NetworkingActivityBinding binding;
    private View _view;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;
    private TextInputLayout interfacesLayout;
    private AutoCompleteTextView interfaces;
    private Button interfacesUpdate;
    private Button renameInterface;
    private Button macchanger;
    private TextView bluebinderProcesses;
    private Button runBluebinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NetworkingActivityBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);
        _view = root;

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        MaterialToolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isBluebinderInstalled();
        loadInterfaces();

        interfacesLayout = binding.interfacesLayout;
        interfaces = binding.interfaces;
        interfacesUpdate = binding.interfacesUpdate;
        renameInterface = binding.renameInterface;
        macchanger = binding.macchanger;
        runBluebinder = binding.runBluebinder;
        bluebinderProcesses = binding.bluebinderProcesses;

        interfacesUpdate.setOnClickListener(v -> loadInterfaces());

        interfaces.setOnItemClickListener((adapterView, v, pos, l) -> prefs.edit().putString("macchanger_interface", interfaces.getText().toString()).apply());

        renameInterface.setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.input_dialog, null);
            TextInputEditText newName = view.findViewById(R.id.editText);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Rename interface")
                    .setMessage("Enter new interface name:")
                    .setView(view)
                    .setPositiveButton(android.R.string.ok, (di, i) -> renameInterface(newName.getText().toString()))
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .show();
        });

        macchanger.setOnClickListener(v -> {
            finish();
            Intent intent = new Intent(this, MACChangerActivity.class);
            startActivity(intent);
        });

        runBluebinder.setOnClickListener(v -> runBluebinder());

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getBluebinderProcesses();
            }
        }, 0, 5000);
    }

    private void loadInterfaces() {
        executor.execute(() -> {
            String[] list = new ShellUtils().executeCommandAsRootWithOutput("iw dev | grep \"Interface\" | sed -r 's/Interface//g' | xargs | sed -r 's/ /\n/g'").split("\n");
            ArrayList<String> mInterfaces = new ArrayList<>(Arrays.asList(list));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.mh_spinner_item, mInterfaces);
            new Handler(Looper.getMainLooper()).post(() -> interfaces.setAdapter(adapter));
            String previousWlan = prefs.getString("macchanger_interface", "");
            if (previousWlan.isEmpty()) {
                String preferredInterface = list[0];
                for (String iInterface : mInterfaces) {
                    if (iInterface.startsWith("wl")) {
                        preferredInterface = iInterface;
                    }
                }
                String finalPreferredInterface = preferredInterface;
                new Handler(Looper.getMainLooper()).post(() -> {
                    interfaces.setText(finalPreferredInterface, false);
                    prefs.edit().putString("macchanger_interface", finalPreferredInterface).apply();
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> interfaces.setText(previousWlan, false));
            }
        });
    }

    private void getBluebinderProcesses() {
        executor.execute(() -> {
            String processes = new ShellUtils().executeCommandAsChrootWithOutput("pidof bluebinder");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!processes.isEmpty()) {
                    bluebinderProcesses.setVisibility(View.VISIBLE);
                    bluebinderProcesses.setText("Running processes: " + processes.replaceAll("\n", ", "));
                } else {
                    bluebinderProcesses.setVisibility(View.GONE);
                    bluebinderProcesses.setText("");
                }
            });
        });
    }

    private boolean isServiceRunning(Class<?> zClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (zClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void runBluebinder() {
        if (isServiceRunning(BluetoothD.class)) {
            Intent service = new Intent(this, BluetoothD.class);
            service.setAction(BluetoothD.ACTION_STOP);
            ContextCompat.startForegroundService(this, service);
        } else {
            Intent service = new Intent(this, BluetoothD.class);
            service.setAction(BluetoothD.ACTION_START);
            ContextCompat.startForegroundService(this, service);
        }
    }

    private void isBluebinderInstalled() {
        executor.execute(() -> {
            int code = new ShellUtils().executeCommandAsChrootWithReturnCode("which bluebinder");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (code == 1) {
                    bluebinderIsNotInstalledDialog();
                }
            });
        });
    }

    private void bluebinderIsNotInstalledDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Networking")
                .setMessage("bluebinder required, but it isn't installed in chroot.")
                .setPositiveButton("Install", (di, i) -> {
                    try {
                        terminalUtil.runCommand(
                                PathsUtil.APP_SCRIPTS_PATH +
                                        "/bootroot_exec 'apt update && apt install build-essential gcc git libglib2.0-dev libsystemd-dev libbluetooth-dev make -y " +
                                        "&& cd ~ && rm -rf bluebinder && mkdir -p bluebinder && cd bluebinder " +
                                        "&& git clone https://github.com/TMayfine/libglibutil --depth 1 " +
                                        "&& git clone https://github.com/TMayfine/libgbinder --depth 1 " +
                                        "&& git clone https://github.com/TMayfine/bluebinder --depth 1 " +
                                        "&& cd libglibutil && make -j$(nproc --all) && make install-dev -j$(nproc --all) && cd .. " +
                                        "&& cd libgbinder && make -j$(nproc --all) && make install-dev -j$(nproc --all) && cd .. " +
                                        "&& cd bluebinder && make -j$(nproc --all) && make install -j$(nproc --all) && cd .. " +
                                        "&& cd .. && rm -rf bluebinder'",
                                false);
                    } catch (ActivityNotFoundException e) {
                        if (prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX).equals(TerminalUtil.TERMINAL_TYPE_TERMUX)) {
                            terminalUtil.checkIsTermuxApiSupported();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        terminalUtil.showTerminalNotInstalledDialog();
                    } catch (SecurityException e) {
                        terminalUtil.showPermissionDeniedDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (di, i) -> {
                })
                .show();
    }

    private void renameInterface(String newInterfaceName) {
        executor.execute(() -> {
            int code = new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + interfaces.getText().toString() + " down");
            if (code == 0) {
                code = new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + interfaces.getText().toString() + " name " + newInterfaceName);
                if (code == 0) {
                    new Handler(Looper.getMainLooper()).post(() -> interfaces.setText(newInterfaceName));
                    prefs.edit().putString("macchanger_interface", newInterfaceName).apply();
                    new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + interfaces.getText().toString() + " up");
                    if (newInterfaceName.matches("(s|)wlan0")) {
                        new ShellUtils().executeCommandAsRootWithReturnCode("svc wifi enable");
                    }
                    new Handler(Looper.getMainLooper()).post(() ->
                            PathsUtil.showSnack(_view, "Interface successfully renamed!", false));
                } else {
                    int finalCode = code;
                    new Handler(Looper.getMainLooper()).post(() ->
                            PathsUtil.showSnack(_view, "Failed to rename interface. Code: " + finalCode, false));
                }
            }
        });
    }
}
