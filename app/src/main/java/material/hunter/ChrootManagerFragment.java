package material.hunter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import material.hunter.AsyncTask.ChrootManagerAsynctask;
import material.hunter.service.CompatCheckService;
import material.hunter.service.NotificationChannelService;
import material.hunter.utils.NhPaths;
import material.hunter.utils.SharePrefTag;
import material.hunter.utils.ShellExecuter;

public class ChrootManagerFragment extends Fragment {

    public static final String TAG = "ChrootManager";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int IS_MOUNTED = 0;
    private static final int IS_UNMOUNTED = 1;
    private static final int NEED_TO_INSTALL = 2;
    public static boolean isAsyncTaskRunning = false;
    private final Intent backPressedintent = new Intent();
    private TextView mountStatsTextView;
    private TextView baseChrootPathTextView;
    private TextView resultViewerLoggerTextView;
    private TextView kaliFolderTextView;
    private Button kaliFolderEditButton;
    private Button mountChrootButton;
    private Button unmountChrootButton;
	private Button optionsChrootButton;
    private Button installChrootButton;
    private Button removeChrootButton;
    private Button backupChrootButton;
    private SharedPreferences sharedPreferences;
    private ChrootManagerAsynctask chrootManagerAsynctask;
    private Context context;
    private Activity activity;

