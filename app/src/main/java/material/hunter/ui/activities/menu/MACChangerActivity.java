package material.hunter.ui.activities.menu;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.MacchangerActivityBinding;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;
import material.hunter.utils.Utils;

public class MACChangerActivity extends ThemedActivity {

    private MacchangerActivityBinding binding;
    private TextInputLayout interfacesLayout;
    private AutoCompleteTextView interfaces;
    private Button interfacesUpdate;
    private TextInputLayout macaddress_layout;
    private TextInputEditText macaddress;
    private Button macaddressSetup;
    private TextInputLayout permanent_layout;
    private TextInputEditText permanent;
    private Button permanentSetup;
    private Button networking;
    private View _view;
    private final ShellUtils exe = new ShellUtils();
    private ExecutorService executor;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;
    private String CHANGED_MAC = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MacchangerActivityBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);
        _view = root;

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        MaterialToolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        interfacesLayout = binding.interfacesLayout;
        interfaces = binding.interfaces;
        interfacesUpdate = binding.interfacesUpdate;
        macaddress_layout = binding.macaddressLayout;
        macaddress = binding.macaddress;
        macaddressSetup = binding.macaddressSetup;
        permanent_layout = binding.permanentLayout;
        permanent = binding.permanent;
        permanentSetup = binding.permanentSetup;
        networking = binding.networking;

        executor.execute(() -> exe.executeCommandAsRoot("dos2unix " + PathsUtil.APP_SCRIPTS_PATH + "/changemac"));

        isRequirementsInstalled();
        loadInterfaces();

        interfaces.setOnItemClickListener((adapterView, v, pos, l) -> {
            prefs.edit().putString("macchanger_interface", interfaces.getText().toString()).apply();
            loadMACAddress();
            getPermanentMAC();
        });

        interfacesUpdate.setOnClickListener(v -> loadInterfaces());

        macaddress_layout.setStartIconOnClickListener(v -> macaddress.setText(genRandomMACAddress()));

        macaddressSetup.setOnClickListener(v -> setMACAddress(interfaces.getText().toString(), macaddress.getText().toString()));

        permanentSetup.setOnClickListener(v -> setMACAddress(interfaces.getText().toString(), permanent.getText().toString()));

        networking.setOnClickListener(v -> {
            finish();
            Intent intent = new Intent(this, NetworkingActivity.class);
            startActivity(intent);
        });
    }

    private void setMACAddress(String iface, String macAddress) {
        if (!macAddress.isEmpty() || macAddress.matches("((\\w{2}:){5}\\w{2})")) {
            if (iface.matches("^(s|)wlan(0|1)") && (Build.VERSION.SDK_INT != Build.VERSION_CODES.R)) {
                new ShellUtils.YetAnotherActiveShellExecutor() {
                    @Override
                    public void onPrepare() {
                        interfacesLayout.setEnabled(false);
                        macaddress_layout.setEnabled(false);
                        permanent_layout.setEnabled(false);
                        networking.setEnabled(false);
                        CHANGED_MAC = macAddress;
                    }

                    @Override
                    public void onNewLine(String line) {
                        if (line.contains("MAC address format error")) {
                            PathsUtil.showSnack(_view, "MAC address format error.", false);
                        } else if (line.contains("Changing MAC address")) {
                            PathsUtil.showSnack(_view, "Changing MAC address...", false);
                        } else if (line.contains("Wait 5")) {
                            PathsUtil.showSnack(_view, line, false);
                        } else if (line.contains("Failed to change")) {
                            PathsUtil.showSnack(_view, "Failed to change MAC address.", false);
                        } else if (line.contains("successfully changed")) {
                            PathsUtil.showSnack(_view, "MAC address was successfully changed.", false);
                        }
                    }

                    @Override
                    public void onNewErrorLine(String line) {

                    }

                    @Override
                    public void onFinished(int code) {
                        if (code == 0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                PathsUtil.showSnack(_view, "Need to connect to network.\nWaiting 10 seconds...", true);
                                executor.execute(() -> {
                                    long end = System.currentTimeMillis() + 10000;
                                    while (System.currentTimeMillis() < end) {
                                        if (isNetworkAvailable()) {
                                            String macOnUp = exe.executeCommandAsRootWithOutput("ip addr show " + iface + " | sed -n \"s/.*link\\/ether \\(\\(\\w\\{2\\}:\\)\\{5\\}\\w\\{2\\}\\).*/\\1/p\"");
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                if (!CHANGED_MAC.equalsIgnoreCase(macOnUp)) {
                                                    View view = getLayoutInflater().inflate(R.layout.macchanger_a12dialog, null);
                                                    TextView message = view.findViewById(R.id.message);
                                                    message.setText(Html.fromHtml("Failed to change the MAC address on your device. The Android version of your device is greater than 11 (12+), in which case you need to use XPosed (or <a href=\"https://github.com/LSPosed/LSPosed#install\">LSPosed</a>) and the <a href=\"https://github.com/DavidBerdik/MACsposed\">MACsposed</a> module. More details <a href=\"https://github.com/DavidBerdik/MACsposed\">here</a>.", Html.FROM_HTML_MODE_LEGACY));
                                                    message.setMovementMethod(new LinkMovementMethod());
                                                    failedToChangeMACDialog(view);
                                                } else {
                                                    PathsUtil.showSnack(_view, "MAC address successful changed.", false);
                                                }
                                                loadMACAddress();
                                                interfacesLayout.setEnabled(true);
                                                macaddress_layout.setEnabled(true);
                                                permanent_layout.setEnabled(true);
                                                networking.setEnabled(true);
                                            });
                                            return;
                                        }
                                    }
                                    PathsUtil.showSnack(_view, "The network wait time was longer than expected.", false);
                                });
                            }
                        } else {
                            loadMACAddress();
                            interfacesLayout.setEnabled(true);
                            macaddress_layout.setEnabled(true);
                            permanent_layout.setEnabled(true);
                            networking.setEnabled(true);
                            PathsUtil.showSnack(_view, "Something wrong...", false);
                        }
                    }
                }.exec(PathsUtil.APP_SCRIPTS_PATH
                        + "/changemac \""
                        + iface
                        + "\" \""
                        + macAddress + "\"");
            } else {
                interfacesLayout.setEnabled(false);
                macaddress_layout.setEnabled(false);
                permanent_layout.setEnabled(false);
                networking.setEnabled(false);
                executor.execute(() -> {
                    int downNetworkInterface = exe.executeCommandAsRootWithReturnCode("ip link set " + iface + " down");
                    if (downNetworkInterface == 0) {
                        int changeMacAddress = exe.executeCommandAsRootWithReturnCode("macchanger " + iface + " -m " + macAddress);
                        if (changeMacAddress == 0) {
                            int upNetworkInterface = exe.executeCommandAsRootWithReturnCode("ip link set " + iface + " up");
                            if (upNetworkInterface == 0) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        PathsUtil.showSnack(_view, "MAC address successful changed.", false));
                            } else {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        PathsUtil.showSnack(_view, "Failed to up network interface.", false));
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    PathsUtil.showSnack(_view, "Failed to change MAC address.", false));
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                                PathsUtil.showSnack(_view, "Failed to down network interface.", false));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        loadMACAddress();
                        interfacesLayout.setEnabled(true);
                        macaddress_layout.setEnabled(true);
                        permanent_layout.setEnabled(true);
                        networking.setEnabled(true);
                    });
                });
            }
        } else {
            PathsUtil.showSnack(_view, "MAC address format error.", false);
        }
    }

    private void failedToChangeMACDialog(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Failed to change MAC address.")
                .setView(view)
                .setPositiveButton(android.R.string.ok, (di, i) -> {
                })
                .setCancelable(true)
                .show();
    }

    private String genRandomMACAddress() {
        SecureRandom random = new SecureRandom();
        byte[] macBytes = new byte[6];
        random.nextBytes(macBytes);
        String macAddress = "";
        macAddress += String.format("%02x", ((macBytes[0] & 0xfc) | 0x2));
        macAddress += ":" + String.format("%02x", macBytes[1]);
        macAddress += ":" + String.format("%02x", macBytes[2]);
        macAddress += ":" + String.format("%02x", macBytes[3]);
        macAddress += ":" + String.format("%02x", macBytes[4]);
        macAddress += ":" + String.format("%02x", macBytes[5]);
        return macAddress;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null
                && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
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
            new Handler(Looper.getMainLooper()).post(() -> {
                loadMACAddress();
                getPermanentMAC();
            });
        });
    }

    private void loadMACAddress() {
        executor.execute(() -> {
            String mac = exe.executeCommandAsRootWithOutput("ip addr show " + interfaces.getText().toString() + " | sed -n \"s/.*link\\/ether \\(\\([0-9A-f]\\{2\\}:\\)\\{5\\}[0-9A-f]\\{2\\}\\).*/\\1/p\"");
            new Handler(Looper.getMainLooper()).post(() -> macaddress.setText(mac));
        });
    }

    private void getPermanentMAC() {
        new ShellUtils.YetAnotherActiveShellExecutor(true) {

            @Override
            public void onPrepare() {

            }

            @Override
            public void onNewLine(String line) {
                permanent.setText(Utils.matchString("Permanent MAC: (.*) \\(", line, "00:00:00:00:00:00", 1));
            }

            @Override
            public void onNewErrorLine(String line) {

            }

            @Override
            public void onFinished(int code) {
                if (code != 0) {
                    PathsUtil.showSnack(_view, "Failed to get permanent MAC address!", false);
                }
            }
        }.exec("macchanger -s " + interfaces.getText().toString());
    }

    private void isRequirementsInstalled() {
        executor.execute(() -> {
            String[] requirements = new String[]{
                    "iw",
                    "macchanger"
            };
            StringBuilder requirementsBuilder = new StringBuilder();
            for (String requirement : requirements) {
                int code = new ShellUtils().executeCommandAsChrootWithReturnCode("which " + requirement);
                if (code == 1) {
                    requirementsBuilder.append(requirement).append(", ");
                }
            }
            if (requirementsBuilder.length() > 1) {
                requirementsBuilder = new StringBuilder(requirementsBuilder.substring(0, requirementsBuilder.length() - 2));
                requirementsIsNotInstalledDialog(requirementsBuilder.toString());
            }
        });
    }

    private void requirementsIsNotInstalledDialog(String requirements) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("MAC Changer")
                .setMessage(requirements + " required, but it isn't installed in chroot.")
                .setCancelable(false)
                .setPositiveButton("Install", (di, i) -> {
                    try {
                        terminalUtil.runCommand(PathsUtil.APP_SCRIPTS_PATH + "/bootroot_exec 'apt update && apt install iw macchanger'", false);
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
}