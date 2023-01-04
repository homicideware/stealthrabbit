package material.hunter;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellExecuter;
import material.hunter.utils.YetAnotherActiveShellExecuter;

public class MACChanger extends ThemedActivity {

    private static ActionBar actionBar;
    private final ShellExecuter exe = new ShellExecuter();
    MaterialToolbar toolbar;
    private AutoCompleteTextView iface;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private String CHANGED_MAC = "";
    private TextInputLayout iface_layout;
    private TextInputLayout macaddress_layout;
    private TextInputEditText macaddress;
    private View _view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences("material.hunter", Context.MODE_PRIVATE);

        setContentView(R.layout.macchanger_activity);

        _view = getWindow().getDecorView();
        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        iface_layout = findViewById(R.id.iface_layout);
        iface = findViewById(R.id.iface);
        macaddress_layout = findViewById(R.id.macaddress_layout);
        macaddress = findViewById(R.id.macaddress);

        executor.execute(() -> exe.RunAsRoot("dos2unix " + PathsUtil.APP_SCRIPTS_PATH + "/changemac"));

        loadInterfaces();

        iface.setOnItemClickListener((adapterView, view, pos, l) -> {
            prefs.edit().putString("macchanger_wlan", iface.getText().toString()).apply();
            loadMacAddress();
        });

        iface_layout.setStartIconOnClickListener(v -> loadInterfaces());

        macaddress_layout.setStartIconOnClickListener(v -> macaddress.setText(genRandomMACAddress()));

        macaddress_layout.setEndIconOnClickListener(v -> {
            if (!macaddress.getText().toString().isEmpty() || macaddress.getText().toString().matches("(([0-9A-f]{2}:){5}[0-9A-f]{2})")) {
                if (iface.getText().toString().matches("(s|)wlan(0|1)") && (Build.VERSION.SDK_INT != Build.VERSION_CODES.R)) {
                    new YetAnotherActiveShellExecuter() {
                        @Override
                        public void onPrepare() {
                            iface_layout.setEnabled(false);
                            macaddress_layout.setEnabled(false);
                            CHANGED_MAC = macaddress.getText().toString();
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
                        public void onFinished(int code) {
                            if (code == 0) {
                                if (Build.VERSION.SDK_INT > 30) {
                                    PathsUtil.showSnack(_view, "Need to connect to network.\nWaiting 10 seconds...", true);
                                    executor.execute(() -> {
                                        long end = System.currentTimeMillis() + 10000;
                                        while (System.currentTimeMillis() < end) {
                                            if (isNetworkAvailable()) {
                                                String macOnUp = exe.RunAsRootOutput("ip addr show " + iface.getText() + " | sed -n \"s/.*link\\/ether \\(\\([0-9A-f]\\{2\\}:\\)\\{5\\}[0-9A-f]\\{2\\}\\).*/\\1/p\"");
                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                    if (!CHANGED_MAC.equalsIgnoreCase(macOnUp)) {
                                                        View view = getLayoutInflater().inflate(R.layout.macchanger_a12dialog, null);
                                                        TextView message = view.findViewById(R.id.message);
                                                        message.setText(Html.fromHtml("Failed to change the MAC address on your device. The Android version of your device is greater than 11 (12+), in which case you need to use XPosed (or <a href=\"https://github.com/LSPosed/LSPosed#install\">LSPosed</a>) and the <a href=\"https://github.com/DavidBerdik/MACsposed\">MACsposed</a> module. More details <a href=\"https://github.com/DavidBerdik/MACsposed\">here</a>.", Html.FROM_HTML_MODE_LEGACY));
                                                        message.setMovementMethod(new LinkMovementMethod());
                                                        new MaterialAlertDialogBuilder(MACChanger.this)
                                                                .setTitle("Failed to change MAC address.")
                                                                .setView(view)
                                                                .setPositiveButton(android.R.string.ok, (di, i) -> {
                                                                })
                                                                .setCancelable(true)
                                                                .show();
                                                        loadMacAddress();
                                                    } else {
                                                        PathsUtil.showSnack(_view, "MAC address successful changed.", false);
                                                    }
                                                    iface_layout.setEnabled(true);
                                                    macaddress_layout.setEnabled(true);
                                                    loadMacAddress();
                                                });
                                                return;
                                            }
                                        }
                                        PathsUtil.showSnack(_view, "The network wait time was longer than expected.", false);
                                    });
                                }
                            }
                        }
                    }.exec(PathsUtil.APP_SCRIPTS_PATH
                            + "/changemac \""
                            + iface.getText()
                            + "\" \""
                            + macaddress.getText() + "\"");
                } else {
                    new YetAnotherActiveShellExecuter() {
                        @Override
                        public void onPrepare() {
                            iface_layout.setEnabled(false);
                            macaddress_layout.setEnabled(false);
                        }

                        @Override
                        public void onNewLine(String line) {}

                        @Override
                        public void onFinished(int code) {
                            if (code == 0) {
                                new YetAnotherActiveShellExecuter() {
                                    @Override
                                    public void onPrepare() {}

                                    @Override
                                    public void onNewLine(String line) {}

                                    @Override
                                    public void onFinished(int code) {
                                        if (code == 0) {
                                            PathsUtil.showSnack(_view, "MAC address successful changed.", false);
                                        } else {
                                            PathsUtil.showSnack(_view, "Failed to change MAC address.", false);
                                        }
                                        new YetAnotherActiveShellExecuter() {
                                            @Override
                                            public void onPrepare() {}

                                            @Override
                                            public void onNewLine(String line) {}

                                            @Override
                                            public void onFinished(int code) {
                                                if (code != 0) {
                                                    PathsUtil.showSnack(_view, "Failed to up interface.", false);
                                                }
                                                loadMacAddress();
                                                iface_layout.setEnabled(true);
                                                macaddress_layout.setEnabled(true);
                                            }
                                        }.exec("ip link set " + iface.getText().toString() + " up");
                                    }
                                }.exec(PathsUtil.APP_SCRIPTS_BIN_PATH + "/macchanger " + iface.getText().toString() + " -m " + macaddress.getText().toString());
                            } else {
                                PathsUtil.showSnack(_view, "Failed to demote interface.", false);
                            }
                        }
                    }.exec("ip link set " + iface.getText().toString() + " down");
                }
            } else {
                PathsUtil.showSnack(_view, "MAC address format error.", false);
            }
        });
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
            String[] list = exe.RunAsRootOutput("for i in $(ls /sys/class/net); do if [ -d /sys/class/net/$i/wireless ]; then echo $i; fi; done").split("\n");
            ArrayList<String> interfaces = new ArrayList<>(Arrays.asList(list));
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, R.layout.mh_spinner_item, interfaces);
            String previousWlan = prefs.getString("macchanger_wlan", "");
            new Handler(Looper.getMainLooper()).post(() -> {
                iface.setAdapter(adapter);
                iface.setText(previousWlan.isEmpty() ? list[0] : previousWlan, false);
                loadMacAddress();
            });
        });
    }

    private void loadMacAddress() {
        executor.execute(() -> {
            String mac = exe.RunAsRootOutput("ip addr show " + iface.getText().toString() + " | sed -n \"s/.*link\\/ether \\(\\([0-9A-f]\\{2\\}:\\)\\{5\\}[0-9A-f]\\{2\\}\\).*/\\1/p\"");
            new Handler(Looper.getMainLooper()).post(() -> macaddress.setText(mac));
        });
    }
}