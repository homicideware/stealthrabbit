package material.hunter.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.AboutActivityBinding;

public class AboutActivity extends ThemedActivity {

    private AboutActivityBinding binding;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AboutActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.appName.setText(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        binding.author.setText("by " + BuildConfig.AUTHOR);

        binding.openAuthors.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthorsActivity.class);
            startActivity(intent);
        });

        binding.openLicenses.setOnClickListener(v -> {
            Intent intent = new Intent(this, LicensesActivity.class);
            startActivity(intent);
        });

        binding.openGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mirivan/material_hunter"));
            startActivity(intent);
        });
    }
}