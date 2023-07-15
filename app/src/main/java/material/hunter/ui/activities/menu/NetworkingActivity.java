package material.hunter.ui.activities.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.InputDialogBinding;
import material.hunter.databinding.NetworkingActivityBinding;
import material.hunter.services.BluetoothD;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;
import material.hunter.utils.Utils;

public class NetworkingActivity extends ThemedActivity {

    private Activity activity;
    private NetworkingActivityBinding binding;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        binding = NetworkingActivityBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        MaterialToolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadInterfaces();

        binding.interfacesUpdate.setOnClickListener(v -> loadInterfaces());

        binding.upDownInterface.setOnClickListener(v -> upDownInterface());

        binding.interfaces.setOnItemClickListener((adapterView, v, pos, l) -> {
            prefs.edit().putString("macchanger_interface", binding.interfaces.getText().toString()).apply();
            getInterfaceInformation();
            isInterfaceUp();
        });

        binding.renameInterface.setOnClickListener(v -> {
            InputDialogBinding binding1 = InputDialogBinding.inflate(getLayoutInflater());
            View view = binding1.getRoot();
            TextInputEditText newName = binding1.editText;
            newName.setText(binding.interfaces.getText().toString());
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Rename interface")
                    .setView(view)
                    .setPositiveButton(android.R.string.ok, (di, i) -> renameInterface(newName.getText().toString()))
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .show();
        });

        binding.macchanger.setOnClickListener(v -> {
            finish();
            Intent intent = new Intent(this, MACChangerActivity.class);
            startActivity(intent);
        });

        binding.runBluebinder.setOnClickListener(v -> runBluebinderIfItIsInstalled());

