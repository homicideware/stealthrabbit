package material.hunter.ui.activities.menu.Nmap;

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
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.NmapActivityBinding;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;

public class NmapActivity extends ThemedActivity {

    private final ArrayList<NmapArgument> arguments = new ArrayList<>();
    private NmapActivityBinding binding;
    private View _view;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;
    private LinearProgressIndicator progressIndicator;
    private ScrollView scrollView;
    private ChipGroup argumentsChip;
    private TextInputLayout interfaceLayout;
    private AutoCompleteTextView mInterface;
    private TextInputLayout hostsLayout;
    private TextInputEditText hosts;
    private TextInputLayout portsLayout;
    private TextInputEditText ports;
    private MaterialCardView reportCard;
    private TextView nmapReport;
    private Button clearReport;
    private Button shareReport;
    private Button saveReport;
    private ShellUtils.YetAnotherActiveShellExecutor nmapExecutor;
    private boolean isNmapRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NmapActivityBinding.inflate(getLayoutInflater());
        _view = binding.getRoot();
        setContentView(_view);

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        terminalUtil = new TerminalUtil(this, this);

        MaterialToolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isNmapInstalled();

        File usefulDirectory = new File(PathsUtil.APP_SD_PATH + "/Nmap");
        if (!usefulDirectory.exists()) {
            PathsUtil.showSnack(_view, "Creating directory for reports...", false);
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

        progressIndicator = binding.progressIndicator;
        scrollView = binding.scrollView;
        argumentsChip = binding.argumentsChip;
        interfaceLayout = binding.interfaceLayout;
        mInterface = binding.mInterface;
        hostsLayout = binding.hostsLayout;
        hosts = binding.hosts;
        portsLayout = binding.portsLayout;
        ports = binding.ports;
        reportCard = binding.reportCard;
        nmapReport = binding.report;
        clearReport = binding.clearReport;
        shareReport = binding.shareReport;
        saveReport = binding.saveReport;

        loadArguments();
        loadInterfaces();

        interfaceLayout.setStartIconOnClickListener(v -> loadInterfaces());
        mInterface.setOnItemClickListener((adapterView, v, pos, l) -> prefs.edit().putString("nmap_interface", mInterface.getText().toString()).apply());

        hostsLayout.setEndIconOnClickListener(v -> {
            if (isNmapRunning) {
                if (!destroyNmap())
                    PathsUtil.showSnack(_view, "Something wrong...", false);
            } else {
                scan();
            }
        });

        clearReport.setOnClickListener(v -> {
            nmapReport.setText("");
            reportCard.setVisibility(View.GONE);
        });

        shareReport.setOnClickListener(v -> {
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, getNmapReport());
            startActivity(Intent.createChooser(intent, "Share Nmap report"));
        });