    public static ChrootManagerFragment newInstance(int sectionNumber) {
        ChrootManagerFragment fragment = new ChrootManagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chroot_manager, container, false);
        sharedPreferences = activity.getSharedPreferences("material.hunter", Context.MODE_PRIVATE);
        baseChrootPathTextView = rootView.findViewById(R.id.f_chrootmanager_base_path_tv);
        mountStatsTextView = rootView.findViewById(R.id.f_chrootmanager_mountresult_tv);
        resultViewerLoggerTextView = rootView.findViewById(R.id.f_chrootmanager_viewlogger);
        kaliFolderTextView = rootView.findViewById(R.id.f_chrootmanager_kalifolder_tv);
        kaliFolderEditButton = rootView.findViewById(R.id.f_chrootmanager_edit_btn);
        mountChrootButton = rootView.findViewById(R.id.f_chrootmanager_mount_btn);
        unmountChrootButton = rootView.findViewById(R.id.f_chrootmanager_unmount_btn);
		optionsChrootButton = rootView.findViewById(R.id.f_chrootmanager_options_btn);
        installChrootButton = rootView.findViewById(R.id.f_chrootmanager_install_btn);
        removeChrootButton = rootView.findViewById(R.id.f_chrootmanager_removechroot_btn);
        backupChrootButton = rootView.findViewById(R.id.f_chrootmanager_backupchroot_btn);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resultViewerLoggerTextView.setMovementMethod(new ScrollingMovementMethod());
        kaliFolderTextView.setText(sharedPreferences.getString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, NhPaths.ARCH_FOLDER));
        setEditButton();
        setStopKaliButton();
		setOptionsButton();
        setStartKaliButton();
        setInstallChrootButton();
        setRemoveChrootButton();
        setBackupChrootButton();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAsyncTaskRunning) {
            showBanner();
            compatCheck();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mountStatsTextView = null;
        baseChrootPathTextView = null;
        resultViewerLoggerTextView = null;
        kaliFolderTextView = null;
        kaliFolderEditButton = null;
        mountChrootButton = null;
        unmountChrootButton = null;
		optionsChrootButton = null;
        installChrootButton = null;
        removeChrootButton = null;
        backupChrootButton = null;
        chrootManagerAsynctask = null;
    }

    private void setEditButton() {
        kaliFolderEditButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity);
            LinearLayout ll = new LinearLayout(activity);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setLayoutParams(layoutParams);
            EditText chrootPathEditText = new EditText(activity);
            TextView availableChrootPathextview = new TextView(activity);
            LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            editTextParams.setMargins(58, 0, 58, 0);
            chrootPathEditText.setText(sharedPreferences.getString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, ""));
            chrootPathEditText.setSingleLine();
            chrootPathEditText.setLayoutParams(editTextParams);
            availableChrootPathextview.setLayoutParams(editTextParams);
            availableChrootPathextview.setTextColor(getResources().getColor(R.color.clearTitle));
            File chrootDir = new File(NhPaths.NH_SYSTEM_PATH);
            int count = 0;
			availableChrootPathextview.append("\n");
            for (File file : chrootDir.listFiles()) {
                if (file.isDirectory()) {
                    if (file.getName().equals("kalifs")) continue;
                    count += 1;
                    availableChrootPathextview.append("    " + count + ". " + file.getName() + "\n");
                }
            }
            ll.addView(chrootPathEditText);
            ll.addView(availableChrootPathextview);
            adb.setCancelable(true);
            adb.setTitle("Setup");
            adb.setMessage("Prefixed to:\n\"/data/local/nhsystem/\"");
            adb.setView(ll);
            adb.setPositiveButton("Apply", (dialogInterface, i) -> {
                if (chrootPathEditText.getText().toString().matches("^\\.(.*$)|^\\.\\.(.*$)|^/+(.*$)|^.*/+(.*$)|^$")) {
                    NhPaths.showMessage(activity, "Invalid name", false);
                } else {
                    NhPaths.ARCH_FOLDER = chrootPathEditText.getText().toString();
                    kaliFolderTextView.setText(NhPaths.ARCH_FOLDER);
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, NhPaths.ARCH_FOLDER).apply();
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, NhPaths.CHROOT_PATH()).apply();
                    new ShellExecuter().RunAsRootOutput("ln -sfn " + NhPaths.CHROOT_PATH() + " " + NhPaths.CHROOT_SYMLINK_PATH);
                    compatCheck();
                }
                dialogInterface.dismiss();
            });
            adb.show();
        });
    }

    private void setStartKaliButton() {
        mountChrootButton.setOnClickListener(view -> {
            chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.MOUNT_CHROOT);
            chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                @Override
                public void onAsyncTaskPrepare() {
                    setAllButtonEnable(false);
                }

                @Override
                public void onAsyncTaskProgressUpdate(int progress) {
                }

                @Override
                public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                    if (resultCode == 0) {
                        setButtonVisibilty(IS_MOUNTED);
                        setMountStatsTextView(IS_MOUNTED);
                        setAllButtonEnable(true);
                        compatCheck();
                        context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.USENETHUNTER));
                    }
                }
            });
            chrootManagerAsynctask.execute(resultViewerLoggerTextView);
        });
    }

    private void setStopKaliButton() {
        unmountChrootButton.setOnClickListener(view -> {
            chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.UNMOUNT_CHROOT);
            chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                @Override
                public void onAsyncTaskPrepare() {
                    setAllButtonEnable(false);
                }

                @Override
                public void onAsyncTaskProgressUpdate(int progress) {
                }

                @Override
                public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                    if (resultCode == 0) {
                        setMountStatsTextView(IS_UNMOUNTED);
                        setButtonVisibilty(IS_UNMOUNTED);
                        setAllButtonEnable(true);
                        compatCheck();
                    }
                }
            });
            chrootManagerAsynctask.execute(resultViewerLoggerTextView);
        });
    }

    private void setOptionsButton() {
		optionsChrootButton.setOnClickListener(view -> {
			MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            final View rootView = getLayoutInflater().inflate(R.layout.chroot_manager_options, null);
			final TextInputEditText editText = rootView.findViewById(R.id.hostname);
			editText.setText(sharedPreferences.getString("hostname", "android"));
			adb.setView(rootView);
			adb.setPositiveButton("Setup", (dialogInterface, i) -> {
				final String hostname = editText.getText().toString();
				if (!hostname.matches("([a-zA-Z0-9-]){2,253}")) {
					NhPaths.showSnack(getView(), "Invalid hostname", false);
					return;
				}
			    sharedPreferences.edit().putString("hostname", hostname).apply();
				NhPaths.showSnack(getView(), "Need remounting chroot!", false);
			});
			adb.setNegativeButton("Cancel", (dialogInterface, i) -> {});
			adb.show();
		});
	}

    private void setInstallChrootButton() {
        installChrootButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
            final View rootView = getLayoutInflater().inflate(R.layout.chroot_manager_download_dialog, null);
			final LinearProgressIndicator progressbar = getView().findViewById(R.id.progressbar);
            Button db = rootView.findViewById(R.id.downloadButton);
            Button rb = rootView.findViewById(R.id.restoreButton);
            adb.setTitle("Install");
			adb.setView(rootView);
			AlertDialog ad = adb.create();
            db.setOnClickListener(view1 -> {
                ad.dismiss();
                MaterialAlertDialogBuilder adb1 = new MaterialAlertDialogBuilder(activity);
                final View promtDownloadView = getLayoutInflater().inflate(R.layout.chroot_manager_prepare_dialog, null);
                final TextInputEditText storepathEditText = promtDownloadView.findViewById(R.id.link);
                storepathEditText.setText(sharedPreferences.getString(SharePrefTag.CHROOT_DEFAULT_STORE_DOWNLOAD_SHAREPREF_TAG, ""));
                adb1.setView(promtDownloadView);
                adb1.setPositiveButton("Setup", (dialogInterface, i) -> {
                    String chroot_url = storepathEditText.getText().toString();
					if (!chroot_url.equals("")) {
						if (!chroot_url.startsWith("http://") && !chroot_url.startsWith("https://")) {
						    chroot_url = "http://" + chroot_url;
					    }
						if (!chroot_url.endsWith(".xz") && !chroot_url.endsWith(".gz")) {
							NhPaths.showMessage(context, "Tarball must be xz or gz compression.", true);
							return;
						}
					} else {
						NhPaths.showMessage(context, "URL is incorrect!", false);
			            return;
					}
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_DEFAULT_STORE_DOWNLOAD_SHAREPREF_TAG, chroot_url).apply();
		            String filename;
					try {
					    filename = Paths.get(new URL(chroot_url).getPath()).getFileName().toString();
					} catch (MalformedURLException e) {
						NhPaths.showMessage(context, "Runtime exception throwed!", false);
						throw new RuntimeException(e);
					}
					context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.DOWNLOADING));
					File chroot = new File("/sdcard/" + filename);
                    chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.DOWNLOAD_CHROOT);
                    chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                        @Override
                        public void onAsyncTaskPrepare() {
                            broadcastBackPressedIntent(false);
                            setAllButtonEnable(false);
							progressbar.show();
                            progressbar.setProgress(0);
                            progressbar.setMax(100);
                        }

                        @Override
                        public void onAsyncTaskProgressUpdate(int progress) {
                            if (progress == 100) {
                                progressbar.setIndeterminate(true);
                            } else {
                                progressbar.setProgress(progress);
                            }
                        }

                        @Override
                        public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                            if (resultCode == 0) {
                                chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.INSTALL_CHROOT);
                                chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                                    @Override
                                    public void onAsyncTaskPrepare() {
                                        context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.INSTALLING));
                                        broadcastBackPressedIntent(false);
                                        setAllButtonEnable(false);
                                    }

                                    @Override
                                    public void onAsyncTaskProgressUpdate(int progress) {
                                    }

                                    @Override
                                    public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                                        broadcastBackPressedIntent(true);
                                        setAllButtonEnable(true);
                                        compatCheck();
								        progressbar.hide();
                                    }
                                });
                                chrootManagerAsynctask.execute(resultViewerLoggerTextView, chroot, NhPaths.CHROOT_PATH());
                            } else {
                                progressbar.hide();
                            }
                        }
                    });
                    chrootManagerAsynctask.execute(resultViewerLoggerTextView, chroot_url, chroot);
					chroot.delete();
                });
                adb1.create().show();
            });
            rb.setOnClickListener(view12 -> {
				ad.dismiss();
            	MaterialAlertDialogBuilder adb2 = new MaterialAlertDialogBuilder(activity);
            	final View rootViewR = getLayoutInflater().inflate(R.layout.chroot_restore, null);
                final TextInputEditText et = rootViewR.findViewById(R.id.chrootRestorePath);
                et.setText(sharedPreferences.getString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, ""));
                adb2.setView(rootViewR);
                adb2.setPositiveButton("OK", (dialogInterface, i) -> {
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, et.getText().toString()).apply();
                    chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.INSTALL_CHROOT);
                    chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                        @Override
                        public void onAsyncTaskPrepare() {
                            context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.INSTALLING));
                            broadcastBackPressedIntent(false);
                            setAllButtonEnable(false);
							progressbar.show();
                            progressbar.setIndeterminate(true);
                        }
                        @Override
                        public void onAsyncTaskProgressUpdate(int progress) { }
                        @Override
                        public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                            compatCheck();
							progressbar.hide();
                        }
                    });
                    chrootManagerAsynctask.execute(resultViewerLoggerTextView, et.getText().toString(), NhPaths.CHROOT_PATH());
                });
                adb2.show();
            });
            ad.show();
        });
    }

    private void setRemoveChrootButton() {
        removeChrootButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity)
                .setTitle("Warning!")
				.setView(R.layout.noroot)
                .setMessage("Are you sure to remove the below Chroot folder?\n" + NhPaths.CHROOT_PATH())
                .setPositiveButton("I'm sure.", (dialogInterface, i) -> {
                    chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.REMOVE_CHROOT);
                    chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                        @Override
                        public void onAsyncTaskPrepare() {
                            broadcastBackPressedIntent(false);
                            setAllButtonEnable(false);
                        }

                        @Override
                        public void onAsyncTaskProgressUpdate(int progress) {
                        }

                        @Override
                        public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                            compatCheck();
                        }
                    });
                    chrootManagerAsynctask.execute(resultViewerLoggerTextView);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
            });
            adb.show();
        });
    }

    private void setBackupChrootButton() {
        backupChrootButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity);
            EditText backupFullPathEditText = new EditText(activity);
            LinearLayout ll = new LinearLayout(activity);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setLayoutParams(layoutParams);
            LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            editTextParams.setMargins(58, 40, 58, 0);
            backupFullPathEditText.setLayoutParams(editTextParams);
            ll.addView(backupFullPathEditText);
            adb.setTitle("Backup Chroot");
            adb.setMessage("Create a backup of the your chroot environment.\n\nPath: \"" + NhPaths.CHROOT_PATH() + "\" to:");
            backupFullPathEditText.setText(sharedPreferences.getString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, ""));
			adb.setView(ll);
            adb.setPositiveButton("OK", (dialogInterface, i) -> {
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, backupFullPathEditText.getText().toString()).apply();
                if (new File(backupFullPathEditText.getText().toString()).exists()) {
					dialogInterface.dismiss();
                    MaterialAlertDialogBuilder ad2 = new MaterialAlertDialogBuilder(activity);
                    ad2.setMessage("File exists already, do you want ot overwrite it anyway?");
                    ad2.setPositiveButton("Yes", (dialogInterface1, i1) -> {
                        chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.BACKUP_CHROOT);
                        chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                            @Override
                            public void onAsyncTaskPrepare() {
                                context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.BACKINGUP));
                                broadcastBackPressedIntent(false);
                                setAllButtonEnable(false);
                            }

                            @Override
                            public void onAsyncTaskProgressUpdate(int progress) {
                            }

                            @Override
                            public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                                broadcastBackPressedIntent(true);
                                setAllButtonEnable(true);
                            }
                        });
                        chrootManagerAsynctask.execute(resultViewerLoggerTextView, NhPaths.CHROOT_PATH(), backupFullPathEditText.getText().toString());
                    });
					ad2.setNegativeButton("Cancel", (dialogInterface2, i2) -> {});
                    ad2.show();
                } else {
                    chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.BACKUP_CHROOT);
                    chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
                        @Override
                        public void onAsyncTaskPrepare() {
                            context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.BACKINGUP));
                            broadcastBackPressedIntent(false);
                            setAllButtonEnable(false);
                        }

                        @Override
                        public void onAsyncTaskProgressUpdate(int progress) {
                        }

                        @Override
                        public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                        }
                    });
                    chrootManagerAsynctask.execute(resultViewerLoggerTextView, NhPaths.CHROOT_PATH(), backupFullPathEditText.getText().toString());
                }
            });
            adb.show();
        });
    }

    private void showBanner() {
        chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.ISSUE_BANNER);
        chrootManagerAsynctask.execute(resultViewerLoggerTextView, "MaterialHunter 3");
    }

    private void compatCheck() {
        chrootManagerAsynctask = new ChrootManagerAsynctask(ChrootManagerAsynctask.CHECK_CHROOT);
        chrootManagerAsynctask.setListener(new ChrootManagerAsynctask.ChrootManagerAsyncTaskListener() {
            @Override
            public void onAsyncTaskPrepare() {
                broadcastBackPressedIntent(false);
            }

            @Override
            public void onAsyncTaskProgressUpdate(int progress) {
            }

            @Override
            public void onAsyncTaskFinished(int resultCode, ArrayList<String> resultString) {
                broadcastBackPressedIntent(true);
                setButtonVisibilty(resultCode);
                setMountStatsTextView(resultCode);
                setAllButtonEnable(true);
                context.startService(new Intent(context, CompatCheckService.class).putExtra("RESULTCODE", resultCode));
            }
        });
        chrootManagerAsynctask.execute(resultViewerLoggerTextView, sharedPreferences.getString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, ""));
    }

    private void setMountStatsTextView(int MODE) {
        if (MODE == IS_MOUNTED) {
            mountStatsTextView.setTextColor(Color.GREEN);
            mountStatsTextView.setText("Chroot is now running!");
        } else if (MODE == IS_UNMOUNTED) {
            mountStatsTextView.setTextColor(Color.CYAN);
            mountStatsTextView.setText("Chroot hasn't yet started!");
        } else if (MODE == NEED_TO_INSTALL) {
            mountStatsTextView.setTextColor(Color.parseColor("#D81B60"));
            mountStatsTextView.setText("Chroot isn't yet installed!");
        }
    }

    private void setButtonVisibilty(int MODE) {
        switch (MODE) {
            case IS_MOUNTED:
                mountChrootButton.setVisibility(View.GONE);
                unmountChrootButton.setVisibility(View.VISIBLE);
				// optionsChrootButton.setVisibility(View.VISIBLE);
                installChrootButton.setVisibility(View.GONE);
                removeChrootButton.setVisibility(View.GONE);
                backupChrootButton.setVisibility(View.GONE);
                break;
            case IS_UNMOUNTED:
                mountChrootButton.setVisibility(View.VISIBLE);
                unmountChrootButton.setVisibility(View.GONE);
				// optionsChrootButton.setVisibility(View.GONE);
                installChrootButton.setVisibility(View.GONE);
                removeChrootButton.setVisibility(View.VISIBLE);
                backupChrootButton.setVisibility(View.VISIBLE);
                break;
            case NEED_TO_INSTALL:
                mountChrootButton.setVisibility(View.GONE);
                unmountChrootButton.setVisibility(View.GONE);
				// optionsChrootButton.setVisibility(View.GONE);
                installChrootButton.setVisibility(View.VISIBLE);
                removeChrootButton.setVisibility(View.GONE);
                backupChrootButton.setVisibility(View.GONE);
                break;
        }
    }

    private void setAllButtonEnable(boolean isEnable) {
        mountChrootButton.setEnabled(isEnable);
        unmountChrootButton.setEnabled(isEnable);
		// optionsChrootButton.setEnabled(isEnable);
        installChrootButton.setEnabled(isEnable);
        removeChrootButton.setEnabled(isEnable);
        kaliFolderEditButton.setEnabled(isEnable);
        backupChrootButton.setEnabled(isEnable);
    }

    private void broadcastBackPressedIntent(Boolean isEnabled) {
        setHasOptionsMenu(isEnabled);
    }
}