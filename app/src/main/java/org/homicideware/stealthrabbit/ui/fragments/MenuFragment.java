package org.homicideware.stealthrabbit.ui.fragments;

import android.app.Activity;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;

import org.homicideware.stealthrabbit.BuildConfig;
import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.databinding.MenuFragmentBinding;
import org.homicideware.stealthrabbit.databinding.UsbarmoryActivityBinding;
import org.homicideware.stealthrabbit.ui.activities.MainActivity;
import org.homicideware.stealthrabbit.ui.activities.TerminalRunActivity;
import org.homicideware.stealthrabbit.ui.activities.menu.CustomCommandsActivity;
import org.homicideware.stealthrabbit.ui.activities.menu.MACChangerActivity;
import org.homicideware.stealthrabbit.ui.activities.menu.NetworkingActivity;
import org.homicideware.stealthrabbit.ui.activities.menu.ServicesActivity;
import org.homicideware.stealthrabbit.utils.PathsUtil;
import org.homicideware.stealthrabbit.utils.TerminalUtil;

public class MenuFragment extends Fragment {

    private static final String SHORTCUT_ID = BuildConfig.APPLICATION_ID + ".shortcut";
    private static MenuFragmentBinding binding;
    private Activity activity;
    private Context context;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;

    public static void compatVerified(boolean is) {
        ArrayList<View> views = new ArrayList<>();
        // views.add(view);
        views.add(binding.terminal);
        views.add(binding.customCommands);
        views.add(binding.services);
        views.add(binding.macchanger);
        views.add(binding.networking);
        views.add(binding.nmap);
        views.add(binding.oneshot);
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            view.setEnabled(is);
            view.setVisibility(is ? View.VISIBLE : View.GONE);
        }
        binding.help.setVisibility(is ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        context = getContext();
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = MenuFragmentBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        terminalUtil = new TerminalUtil(activity, context);

        binding.help.setOnClickListener(v -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            adb.setTitle("Menu");
            adb.setMessage(
                    "When the chroot isn't running, some elements that interact with it are hidden.");
            adb.setPositiveButton("Open Manager", (di, i) -> MainActivity.openPage("manager"));
            adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
            });
            adb.show();
        });

        binding.usbarmory.setOnClickListener(v -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            adb.setTitle("USB Armory");
            if (MainActivity.isSelinuxEnforcing()) {
                adb.setMessage(
                        "Selinux is enforcing, StealthRabbit cannot fully verify that your device is compatible with " +
                                "the functionality of this item.");
            } else if (!new File("/config/usb_gadget").exists()) {
                adb.setMessage(
                        "Not supported by the kernel.");
            } else {
                openActivity(UsbarmoryActivityBinding.class);
                return;
            }
            adb.setPositiveButton(android.R.string.ok, (di, i) -> {
            });
            adb.show();
        });

        binding.terminal.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isShortcutPinned()) {
                    runTerminalFromApp();
                } else {
                    MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                    adb.setTitle("StealthRabbit");
                    adb.setMessage("We recommend creating a shortcut on your desktop to quickly launch the Terminal.");
                    adb.setPositiveButton("Create", (di, i) -> createShortcut());
                    adb.setNeutralButton("Open in app", (di, i) -> runTerminalFromApp());
                    adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
                    });
                    adb.show();
                }
            } else {
                runTerminalFromApp();
            }
        });

        binding.customCommands.setOnClickListener(v -> openActivity(CustomCommandsActivity.class));

        binding.services.setOnClickListener(v -> openActivity(ServicesActivity.class));

        binding.macchanger.setOnClickListener(v -> openActivity(MACChangerActivity.class));

        binding.networking.setOnClickListener(v -> openActivity(NetworkingActivity.class));

        binding.nmap.setOnClickListener(v ->
                openActivity(org.homicideware.stealthrabbit.ui.activities.menu.Nmap.Activity.class));

        binding.oneshot.setOnClickListener(v ->
                openActivity(org.homicideware.stealthrabbit.ui.activities.menu.OneShot.Activity.class));

        binding.otgArmory.setOnClickListener(v ->
                openActivity(org.homicideware.stealthrabbit.ui.activities.menu.OTGArmory.Activity.class));
    }

    private void openActivity(Class<?> activity) {
        Intent intent = new Intent(context, activity);
        startActivity(intent);
    }

    private void runTerminalFromApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                runTerminalInChrootThrow();
            } catch (BackgroundServiceStartNotAllowedException e) {
                terminalUtil.termuxServiceIsNotRunning();
            }
        } else {
            runTerminalInChrootThrow();
        }
    }

    private void runTerminalInChrootThrow() {
        try {
            terminalUtil.runCommand(PathsUtil.APP_SCRIPTS_PATH + "/bootroot_login", false);
        } catch (ActivityNotFoundException e) {
            if (prefs.getString("terminal_type", TerminalUtil.TERMINAL_TYPE_TERMUX).equals(TerminalUtil.TERMINAL_TYPE_TERMUX)) {
                terminalUtil.checkIsTermuxApiSupported();
            }
        } catch (PackageManager.NameNotFoundException e) {
            terminalUtil.showTerminalNotInstalledDialog();
        } catch (SecurityException e) {
            terminalUtil.showPermissionDeniedDialog();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isShortcutPinned() {
        ShortcutManager shortcutManager =
                context.getSystemService(ShortcutManager.class);
        for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
            if (SHORTCUT_ID.equals(shortcutInfo.getId()) && shortcutInfo.isPinned()) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createShortcut() {
        ShortcutManager shortcutManager =
                context.getSystemService(ShortcutManager.class);

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Intent intent = new Intent(context, TerminalRunActivity.class);
            intent.setAction(Intent.ACTION_VIEW);

            Icon icon = Icon.createWithResource(context, R.mipmap.ic_sr_termux);

            ShortcutInfo shortcut =
                    new ShortcutInfo.Builder(context, SHORTCUT_ID)
                            .setShortLabel("Chroot Terminal")
                            .setIntent(intent)
                            .setIcon(icon)
                            .build();

            shortcutManager.requestPinShortcut(shortcut, null);
        }
    }
}
