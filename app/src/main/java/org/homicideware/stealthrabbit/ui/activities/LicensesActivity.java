package org.homicideware.stealthrabbit.ui.activities;

import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.adapters.LicensesRecyclerViewAdapter;
import org.homicideware.stealthrabbit.databinding.LicensesActivityBinding;
import org.homicideware.stealthrabbit.models.LicenseModel;

public class LicensesActivity extends ThemedActivity {

    private final List<LicenseModel> list = new ArrayList<>();
    private LicensesActivityBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LicensesActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AssetManager assetManager = getAssets();
        String licensesDir = "licenses";
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            String[] dirs = assetManager.list(licensesDir);
            if (dirs == null || dirs.length == 0) return;
            for (String dir : dirs) {

                String[] assets = assetManager.list(licensesDir + "/" + dir);

                for (String asset : assets) {
                    reader = new BufferedReader(new InputStreamReader(assetManager.open(licensesDir + "/" + dir + "/" + asset), StandardCharsets.UTF_8));

                    int value;
                    while ((value = reader.read()) != -1) {
                        builder.append((char) value);
                    }
                    String builded = builder.toString();
                    list.add(new LicenseModel(asset, builded.substring(0, builded.length() - 1)));
                    builder = new StringBuilder();
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        LicensesRecyclerViewAdapter adapter = new LicensesRecyclerViewAdapter(this, list);
        
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(adapter);
    }
}
