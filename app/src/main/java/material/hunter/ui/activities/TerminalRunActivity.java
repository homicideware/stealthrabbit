package material.hunter.ui.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import material.hunter.utils.PathsUtil;
import material.hunter.utils.TerminalUtil;

public class TerminalRunActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PathsUtil.getInstance(this);
        try {
            TerminalUtil terminal = new TerminalUtil(this, this);
            terminal.runCommand(PathsUtil.APP_SCRIPTS_PATH + "/bootroot_login", false);
        } catch (ActivityNotFoundException
                 | PackageManager.NameNotFoundException
                 | SecurityException e) {
            Toast.makeText(
                            this,
                            "Something wrong, try to open terminal in MaterialHunter.",
                            Toast.LENGTH_LONG)
                    .show();
        }
        finish();
    }
}