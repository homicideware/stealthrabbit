package material.hunter.ui.activities;

import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import material.hunter.adapters.AuthorsRecyclerViewAdapter;
import material.hunter.databinding.AuthorsActivityBinding;
import material.hunter.models.AuthorsModel;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.Utils;

public class AuthorsActivity extends ThemedActivity {
    
    private final List<AuthorsModel> list = new ArrayList<>();
    private AuthorsActivityBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AuthorsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AssetManager assetManager = getAssets();
        String authorsDir = "authors";
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            String[] assets = assetManager.list(authorsDir);
            if (assets == null || assets.length == 0) return;

            for (String asset : assets) {
                reader = new BufferedReader(new InputStreamReader(assetManager.open(authorsDir + "/" + asset), StandardCharsets.UTF_8));

                int value;
                while ((value = reader.read()) != -1) {
                    builder.append((char) value);
                }
                String[] built = builder.toString().split("\n", 3);
                System.out.println(asset);
                list.add(
                        new AuthorsModel(
                                asset,
                                built[0],
                                Utils.matchString("GitHub: (http[s]?:\\/\\/(?:[A-z]|[0-9]|[$-_@.]|[!*\\(\\),]|(?:%[0-9A-f][0-9A-f]))+)", built[1], "", 1),
                                Utils.matchString("Telegram: (http[s]?:\\/\\/(?:[A-z]|[0-9]|[$-_@.]|[!*\\(\\),]|(?:%[0-9A-f][0-9A-f]))+)", built[1], "", 1),
                                built[2]));
                builder = new StringBuilder();
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (IOException ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    PathsUtil.showToast(this, e.toString(), true);
                }
            }
        }

        AuthorsRecyclerViewAdapter adapter = new AuthorsRecyclerViewAdapter(this, list);

        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(adapter);
    }
}