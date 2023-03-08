package material.hunter.ui.activities;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import material.hunter.R;
import material.hunter.adapters.AuthorsRecyclerViewAdapter;
import material.hunter.models.AuthorsModel;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.Utils;

public class AuthorsActivity extends ThemedActivity {

    private final List<AuthorsModel> list = new ArrayList<AuthorsModel>();
    MaterialToolbar toolbar;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authors_activity);

        View included = findViewById(R.id.included);
        toolbar = included.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
                String[] builded = builder.toString().split("\n", 3);
                System.out.println(asset);
                list.add(
                        new AuthorsModel(
                                asset,
                                builded[0],
                                Utils.matchString("GitHub: (http[s]?:\\/\\/(?:[A-z]|[0-9]|[$-_@.]|[!*\\(\\),]|(?:%[0-9A-f][0-9A-f]))+)", builded[1], "", 1),
                                Utils.matchString("Telegram: (http[s]?:\\/\\/(?:[A-z]|[0-9]|[$-_@.]|[!*\\(\\),]|(?:%[0-9A-f][0-9A-f]))+)", builded[1], "", 1),
                                builded[2]));
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
                    PathsUtil.showMessage(this, e.toString(), true);
                }
            }
        }

        AuthorsRecyclerViewAdapter adapter = new AuthorsRecyclerViewAdapter(this, list);

        recycler = findViewById(R.id.recycler_view);
        recycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recycler.setAdapter(adapter);
    }
}