        saveReport.setOnClickListener(v -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcDate = sdf.format(new Date());
            File report = new File(PathsUtil.APP_SD_PATH + "/Nmap/report_" + utcDate);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(report));
                writer.write(getNmapReport());
                writer.close();
            } catch (IOException e) {
                PathsUtil.showSnack(_view, "Error writing: " + report, true);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyNmap();
    }

    private void loadInterfaces() {
        executor.execute(() -> {
            String[] list = new ShellUtils().executeCommandAsRootWithOutput("iw dev | grep \"Interface\" | sed -r 's/Interface//g' | xargs | sed -r 's/ /\n/g'").split("\n");
            ArrayList<String> interfaces = new ArrayList<>(Arrays.asList(list));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.mh_spinner_item, interfaces);
            new Handler(Looper.getMainLooper()).post(() -> mInterface.setAdapter(adapter));
            String previousWlan = prefs.getString("nmap_interface", "");
            if (previousWlan.isEmpty()) {
                String preferredInterface = list[0];
                for (String iInterface : interfaces) {
                    if (iInterface.startsWith("wl")) {
                        preferredInterface = iInterface;
                    }
                }
                String finalPreferredInterface = preferredInterface;
                new Handler(Looper.getMainLooper()).post(() -> {
                    mInterface.setText(finalPreferredInterface, false);
                    prefs.edit().putString("nmap_interface", finalPreferredInterface).apply();
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> mInterface.setText(previousWlan, false));
            }
        });
    }

    private void scan() {
        nmapExecutor = new ShellUtils.YetAnotherActiveShellExecutor(true) {
            @Override
            public void onPrepare() {
                hostsLayout.setEndIconDrawable(R.drawable.ic_stop);
                portsLayout.setEnabled(false);
                clearReport.setEnabled(false);
                shareReport.setEnabled(false);
                saveReport.setEnabled(false);
                reportCard.setVisibility(View.VISIBLE);
                progressIndicator.setIndeterminate(true);
                progressIndicator.setVisibility(View.VISIBLE);
                isNmapRunning = true;
            }

            @Override
            public void onNewLine(String line) {
                nmapReport.append(line + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onNewErrorLine(String line) {
                nmapReport.append(line + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onFinished(int code) {
                nmapReport.append("-----------------------");
                nmapReport.append("\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
                isNmapRunning = false;
                progressIndicator.setVisibility(View.INVISIBLE);
                progressIndicator.setIndeterminate(false);
                portsLayout.setEnabled(true);
                clearReport.setEnabled(true);
                shareReport.setEnabled(true);
                saveReport.setEnabled(true);
                hostsLayout.setEndIconDrawable(R.drawable.ic_play);
            }
        };
        nmapExecutor.exec(assembleCommand());
    }

    private void isNmapInstalled() {
        executor.execute(() -> {
            int code = new ShellUtils().executeCommandAsChrootWithReturnCode("which nmap");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (code == 1) {
                    nmapIsNotInstalledDialog();
                }
            });
        });
    }

    private void nmapIsNotInstalledDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Nmap")
                .setMessage("nmap required, but it isn't installed in chroot.")
                .setPositiveButton("Install", (di, i) -> {
                    try {
                        terminalUtil.runCommand(
                                PathsUtil.APP_SCRIPTS_PATH +
                                        "/bootroot_exec 'apt update && apt install nmap -y'",
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

    private String getNmapReport() {
        String report = nmapReport.getText().toString();
        return report.substring(0, report.length() - 24);
    }

    private void loadArguments() {
        arguments.add(new NmapArgument("Treat as online", "-Pn", false));
        arguments.add(new NmapArgument("OS Detection", "-O", false));
        arguments.add(new NmapArgument("IPv6 scanning", "-6", false));
        arguments.add(new NmapArgument("No port scan", "-sn", false));
        arguments.add(new NmapArgument("service/version info", "-sV", false));
        arguments.add(new NmapArgument("Fast mode", "-F", false));
        arguments.add(new NmapArgument("Don't randomize port scan", "-r", false));
        for (int i = 0; i < arguments.size(); i++) {
            Chip chip = new Chip(this);
            chip.setText(arguments.get(i).getName());
            chip.setCheckedIconVisible(true);
            chip.setCheckable(true);
            int finalI = i;
            chip.setOnCheckedChangeListener((view, bool) -> arguments.set(finalI, new NmapArgument(arguments.get(finalI).getName(), arguments.get(finalI).getArgument(), bool)));
            argumentsChip.addView(chip);
        }
    }

    private String assembleCommand() {
        StringBuilder command = new StringBuilder();
        command.append("nmap");
        command.append(mInterface.getText().toString().isEmpty() ? "" : " -e " + mInterface.getText().toString());
        command.append(ports.getText().toString().isEmpty() ? "" : " -p " + ports.getText().toString()).append(" ");
        for (NmapArgument argument : arguments) {
            if (argument.isEnabled()) {
                command.append(argument.getArgument()).append(" ");
            }
        }
        command.append(binding.hosts.getText().toString());
        return command.toString();
    }

    private boolean destroyNmap() {
        if (nmapExecutor != null) {
            Process process = nmapExecutor.getProcess();
            if (process != null) {
                process.destroy();
                return true;
            }
        }
        return false;
    }
}
