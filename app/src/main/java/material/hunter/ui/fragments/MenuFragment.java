package material.hunter.ui.fragments;

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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.MenuFragmentBinding;
import material.hunter.ui.activities.MainActivity;
import material.hunter.ui.activities.TerminalRunActivity;
import material.hunter.ui.activities.menu.CustomCommandsActivity;
import material.hunter.ui.activities.menu.MACChangerActivity;
import material.hunter.ui.activities.menu.NetworkingActivity;
import material.hunter.ui.activities.menu.OneShot.Activity;
import material.hunter.ui.activities.menu.ServicesActivity;
import material.hunter.ui.activities.menu.USBArmoryActivity;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.TerminalUtil;

public class MenuFragment extends Fragment {

    private static final String SHORTCUT_ID = BuildConfig.APPLICATION_ID + ".shortcut";
    private static MaterialCardView help;
    private static MaterialCardView usbarmory;
    private static MaterialCardView terminal;
    private static MaterialCardView custom_commands;
    private static MaterialCardView services;
    private static MaterialCardView macchanger;
    private static MaterialCardView netwroking;
    private static MaterialCardView nmap;
    private static MaterialCardView oneshot;
    private MenuFragmentBinding binding;
    private android.app.Activity activity;
    private Context context;
    private SharedPreferences prefs;
    private TerminalUtil terminalUtil;

    public static void compatVerified(boolean is) {
        ArrayList<View> views = new ArrayList<>();
        // views.add(view);
        views.add(terminal);
        views.add(custom_commands);
        views.add(services);
        views.add(macchanger);
        views.add(netwroking);
        views.add(nmap);
        views.add(oneshot);
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            view.setEnabled(is);
            view.setVisibility(is ? View.VISIBLE : View.GONE);
        }
        help.setVisibility(is ? View.GONE : View.VISIBLE);
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
        help = binding.help;
        usbarmory = binding.usbarmory;
        terminal = binding.terminal;
        custom_commands = binding.customCommands;
        services = binding.services;
        macchanger = binding.macchanger;
        netwroking = binding.networking;
        nmap = binding.nmap;
        oneshot = binding.oneshot;
        terminalUtil = new TerminalUtil(activity, context);

        help.setOnClickListener(v -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            adb.setTitle("Menu");
            adb.setMessage(
                    "When the chroot isn't running, some elements that interact with it are hidden.");
            adb.setPositiveButton("Open Manager", (di, i) -> MainActivity.openPage("manager"));
            adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
            });
            adb.show();
        });

        usbarmory.setOnClickListener(v -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            adb.setTitle("USB Armory");
            if (MainActivity.isSelinuxEnforcing()) {
                adb.setMessage(
                        "Selinux is enforcing, MaterialHunter cannot fully verify that your device is compatible with the functionality of this item.");
            } else if (!new File("/config/usb_gadget").exists()) {
                adb.setMessage(
                        "Not supported by the kernel.");
            } else {
                Intent intent = new Intent(context, USBArmoryActivity.class);
                startActivity(intent);
                return;
            }
            adb.setPositiveButton(android.R.string.ok, (di, i) -> {
            });
            adb.show();
        });

        terminal.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isShortcutPinned()) {
                    runTerminalInChroot();
                } else {
                    MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
                    adb.setTitle("MaterialHunter");
                    adb.setMessage("We recommend creating a shortcut on your desktop to quickly launch the Terminal.");
                    adb.setPositiveButton("Create", (di, i) -> createShortcut());
                    adb.setNeutralButton("Open in app", (di, i) -> runTerminalInChroot());
                    adb.setNegativeButton(android.R.string.cancel, (di, i) -> {
                    });
                    adb.show();
                }
            } else {
                runTerminalInChroot();
            }
        });

        custom_commands.setOnClickListener(v -> {
            Intent intent = new Intent(context, CustomCommandsActivity.class);
            startActivity(intent);
        });

        services.setOnClickListener(v -> {
            Intent intent = new Intent(context, ServicesActivity.class);
            startActivity(intent);
        });

        macchanger.setOnClickListener(v -> {
            Intent intent = new Intent(context, MACChangerActivity.class);
            startActivity(intent);
        });

        netwroking.setOnClickListener(v -> {
            Intent intent = new Intent(context, NetworkingActivity.class);
            startActivity(intent);
        });

        nmap.setOnClickListener(v -> {
            Intent intent = new Intent(context, material.hunter.ui.activities.menu.Nmap.Activity.class);
            startActivity(intent);
        });

        oneshot.setOnClickListener(v -> {
            Intent intent = new Intent(context, Activity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void runTerminalInChroot() {
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

            Icon icon = Icon.createWithResource(context, R.mipmap.ic_mh_termux);

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