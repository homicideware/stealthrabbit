package material.hunter.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import material.hunter.utils.PathsUtil;
import material.hunter.utils.TerminalUtil;

public class TerminalRunActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PathsUtil.getInstance(this);
            TerminalUtil terminalUtil = new TerminalUtil(this, this);
            terminalUtil.runCommand(PathsUtil.APP_SCRIPTS_PATH + "/bootroot_login", false);
            finish();
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Terminal")
                    .setMessage("Something wrong, try to open terminal in MaterialHunter.")
                    .setPositiveButton("Open", (di, i) -> {
                        finish();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setData(Uri.parse("menu"));
                        startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.cancel, (di, i) -> finish())
                    .setOnCancelListener((di) -> finish())
                    .show();
        }
    }
}