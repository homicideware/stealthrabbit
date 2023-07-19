package material.hunter.ui.activities.menu.LocalScanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.adapters.LocalScannerRecyclerViewAdapter;
import material.hunter.adapters.OneShotRecyclerViewAdapter;
import material.hunter.databinding.LocalScannerActivityBinding;
import material.hunter.ui.activities.ThemedActivity;
import material.hunter.ui.activities.menu.NetworkingActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;

public class Activity extends ThemedActivity {

    private final ArrayList<Item> devices = new ArrayList<>();
    private LocalScannerActivityBinding binding;
    private ExecutorService executor;
    private SharedPreferences prefs;
    private LocalScannerRecyclerViewAdapter adapter;
    private boolean isScanning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LocalScannerActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        File usefulDirectory = new File(PathsUtil.APP_SD_PATH + "/LocalScanner");
        if (!usefulDirectory.exists()) {
            PathsUtil.showSnackBar(this, "Creating directory for scanned networks...", false);
            try {
                usefulDirectory.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnackBar(
                        this,
                        "Failed to create directory: " + usefulDirectory,
                        false);
            }
        }

        loadInterfaces();

        adapter = new LocalScannerRecyclerViewAdapter(this, this, devices);
        binding.scanInterface.setText(prefs.getString("local_scanner_interface", ""));
        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(adapter);

        binding.reScan.setOnClickListener(v -> {

        });

        binding.reScan.performClick();
    }

    private void loadInterfaces() {
        executor.execute(() -> {
            String[] list = new ShellUtils().executeCommandAsChrootWithOutput("iw dev | grep \"Interface\" | sed -r \"s/Interface//g\" | xargs").split(" ");
            ArrayList<String> mInterfaces = new ArrayList<>(Arrays.asList(list));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.mh_spinner_item, mInterfaces);
            new Handler(Looper.getMainLooper()).post(() -> binding.scanInterface.setAdapter(adapter));
            String previousWlan = prefs.getString("local_scanner_interface", "");
            if (previousWlan.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.scanInterface.setText(list[0], false);
                    prefs.edit().putString("local_scanner_interface", list[0]).apply();
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> binding.scanInterface.setText(previousWlan, false));
            }
        });
    }
}
