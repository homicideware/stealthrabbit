package material.hunter.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.ManagerDialogEditBinding;
import material.hunter.databinding.ManagerDialogSettingsChangeSystemPathBinding;
import material.hunter.databinding.ManagerDialogSettingsSecurityBinding;
import material.hunter.databinding.ManagerFragmentBinding;
import material.hunter.ui.activities.MainActivity;
import material.hunter.utils.DownloadChroot;
import material.hunter.utils.MHRepo;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.contract.JSON;

public class ManagerFragment extends Fragment {

    private ManagerFragmentBinding binding;
    private static final int IS_MOUNTED = 0;
    private static final int IS_UNMOUNTED = 1;
    private static final int NEED_TO_INSTALL = 2;
    private static final int CHROOT_CORRUPTED = 3;
    private Activity activity;
    private Context context;
    private SharedPreferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        activity = getActivity();
        context = getContext();
        binding = ManagerFragmentBinding.inflate(getLayoutInflater());
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setStopButton();
        setStartButton();
        setInstallButton();
        setRemoveButton();
        setBackupButton();
        showBanner();
        compatCheck();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.manager, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_logger:
                binding.report.setText("[?] Log cleared.\n");
                break;
            case R.id.update_chroot_status:
                compatCheck();
                break;
            case R.id.settings:
                String[] items = new String[]{
                        "Print timestamp every shell line output"
                };
                String[] preferences = new String[]{
                        "print_timestamp"
                };
                final boolean[] itemsBooleans = new boolean[]{
                        prefs.getBoolean("print_timestamp", false)
                };
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Settings")
                        .setMultiChoiceItems(items, itemsBooleans, (di, position, bool) -> itemsBooleans[position] = bool)
                        .setPositiveButton(android.R.string.ok, (di, i) -> {
                            for (int f = 0; f < itemsBooleans.length; f++) {
                                prefs.edit().putBoolean(preferences[f], itemsBooleans[f]).apply();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (di, i) -> {
                        })
                        .show();
                break;
            case R.id.edit_chroot_folder:
                MaterialAlertDialogBuilder edit = new MaterialAlertDialogBuilder(context);
                edit.setTitle("Edit chroot folder");
                View view_edit =
                        getLayoutInflater().inflate(R.layout.manager_dialog_edit, null);
                AutoCompleteTextView path =
                        view_edit.findViewById(R.id.input);
                String path_now = prefs.getString("chroot_directory", "chroot");
                path.setText(path_now);
                ArrayList<String> chroots = new ArrayList<>();
                for (String file : new ShellUtils().executeCommandAsRootWithOutput("for i in $(ls " + PathsUtil.SYSTEM_PATH() + "); do test -d " + PathsUtil.SYSTEM_PATH() + "/$i && echo $i; done").split("\n")) {
                    if (!file.isEmpty()) chroots.add(file);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.mh_spinner_item, chroots);
                path.setAdapter(adapter);
                edit.setView(view_edit);
                edit.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                });
                edit.setNegativeButton(android.R.string.cancel, (dialogInterface2, i2) -> {
                });
                final AlertDialog editAd = edit.create();
                editAd.setOnShowListener(dialog -> {
                    final Button apply = editAd.getButton(DialogInterface.BUTTON_POSITIVE);
                    apply.setOnClickListener(v -> {
                        if (path.getText().toString().matches("^[A-z0-9.\\-_~]+$")) {
                            prefs
                                    .edit()
                                    .putString("chroot_directory", path.getText().toString())
                                    .apply();
                            editAd.dismiss();
                            compatCheck();
                        } else
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Invalid chroot directory name.", false);
                    });
                });
                editAd.show();
                break;
            case R.id.change_system_path:
                MaterialAlertDialogBuilder change_system_path = new MaterialAlertDialogBuilder(context);
                change_system_path.setTitle("Change system path");
                View view_change_system_path = getLayoutInflater().inflate(R.layout.manager_dialog_settings_change_system_path, null);
                TextInputEditText input1 = view_change_system_path.findViewById(R.id.new_system_path);

                input1.setText(prefs.getString("chroot_system_path", "/data/local/nhsystem"));

