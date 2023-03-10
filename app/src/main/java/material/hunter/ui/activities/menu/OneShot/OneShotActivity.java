package material.hunter.ui.activities.menu.OneShot;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.adapters.OneShotRecyclerViewAdapter;
import material.hunter.databinding.OneshotActivityBinding;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.ui.activities.menu.NetworkingActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;
import material.hunter.utils.Utils;

public class OneShotActivity extends ThemedActivity {

    private static RecyclerView recyclerView;
    private OneshotActivityBinding binding;
    private View _view;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;
    private LinearProgressIndicator progressIndicator;
    private TextView description;
    private TextInputEditText mInterface;
    private Button networking;
    private Button settings;
    private final ArrayList<OneShotItem> networks = new ArrayList<>();
    private OneShotRecyclerViewAdapter adapter;
    private Timer timer = new Timer();
    private boolean isScanning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = OneshotActivityBinding.inflate(getLayoutInflater());
        _view = binding.getRoot();
        setContentView(_view);

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        MaterialToolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        File usefulDirectory = new File(PathsUtil.APP_SD_PATH + "/OneShot");
        if (!usefulDirectory.exists()) {
            PathsUtil.showSnack(_view, "Creating directory for saved networks and logs...", false);
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

        isRequirementsInstalled();

        progressIndicator = binding.progressIndicator;
        description = binding.description;
        mInterface = binding.mInterface;
        networking = binding.networking;
        settings = binding.settings;
        recyclerView = binding.recyclerView;

        description.setText(Html.fromHtml("<b>OneShot</b> performs <a href=\"https://forums.kali.org/showthread.php?24286-WPS-Pixie-Dust-Attack-Offline-WPS-Attack\">Pixie Dust attack</a> without having to switch to monitor mode.", Html.FROM_HTML_MODE_LEGACY));
        description.setMovementMethod(LinkMovementMethod.getInstance());
        mInterface.setText(prefs.getString("macchanger_interface", ""));
        networking.setOnClickListener(v -> {
            Intent intent = new Intent(this, NetworkingActivity.class);
            startActivity(intent);
        });
        adapter = new OneShotRecyclerViewAdapter(this, networks, _view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        settings.setOnClickListener(v -> {
            String[] items = new String[]{
                    "Down network interface when the work is finished",
                    "Activate MediaTek Wi-Fi interface driver on startup and deactivate it on exit",
                    "Sort networks list by signal",
                    "Sort networks list by enabled WPS",
                    "Compact stdout"
            };
            String[] preferences = new String[]{
                    "oneshot_iface_down",
                    "oneshot_mtk_wifi",
                    "oneshot_sort_by_signal",
                    "oneshot_sort_by_enabled_wps",
                    "oneshot_compact_stdout"
            };
            final boolean[] itemsBooleans = new boolean[]{
                    prefs.getBoolean("oneshot_iface_down", false),
                    prefs.getBoolean("oneshot_mtk_wifi", false),
                    prefs.getBoolean("oneshot_sort_by_signal", true),
                    prefs.getBoolean("oneshot_sort_by_enabled_wps", true),
                    prefs.getBoolean("oneshot_compact_stdout", true)
            };

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Settings")
                    .setMultiChoiceItems(items, itemsBooleans, (di, position, bool) -> itemsBooleans[position] = bool)
                    .setPositiveButton(android.R.string.ok, (di, i) -> {
                        for (int f = 0; f < itemsBooleans.length; f++) {
                            prefs.edit().putBoolean(preferences[f], itemsBooleans[f]).apply();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .show();
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isScanning)
                    runOnUiThread(() -> scan());
            }
        }, 0, 7000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            timer.cancel();
        } catch (IllegalStateException ignored) {
        }
    }

    private void isRequirementsInstalled() {
        executor.execute(() -> {
            String[] requirements = new String[]{
                    "iw",
                    "python3",
                    "oneshot.py"
            };
            StringBuilder requirementsBuilder = new StringBuilder();
            for (String requirement : requirements) {
                int code = new ShellUtils().executeCommandAsChrootWithReturnCode("which " + requirement);
                if (code == 1) {
                    requirementsBuilder.append(requirement).append(", ");
                }
            }
            if (requirementsBuilder.length() > 1) {
                new Handler(Looper.getMainLooper()).post(() ->
                        requirementsIsNotInstalledDialog(requirementsBuilder.substring(0, requirementsBuilder.length() - 2)));
            }
        });
    }

    private void requirementsIsNotInstalledDialog(String requirements) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("OneShot")
                .setMessage(requirements + " required, but it isn't installed in chroot.")
                .setCancelable(false)
                .setPositiveButton("Install", (di, i) -> {
                    try {
                        terminalUtil.runCommand(PathsUtil.APP_SCRIPTS_PATH + "/bootroot_exec 'apt update && apt install python3 wpasupplicant iw wget pixiewps -y; rm -f /usr/sbin/oneshot.py; wget https://raw.githubusercontent.com/drygdryg/OneShot/master/oneshot.py -O /usr/sbin/oneshot.py; chmod +x /usr/sbin/oneshot.py; mkdir -p /etc/OneShot; wget https://raw.githubusercontent.com/drygdryg/OneShot/master/vulnwsc.txt -O /etc/OneShot/vulnwsc.txt'", false);
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

    private ArrayList<String> compactScanResult(String mInterface) {
        String output = new ShellUtils().executeCommandAsChrootWithOutput("iw dev " + mInterface + " scan");
        String[] lines = output.split("\n", 0);
        StringBuilder temp = new StringBuilder();
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (lines.length > i + 1) {
                if (lines[i + 1].startsWith("BSS")) {
                    result.add(temp.toString());
                    temp = new StringBuilder();
                }
                temp.append(lines[i]).append("\n");
            } else {
                break;
            }
        }
        return result;
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void scan() {
        String mInterface = prefs.getString("macchanger_interface", "");
        if (mInterface.isEmpty()) {
            try {
                timer.cancel();
            } catch (IllegalStateException ignored) {
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("OneShot")
                    .setMessage("No interface selected, open Networking and select network interface, then you can return here.")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (di, i) -> {
                    })
                    .show();
        } else {
            isScanning = true;
            progressIndicator.setIndeterminate(true);
            progressIndicator.setVisibility(View.VISIBLE);
            networks.clear();
            executor.execute(() -> {
                for (String line : compactScanResult(mInterface)) {
                    try {
                        line = line.replace("(on " + mInterface + ")", "").replace(" -- associated", "").strip();
                        String ESSID = decodeESSID(Utils.matchString("SSID: (.*)", line, 1));
                        String BSSID = Utils.matchString("BSS ((\\w{2}:){5}\\w{2})", line, 1).toUpperCase();
                        String security = handleSecurity(Utils.matchString("(capability): (.+)", line, 1), Utils.matchString("(capability): (.+)", line, 2));
                        security = handleSecurity(Utils.matchString("(RSN):.*[*] Version: (\\d+)", line, 1), security);
                        security = handleSecurity(Utils.matchString("(WPA):.*[*] Version: (\\d+)", line, 1), security);
                        float power = Float.parseFloat(Utils.matchString("signal: (([+-][0-9]*)\\.?[0-9]+) dBm", line, 1));
                        boolean wpsEnabled = !Utils.matchString("WPS:.*[*] Version: (([0-9]*[.])?[0-9]+)", line, 1).isEmpty();
                        if (wpsEnabled) {
                            boolean isWpsLocked = Utils.matchString("[*] AP setup locked: (0x[0-9]+)", line, 1).equals("0x01");
                            String model = Utils.matchString("[*] Model: (.*)", line, 1);
                            String modelNumber = Utils.matchString("[*] Model Number: (.*)", line, 1);
                            String deviceName = Utils.matchString("[*] Device name: (.*)", line, 1);
                            networks.add(new OneShotItem(ESSID, BSSID, security, power, isWpsLocked, deviceName, model, modelNumber));
                        }
                    } catch (Exception ignored) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                PathsUtil.showSnack(_view, "Failed to load network information...", false));
                    }
                }
                sortNetworks();
                new Handler(Looper.getMainLooper()).post(() -> {
                    //adapter.notifyDataSetChangedL(networks);
                    adapter.notifyDataSetChanged();
                    progressIndicator.setVisibility(View.INVISIBLE);
                    progressIndicator.setIndeterminate(false);
                    isScanning = false;
                });
            });
        }
    }

    private void sortNetworks() {
        if (prefs.getBoolean("oneshot_sort_by_signal", true)) {
            networks.sort((network, network2) -> (int) (network2.getSignal() - network.getSignal()));
        }
        if (prefs.getBoolean("oneshot_sort_by_enabled_wps", true)) {
            networks.sort((network, network2) -> Boolean.compare(!network2.isWpsLocked(), !network.isWpsLocked()));
        }
    }
    
    private String decodeESSID(String ESSID) {
        if (ESSID.contains("\\x")) {
            return new ShellUtils().executeCommandWithOutput("echo -e '" + ESSID + "'");
        } else {
            return ESSID;
        }
    }

    private String handleSecurity(String matches, String security) {
        if (matches.contains("capability")) {
            if (security.contains("Privacy")) {
                return "WEP";
            } else {
                return "Open";
            }
        }
        if (security.equals("WEP")) {
            if (matches.contains("RSN")) {
                return "WPA2";
            }
            if (matches.contains("WPA")) {
                return "WPA";
            }
        }
        if (security.equals("WPA")) {
            if (matches.contains("RSN")) {
                return "WPA/WPA2";
            }
        }
        if (security.equals("WPA2")) {
            if (matches.contains("WPA")) {
                return "WPA/WPA2";
            }
        }
        return security;
    }
}