        executor.execute(this::isInterfaceUp);

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
            String[] list = new ShellUtils().executeCommandAsChrootWithOutput("iw dev | grep \"Interface\" | sed -r \"s/Interface//g\" | xargs").split(" ");
            ArrayList<String> mInterfaces = new ArrayList<>(Arrays.asList(list));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.mh_spinner_item, mInterfaces);
            new Handler(Looper.getMainLooper()).post(() -> binding.interfaces.setAdapter(adapter));
            String previousWlan = prefs.getString("macchanger_interface", "");
            if (previousWlan.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.interfaces.setText(list[0], false);
                    prefs.edit().putString("macchanger_interface", list[0]).apply();
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> binding.interfaces.setText(previousWlan, false));
            }
            new Handler(Looper.getMainLooper()).post(this::getInterfaceInformation);
        });
    }

    private void upDownInterface() {
        executor.execute(() -> {
            String mInterface = binding.interfaces.getText().toString();
            boolean isInterfaceUp = isInterfaceUp(mInterface);
            if (isInterfaceUp) {
                if (mInterface.matches("(s|)wlan0")) {
                    new ShellUtils().executeCommandAsRoot("svc wifi disable");
                }
                int downInterface = new ShellUtils().executeCommandAsChrootWithReturnCode("ifconfig " + mInterface + " down");
                if (downInterface == 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.upDownInterface.getBackground().setTint(getResources().getColor(R.color.green, getTheme()));
                        binding.upDownInterface.setText("UP interface");
                        binding.upDownInterface.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, getTheme()));
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showSnackBar(activity, "Error code: " + downInterface, false));
                }
            } else {
                int upInterface = new ShellUtils().executeCommandAsChrootWithReturnCode("ifconfig " + mInterface + " up");
                if (upInterface == 0) {
                    if (mInterface.matches("(s|)wlan0")) {
                        new ShellUtils().executeCommandAsRoot("svc wifi enable");
                    }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.upDownInterface.getBackground().setTint(getResources().getColor(R.color.red, getTheme()));
                        binding.upDownInterface.setText("Down interface");
                        binding.upDownInterface.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop, getTheme()));
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> PathsUtil.showSnackBar(activity, "Error code: " + upInterface, false));
                }
            }
            new Handler(Looper.getMainLooper()).post(this::getInterfaceInformation);
        });
    }

    private boolean isInterfaceUp(String mInterface) {
        return new ShellUtils().executeCommandAsRootWithOutput("cat /sys/class/net/" + mInterface + "/operstate").equals("up");
    }

    private void isInterfaceUp() {
        boolean isInterfaceUp = isInterfaceUp(prefs.getString("macchanger_interface", ""));
        if (isInterfaceUp) {
            new Handler(Looper.getMainLooper()).post(() -> {
                binding.upDownInterface.getBackground().setTint(getResources().getColor(R.color.red, getTheme()));
                binding.upDownInterface.setText("Down interface");
                binding.upDownInterface.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop, getTheme()));
            });
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                binding.upDownInterface.getBackground().setTint(getResources().getColor(R.color.green, getTheme()));
                binding.upDownInterface.setText("UP interface");
                binding.upDownInterface.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, getTheme()));
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void getInterfaceInformation() {
        executor.execute(() -> {
            String mInterface = prefs.getString("macchanger_interface", "");
            if (mInterface.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> binding.interfaceInformation.setText("Failed to get information about selected interface."));
            } else {
                boolean isInterfaceUp = isInterfaceUp(mInterface);
                String ifconfigObj = new ShellUtils().executeCommandAsChrootWithOutput("ifconfig " + mInterface);
                if (ifconfigObj.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> binding.interfaceInformation.setText("Failed to get information about selected interface."));
                } else {
                    String ipAddress = Utils.matchString(" +inet ((\\d{1,3}.){3}\\d{1,3})", ifconfigObj, 1);
                    String macAddress = Utils.matchString(" +ether ((\\w{2}:){5}\\w{2})", ifconfigObj, 1);
                    String broadcast = Utils.matchString(" +broadcast ((\\d{1,3}.){3}\\d{1,3})", ifconfigObj, 1);
                    String flags = Utils.matchString(" +flags=\\d*<(.*)>", ifconfigObj, 1);
                    StringBuilder information = new StringBuilder();
                    information.append("Interface is ")
                            .append(isInterfaceUp ? "up" : "down")
                            .append("\n")
                            .append(ipAddress.isEmpty() ? "" : "IP: " + ipAddress + "\n")
                            .append("MAC Address: ")
                            .append(macAddress)
                            .append("\n")
                            .append(broadcast.isEmpty() ? "" : "Broadcast: " + broadcast + "\n")
                            .append("Flags: ")
                            .append(flags);
                    new Handler(Looper.getMainLooper()).post(() -> binding.interfaceInformation.setText(information));
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void getBluebinderProcesses() {
        executor.execute(() -> {
            String processes = new ShellUtils().executeCommandAsChrootWithOutput("pidof bluebinder");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!processes.isEmpty()) {
                    binding.bluebinderProcesses.setVisibility(View.VISIBLE);
                    binding.bluebinderProcesses.setText("Running processes: " + processes.replaceAll("\n", ", "));
                } else {
                    binding.bluebinderProcesses.setVisibility(View.GONE);
                    binding.bluebinderProcesses.setText("");
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

    private void runBluebinderIfItIsInstalled() {
        executor.execute(() -> {
            int code = new ShellUtils().executeCommandAsChrootWithReturnCode("which bluebinder");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (code == 1) {
                    bluebinderIsNotInstalledDialog();
                } else {
                    if (isServiceRunning(BluetoothD.class)) {
                        Intent service = new Intent(this, BluetoothD.class);
                        service.setAction(BluetoothD.ACTION_STOP);
                        ContextCompat.startForegroundService(this, service);
                    } else {
                        PathsUtil.showSnackBar(activity, "Please, wait! Running...", false);
                        Intent service = new Intent(this, BluetoothD.class);
                        service.setAction(BluetoothD.ACTION_START);
                        ContextCompat.startForegroundService(this, service);
                    }
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
            int code = new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + binding.interfaces.getText().toString() + " down");
            if (code == 0) {
                code = new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + binding.interfaces.getText().toString() + " name " + newInterfaceName);
                if (code == 0) {
                    new Handler(Looper.getMainLooper()).post(() -> binding.interfaces.setText(newInterfaceName));
                    prefs.edit().putString("macchanger_interface", newInterfaceName).apply();
                    new ShellUtils().executeCommandAsRootWithReturnCode("ip link set " + binding.interfaces.getText().toString() + " up");
                    if (newInterfaceName.matches("(s|)wlan0")) {
                        new ShellUtils().executeCommandAsRootWithReturnCode("svc wifi enable");
                    }
                    new Handler(Looper.getMainLooper()).post(() ->
                            PathsUtil.showSnackBar(activity, "Interface successfully renamed!", false));
                } else {
                    int finalCode = code;
                    new Handler(Looper.getMainLooper()).post(() ->
                            PathsUtil.showSnackBar(activity, "Failed to rename interface. Code: " + finalCode, false));
                }
            }
        });
    }
}