                change_system_path.setView(view_change_system_path);
                change_system_path.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                });
                change_system_path.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                });
                final AlertDialog ad1 = change_system_path.create();
                ad1.setOnShowListener(dialog -> {
                    final Button apply = ad1.getButton(DialogInterface.BUTTON_POSITIVE);
                    apply.setOnClickListener(v -> {
                        String inputText = input1.getText().toString();
                        if (!inputText.matches("^[A-z0-9.\\-_~]+$")) {
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Invalid system path.", false);
                        } else {
                            prefs.edit().putString("chroot_system_path", inputText).apply();
                            ad1.dismiss();
                            compatCheck();
                        }
                    });
                });
                ad1.show();
                break;
            case R.id.security_settings:
                securitySettings();
                break;
            case R.id.rename_chroot_folder:
                MaterialAlertDialogBuilder rename_chroot_folder = new MaterialAlertDialogBuilder(context);
                rename_chroot_folder.setTitle("Rename chroot folder");
                View view_rename_chroot_folder = getLayoutInflater().inflate(R.layout.manager_dialog_settings_rename_chroot_folder, null);
                TextInputEditText input2 = view_rename_chroot_folder.findViewById(R.id.new_chroot_folder_name);

                input2.setText(prefs.getString("chroot_directory", "chroot"));

                rename_chroot_folder.setView(view_rename_chroot_folder);
                rename_chroot_folder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                });
                rename_chroot_folder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                });
                final AlertDialog ad3 = rename_chroot_folder.create();
                ad3.setOnShowListener(dialog -> {
                    final Button apply = ad3.getButton(DialogInterface.BUTTON_POSITIVE);
                    apply.setOnClickListener(v -> {
                        String inputText = input2.getText().toString();
                        if (!inputText.matches("^[A-z0-9.\\-_~]+$"))
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Invalid folder name.", false);
                        else {
                            new ShellUtils().executeCommandAsRoot("mv " + PathsUtil.CHROOT_PATH() + " " + PathsUtil.SYSTEM_PATH() + "/" + inputText);
                            prefs.edit().putString("chroot_directory", inputText).apply();
                            ad3.dismiss();
                            compatCheck();
                        }
                    });
                });
                ad3.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void securitySettings() {
        ManagerDialogSettingsSecurityBinding binding1 = ManagerDialogSettingsSecurityBinding.inflate(getLayoutInflater());
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Security settings");

        binding1.mountSdcard.setOnClickListener(v -> binding1.mountSdcardSwitch.toggle());
        binding1.mountSystem.setOnClickListener(v -> binding1.mountSystemSwitch.toggle());
        binding1.mountData.setOnClickListener(v -> binding1.mountDataSwitch.toggle());
        binding1.mountModules.setOnClickListener(v -> binding1.mountModulesSwitch.toggle());

        binding1.mountSdcardSwitch.setChecked(prefs.getBoolean("mount_sdcard", false));
        binding1.mountSystemSwitch.setChecked(prefs.getBoolean("mount_system", false));
        binding1.mountDataSwitch.setChecked(prefs.getBoolean("mount_data", false));
        binding1.mountModulesSwitch.setChecked(prefs.getBoolean("mount_modules", false));
        binding1.hostname.setText(prefs.getString("hostname", "mh"));

        adb.setView(binding1.getRoot());
        adb.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
        });
        adb.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
        });
        AlertDialog ad = adb.create();
        ad.setOnShowListener(dialog -> {
            Button apply = ad.getButton(DialogInterface.BUTTON_POSITIVE);
            apply.setOnClickListener(v -> {
                String _hostname = binding1.hostname.getText().toString();
                if (!_hostname.matches("([a-zA-Z0-9-]){2,253}")) {
                    PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Invalid hostname.", false);
                } else {
                    prefs.edit().putBoolean("mount_sdcard", binding1.mountSdcardSwitch.isChecked()).apply();
                    prefs.edit().putBoolean("mount_system", binding1.mountSystemSwitch.isChecked()).apply();
                    prefs.edit().putBoolean("mount_data", binding1.mountDataSwitch.isChecked()).apply();
                    prefs.edit().putBoolean("mount_modules", binding1.mountModulesSwitch.isChecked()).apply();
                    prefs.edit().putString("hostname", _hostname).apply();
                    ad.dismiss();
                    compatCheck();
                }
            });
        });
        ad.show();
    }

    private void setStartButton() {
        binding.start.setOnClickListener(view -> new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
            @Override
            public void onPrepare() {
                setAllButtonEnable(false);
            }

            @Override
            public void onNewLine(String line) {
                MainActivity.notifyManagerBadge();
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onFinished(int code) {
                if (code == 0) {
                    setButtonVisibility(IS_MOUNTED);
                    setMountStatsTextView(IS_MOUNTED);
                    setAllButtonEnable(true);
                    compatCheck();
                }
            }
        }.exec(PathsUtil.APP_SCRIPTS_PATH + "/bootroot", binding.report));
    }

    private void setStopButton() {
        binding.stop.setOnClickListener(view -> new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
            @Override
            public void onPrepare() {
                setAllButtonEnable(false);
            }

            @Override
            public void onNewLine(String line) {
                MainActivity.notifyManagerBadge();
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onFinished(int code) {
                if (code == 0) {
                    setButtonVisibility(IS_MOUNTED);
                    setMountStatsTextView(IS_MOUNTED);
                    setAllButtonEnable(true);
                    compatCheck();
                }
            }
        }.exec(PathsUtil.APP_SCRIPTS_PATH + "/killroot", binding.report));
    }

    private void setInstallButton() {
        binding.install.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            if (MainActivity.isBusyboxInstalled()) {
                View rootView = getLayoutInflater().inflate(R.layout.manager_dialog_install, null);
                Button db = rootView.findViewById(R.id.downloadButton);
                Button r = rootView.findViewById(R.id.repositoryButton);
                Button rb = rootView.findViewById(R.id.restoreButton);
                adb.setView(rootView);
                AlertDialog ad = adb.create();
                db.setOnClickListener(view1 -> {
                    ad.dismiss();
                    MaterialAlertDialogBuilder adb1 = new MaterialAlertDialogBuilder(context);
                    View promtDownloadView = getLayoutInflater().inflate(R.layout.manager_dialog_download, null);
                    final TextInputEditText input = promtDownloadView.findViewById(R.id.input);
                    input.setText(prefs.getString("chroot_download_url_prev", ""));
                    adb1.setView(promtDownloadView);
                    adb1.setPositiveButton("Setup", (dialogInterface, i) -> {
                    });
                    adb1.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    });
                    final AlertDialog adb1Ad = adb1.create();
                    adb1Ad.setOnShowListener(dialog -> {
                        final Button setup = adb1Ad.getButton(DialogInterface.BUTTON_POSITIVE);
                        setup.setOnClickListener(v -> {
                            String chroot_url = input.getText().toString();
                            if (!chroot_url.equals("")) {
                                if (!chroot_url.matches("^(http|https):\\/\\/.*$")) {
                                    chroot_url = "http://" + chroot_url;
                                }
                                if (!chroot_url.matches(".*\\.(tar\\.xz|tar\\.gz)$")) {
                                    PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Tarball must be xz or gz compression.", true);
                                    return;
                                }
                            } else {
                                PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "URL can't be empty!", false);
                                return;
                            }
                            prefs.edit().putString("chroot_download_url_prev", chroot_url).apply();
                            String filename = chroot_url.substring(chroot_url.lastIndexOf('/') + 1);
                            File chroot = new File(PathsUtil.APP_PATH + "/" + filename);

                            new DownloadChroot(prefs.getBoolean("print_timestamp", false)) {

                                @Override
                                public void onPrepare() {
                                    adb1Ad.dismiss();
                                    disableToolbarMenu(true);
                                    setAllButtonEnable(false);
                                    binding.progressbar.setIndeterminate(false);
                                    binding.progressbar.show();
                                    binding.progressbar.setProgress(0);
                                    binding.progressbar.setMax(100);
                                }

                                @Override
                                public void onNewLine(String line) {
                                    MainActivity.notifyManagerBadge();
                                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                }

                                @Override
                                public void onProgressUpdate(int progress) {
                                    binding.progressbar.setProgress(progress);
                                }

                                @Override
                                public void onFinished(int resultCode) {
                                    setAllButtonEnable(true);
                                    if (resultCode == 0) {
                                        new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {

                                            @Override
                                            public void onPrepare() {
                                                setAllButtonEnable(false);
                                                binding.progressbar.setIndeterminate(true);
                                            }

                                            @Override
                                            public void onNewLine(String line) {
                                                MainActivity.notifyManagerBadge();
                                                binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                            }

                                            @Override
                                            public void onFinished(int code) {
                                                disableToolbarMenu(false);
                                                setAllButtonEnable(true);
                                                compatCheck();
                                                binding.progressbar.hide();
                                                binding.progressbar.setIndeterminate(false);
                                                chroot.delete();
                                            }
                                        }.exec(
                                                PathsUtil.APP_SCRIPTS_PATH
                                                        + "/chrootmgr -c \"restore "
                                                        + chroot
                                                        + " "
                                                        + PathsUtil.CHROOT_PATH()
                                                        + "\"",
                                                binding.report);
                                    } else {
                                        binding.progressbar.hide();
                                    }
                                }
                            }.exec(chroot_url, chroot, binding.report);
                        });
                    });
                    adb1Ad.show();
                });
                r.setOnClickListener(view1 -> {
                    ad.dismiss();
                    MaterialAlertDialogBuilder adb2 = new MaterialAlertDialogBuilder(context);
                    View repov = getLayoutInflater().inflate(R.layout.manager_dialog_repository, null);
                    TextView instruction = repov.findViewById(R.id.repository_instruction);
                    TextInputLayout layout = repov.findViewById(R.id.repository_input_layout);
                    final TextInputEditText input = repov.findViewById(R.id.repository_input);
                    final LinearLayout selector_layout = repov.findViewById(R.id.selector_layout);
                    final AutoCompleteTextView selector = repov.findViewById(R.id.chroot_selector);
                    int[] position = {0};
                    instruction.setText(
                            Html.fromHtml("Create your own repository:\n"
                                            + "<a href='https://github.com/Mirivan/dev-root-project/blob/main/REPOSITORY.md'>according to this instruction</a>",
                                    Html.FROM_HTML_MODE_LEGACY));
                    instruction.setMovementMethod(LinkMovementMethod.getInstance());
                    input.setText(prefs.getString("chroot_prev_repository", context.getResources().getString(R.string.mh_repository)));

                    ExecutorService executor = Executors.newSingleThreadExecutor();

                    layout.setEndIconOnClickListener(v -> executor.execute(() -> {
                        try {
                            String repositoryUrl = input.getText().toString();
                            if (repositoryUrl.equals(""))
                                throw new NullPointerException();
                            if (!repositoryUrl.matches("^(http|https):\\/\\/.*$"))
                                repositoryUrl = "http://" + repositoryUrl;

                            JSONObject repo = new JSON().getFromWeb(repositoryUrl);
                            if (!MHRepo.setRepo(repo))
                                throw new JSONException("Bad MaterialHunter repository.");
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.mh_spinner_item, MHRepo.getMainKeys());
                            new Handler(Looper.getMainLooper()).post(() -> {
                                selector.setText(adapter.getItem(0));
                                selector.setAdapter(adapter);
                                selector_layout.setVisibility(View.VISIBLE);
                            });
                        } catch (IOException e) {
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "No internet connection, please try again later.", true);
                        } catch (JSONException e) {
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Bad repository, contact it's author.", true);
                        } catch (NullPointerException e) {
                            PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Repository url must not be empty.", true);
                        }
                    }));

                    selector.setOnItemClickListener((parent, v, p, l) -> position[0] = p);

                    adb2.setView(repov);
                    adb2.setPositiveButton("Download", (dialogInterface, i) -> {
                    });
                    adb2.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    });
                    final AlertDialog adb2Ad = adb2.create();
                    adb2Ad.setOnShowListener(dialog -> {
                        final Button download = adb2Ad.getButton(DialogInterface.BUTTON_POSITIVE);
                        download.setOnClickListener(v -> {
                            String chroot_url = "";
                            String[] chroot_author = {""};

                            try {
                                String chroot_string = MHRepo.getKeyData(Integer.toString(position[0]));
                                JSONObject chroot_json = new JSONObject(chroot_string);

                                if (chroot_json.has("url"))
                                    chroot_url = chroot_json.getString("url");
                                else if (chroot_json.has("file")) {
                                    String inputText = input.getText().toString();
                                    chroot_url = inputText.substring(inputText.lastIndexOf('/') + 1) + "/" + chroot_json.getString("file");
                                } else throw new NullPointerException();
                                chroot_author[0] = chroot_json.getString("author");

                                if (chroot_url.equals(""))
                                    throw new NullPointerException();

                                if (!chroot_url.matches("^(http|https):\\/\\/.*$"))
                                    chroot_url = "http://" + chroot_url;

                                if (!chroot_url.matches(".*\\.(tar\\.xz|tar\\.gz)$")) {
                                    PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Bad chroot type, contact repository author.", true);
                                    return;
                                }

                            } catch (JSONException e) {
                                PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Bad repository skeleton.", true);
                                return;
                            } catch (NullPointerException e) {
                                PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Chroot url must not be empty.", true);
                                return;
                            }

                            prefs.edit().putString("chroot_prev_repository", input.getText().toString()).apply();
                            String filename = chroot_url.substring(chroot_url.lastIndexOf('/') + 1);
                            File chroot = new File(PathsUtil.APP_PATH + "/" + filename);

                            new DownloadChroot(prefs.getBoolean("print_timestamp", false)) {

                                @Override
                                public void onPrepare() {
                                    adb2Ad.dismiss();
                                    disableToolbarMenu(true);
                                    setAllButtonEnable(false);
                                    PathsUtil.showSnackBar(activity, MainActivity.getBnCard(), "Downloading chroot by: " + chroot_author[0], false);
                                    binding.progressbar.setIndeterminate(false);
                                    binding.progressbar.show();
                                    binding.progressbar.setProgress(0);
                                    binding.progressbar.setMax(100);
                                }

                                @Override
                                public void onNewLine(String line) {
                                    MainActivity.notifyManagerBadge();
                                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                }

                                @Override
                                public void onProgressUpdate(int progress) {
                                    binding.progressbar.setProgress(progress);
                                }

                                @Override
                                public void onFinished(int resultCode) {
                                    setAllButtonEnable(true);
                                    if (resultCode == 0) {
                                        new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {

                                            @Override
                                            public void onPrepare() {
                                                setAllButtonEnable(false);
                                                binding.progressbar.setIndeterminate(true);
                                            }

                                            @Override
                                            public void onNewLine(String line) {
                                                MainActivity.notifyManagerBadge();
                                                binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                            }

                                            @Override
                                            public void onFinished(int code) {
                                                disableToolbarMenu(false);
                                                setAllButtonEnable(true);
                                                compatCheck();
                                                binding.progressbar.hide();
                                                binding.progressbar.setIndeterminate(false);
                                                chroot.delete();
                                            }
                                        }.exec(
                                                PathsUtil.APP_SCRIPTS_PATH
                                                        + "/chrootmgr -c \"restore "
                                                        + chroot
                                                        + " "
                                                        + PathsUtil.CHROOT_PATH()
                                                        + "\"",
                                                binding.report);
                                    } else {
                                        binding.progressbar.hide();
                                    }
                                }
                            }.exec(chroot_url, chroot, binding.report);
                        });
                    });
                    adb2Ad.show();
                });
                rb.setOnClickListener(view1 -> {
                    ad.dismiss();
                    MaterialAlertDialogBuilder adb3 = new MaterialAlertDialogBuilder(context);
                    View rootViewR = getLayoutInflater().inflate(R.layout.manager_dialog_restore, null);
                    final TextInputEditText et = rootViewR.findViewById(R.id.input);
                    et.setText(prefs.getString("chroot_restore_path", ""));
                    adb3.setView(rootViewR);
                    adb3.setPositiveButton("Restore", (dialogInterface, i) -> {
                    });
                    adb3.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    });
                    final AlertDialog adb3Ad = adb3.create();
                    adb3Ad.setOnShowListener(dialog -> {
                        final Button restore = adb3Ad.getButton(DialogInterface.BUTTON_POSITIVE);
                        restore.setOnClickListener(v -> {
                            prefs
                                    .edit()
                                    .putString("chroot_restore_path", et.getText().toString())
                                    .apply();

                            new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {

                                @Override
                                public void onPrepare() {
                                    adb3Ad.dismiss();
                                    disableToolbarMenu(true);
                                    setAllButtonEnable(false);
                                    binding.progressbar.show();
                                    binding.progressbar.setIndeterminate(true);
                                }

                                @Override
                                public void onNewLine(String line) {
                                    MainActivity.notifyManagerBadge();
                                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                }

                                @Override
                                public void onFinished(int code) {
                                    disableToolbarMenu(false);
                                    setAllButtonEnable(true);
                                    compatCheck();
                                    binding.progressbar.hide();
                                    binding.progressbar.setIndeterminate(false);
                                }
                            }.exec(
                                    PathsUtil.APP_SCRIPTS_PATH
                                            + "/chrootmgr -c \"restore "
                                            + et.getText().toString()
                                            + " "
                                            + PathsUtil.CHROOT_PATH()
                                            + "\"",
                                    binding.report);
                        });
                    });
                    adb3Ad.show();
                });
                ad.show();
            } else {
                @SuppressLint("InflateParams") View dialogView = getLayoutInflater().inflate(R.layout.manager_dialog_install_busybox, null);
                TextView message = dialogView.findViewById(R.id.message);
                message.setText(Html.fromHtml("Busybox isn't installed, chroot installation cannot be done, please install <a href=\"https://github.com/zgfg/BuiltIn-BusyBox\">busybox</a>. You can use <a href=\"https://github.com/Fox2Code/FoxMagiskModuleManager\">Fox's MMM</a> if you wish.", Html.FROM_HTML_MODE_LEGACY));
                message.setMovementMethod(new LinkMovementMethod());
                adb.setView(dialogView);
                adb.setPositiveButton(android.R.string.ok, (di, i) -> {
                });
                adb.show();
            }
        });
    }

    private void setRemoveButton() {
        binding.remove.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            MaterialAlertDialogBuilder removing = new MaterialAlertDialogBuilder(context);
            removing.setTitle("Removing...");
            removing.setView(R.layout.duck_cleaner);
            removing.setPositiveButton("Hide", (dI, ii) -> {
            });
            AlertDialog removingDialog = removing.create();
            adb.setTitle("Confirmation");
            adb.setMessage(
                    "The chroot environment will be deleted, including the following data:"
                            + "\n"
                            + "\n• files stored inside the environment"
                            + "\n• installed packages"
                            + "\n• environment settings"
                            + "\n• other data");
            adb.setPositiveButton("I'm sure.", (dialogInterface, i) -> new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
                @Override
                public void onPrepare() {
                    disableToolbarMenu(true);
                    setAllButtonEnable(false);
                    binding.progressbar.show();
                    binding.progressbar.setIndeterminate(true);
                    removingDialog.show();
                }

                @Override
                public void onNewLine(String line) {
                    MainActivity.notifyManagerBadge();
                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                }

                @Override
                public void onFinished(int code) {
                    disableToolbarMenu(false);
                    setAllButtonEnable(true);
                    compatCheck();
                    binding.progressbar.hide();
                    binding.progressbar.setIndeterminate(false);
                    removingDialog.dismiss();
                }
            }.exec(
                    PathsUtil.APP_SCRIPTS_PATH
                            + "/chrootmgr -c \"remove "
                            + PathsUtil.CHROOT_PATH()
                            + "\"", binding.report));
            adb.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            });
            adb.show();
        });
    }

    private void setBackupButton() {
        binding.backup.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            View v = getLayoutInflater().inflate(R.layout.manager_dialog_backup, null);
            TextInputEditText path = v.findViewById(R.id.input);
            path.setText(prefs.getString("chroot_backup_path", ""));
            adb.setView(v);
            adb.setPositiveButton("Do", (dialogInterface, i) -> new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
                @Override
                public void onPrepare() {
                    prefs.edit().putString("chroot_backup_path", path.getText().toString()).apply();
                    disableToolbarMenu(true);
                    setAllButtonEnable(false);
                    binding.progressbar.show();
                    binding.progressbar.setIndeterminate(true);
                }

                @Override
                public void onNewLine(String line) {
                    MainActivity.notifyManagerBadge();
                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                }

                @Override
                public void onFinished(int code) {
                    disableToolbarMenu(false);
                    setAllButtonEnable(true);
                    binding.progressbar.hide();
                    binding.progressbar.setIndeterminate(true);
                }
            }.exec(
                    PathsUtil.APP_SCRIPTS_PATH
                            + "/chrootmgr -c \"backup "
                            + PathsUtil.CHROOT_PATH() + " " + path.getText().toString() + "\"", binding.report));
            adb.show();
        });
    }

    private void showBanner() {
        new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
            @Override
            public void onPrepare() {
            }

            @Override
            public void onNewLine(String line) {
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onFinished(int code) {
            }
        }.exec(PathsUtil.APP_SCRIPTS_PATH + "/mhbanner", binding.report);
    }

    private void compatCheck() {
        new ShellUtils.ActiveShellExecutor(prefs.getBoolean("print_timestamp", false)) {
            @Override
            public void onPrepare() {
                disableToolbarMenu(true);
            }

            @Override
            public void onNewLine(String line) {
                MainActivity.notifyManagerBadge();
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onFinished(int code) {
                disableToolbarMenu(false);
                setButtonVisibility(code);
                setMountStatsTextView(code);
                setAllButtonEnable(true);
                MainActivity.setChrootInstalled(code == 0);
            }
        }.exec(
                PathsUtil.APP_SCRIPTS_PATH
                        + "/chrootmgr -c \"status\" -p "
                        + PathsUtil.CHROOT_PATH(), binding.report);
    }

    private void setMountStatsTextView(int MODE) {
        if (MODE == IS_MOUNTED) {
            MainActivity.setChrootStatus("Chroot is now running.");
        } else if (MODE == IS_UNMOUNTED) {
            MainActivity.setChrootStatus("Chroot hasn't yet started.");
        } else if (MODE == NEED_TO_INSTALL) {
            MainActivity.setChrootStatus("Chroot isn't yet installed.");
        } else if (MODE == CHROOT_CORRUPTED) {
            MainActivity.setChrootStatus("Chroot corrupted!");
        }
    }

    private void setButtonVisibility(int MODE) {
        switch (MODE) {
            case IS_MOUNTED:
                binding.start.setVisibility(View.GONE);
                binding.stop.setVisibility(View.VISIBLE);
                binding.install.setVisibility(View.GONE);
                binding.remove.setVisibility(View.GONE);
                binding.backup.setVisibility(View.GONE);
                break;
            case IS_UNMOUNTED:
                binding.start.setVisibility(View.VISIBLE);
                binding.stop.setVisibility(View.GONE);
                binding.install.setVisibility(View.GONE);
                binding.remove.setVisibility(View.VISIBLE);
                binding.backup.setVisibility(View.VISIBLE);
                break;
            case NEED_TO_INSTALL:
                binding.start.setVisibility(View.GONE);
                binding.stop.setVisibility(View.GONE);
                binding.install.setVisibility(View.VISIBLE);
                binding.remove.setVisibility(View.GONE);
                binding.backup.setVisibility(View.GONE);
                break;
            case CHROOT_CORRUPTED:
                binding.start.setVisibility(View.GONE);
                binding.stop.setVisibility(View.GONE);
                binding.install.setVisibility(View.GONE);
                binding.remove.setVisibility(View.VISIBLE);
                binding.backup.setVisibility(View.GONE);
        }
    }

    private void setAllButtonEnable(boolean isEnable) {
        binding.start.setEnabled(isEnable);
        binding.stop.setEnabled(isEnable);
        binding.install.setEnabled(isEnable);
        binding.remove.setEnabled(isEnable);
        binding.backup.setEnabled(isEnable);
    }

    private void disableToolbarMenu(boolean working) {
        setHasOptionsMenu(!working);
    }
}