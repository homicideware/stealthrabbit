package material.hunter.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.OneshotItemBinding;
import material.hunter.ui.activities.menu.OneShot.Item;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.Utils;

public class OneShotRecyclerViewAdapter
        extends RecyclerView.Adapter<OneShotRecyclerViewAdapter.ViewHolder> {

    private final Activity activity;
    private final Context context;
    private final SharedPreferences prefs;
    private final List<Item> items;

    public OneShotRecyclerViewAdapter(Activity activity, @NonNull Context context, List<Item> items) {
        this.context = context;
        this.items = items;
        this.activity = activity;
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public OneShotRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OneshotItemBinding binding = OneshotItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("OneShot", text);
        clipboard.setPrimaryClip(clip);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.getItemCard().setOnLongClickListener(v -> {
            View itemDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_item_dialog, null);
            Button copyEssid = itemDialog.findViewById(R.id.oneshot_copy_essid);
            Button copyBssid = itemDialog.findViewById(R.id.oneshot_copy_bssid);
            Button copyAdditionalInfo = itemDialog.findViewById(R.id.oneshot_copy_additional_info);
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                    .setTitle(item.getESSID())
                    .setView(itemDialog)
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .create();
            copyEssid.setOnClickListener(v2 -> {
                copyTextToClipboard(item.getESSID());
                alertDialog.cancel();
            });
            copyBssid.setOnClickListener(v2 -> {
                copyTextToClipboard(item.getBSSID());
                alertDialog.cancel();
            });
            copyAdditionalInfo.setOnClickListener(v2 -> {
                copyTextToClipboard(getNetworkAdditionalInfo(item));
                alertDialog.cancel();
            });
            alertDialog.show();
            return false;
        });
        holder.getSsid().setText(item.getESSID());
        holder.getSsid().setSelected(true);
        holder.getBssid().setText(item.getBSSID());
        holder.getBssid().setSelected(true);
        holder.getAdditionalInfo().setText(getNetworkAdditionalInfo(item));
        holder.getAdditionalInfo().setSelected(true);
        if (!item.isWpsLocked()) {
            holder.getOpenAttackDialog().setOnClickListener(v -> {
                View attackDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_arguments_dialog, null);
                Button customPin = attackDialog.findViewById(R.id.oneshot_custom_pin);
                Button offlineBruteforce = attackDialog.findViewById(R.id.oneshot_offline_bruteforce);
                Button onlineBruteforce = attackDialog.findViewById(R.id.oneshot_online_bruteforce);
                Button pushButton = attackDialog.findViewById(R.id.oneshot_push_button);
                Button pixieDust = attackDialog.findViewById(R.id.oneshot_pixie_dust);
                offlineBruteforce.setEnabled(false);
                onlineBruteforce.setEnabled(false);
                boolean ifaceDown = prefs.getBoolean("oneshot_iface_down", false);
                boolean isMtkWifi = prefs.getBoolean("oneshot_mtk_wifi", false);
                new MaterialAlertDialogBuilder(context)
                        .setTitle("OneShot")
                        .setView(attackDialog)
                        .setPositiveButton(android.R.string.cancel, (di, i) -> {
                        })
                        .show();
                customPin.setOnClickListener(v2 -> {
                    View customPinDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_custom_pin_dialog, null);
                    ScrollView scrollView = customPinDialog.findViewById(R.id.scrollView);
                    TextInputLayout pinLayout = customPinDialog.findViewById(R.id.pin_layout);
                    TextInputEditText pin = customPinDialog.findViewById(R.id.pin);
                    MaterialCardView outputCard = customPinDialog.findViewById(R.id.outputCard);
                    TextView outputTextView = customPinDialog.findViewById(R.id.outputTextView);
                    final Process[] process = {null};
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("OneShot : Custom Pin")
                            .setView(customPinDialog)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.cancel, (di, i) -> {
                                if (process[0] != null)
                                    process[0].destroy();
                            })
                            .show();
                    pinLayout.setEndIconOnClickListener(v3 -> {
                        if (pin.getText().toString().length() == 8) {
                            new ShellUtils.YetAnotherActiveShellExecutor(true) {

                                final double[] attempt = {0};

                                @Override
                                public void onPrepare() {
                                    pinLayout.setEnabled(false);
                                    outputCard.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onNewLine(String line) {
                                    if (process[0] == null)
                                        process[0] = getProcess();
                                    if (line.equals("[*] Scanning…") || line.equals("[*] Associating with AP…")) {
                                        if (attempt[0] > 3) {
                                            writeLineToLogger("[-] Attack failed, AP isn't vulnerable or signal is low.", outputTextView);
                                            process[0].destroy();
                                        } else {
                                            writeLineToLogger(line, outputTextView);
                                            attempt[0] += 0.5;
                                        }
                                    } else {
                                        if (attempt[0] > 0) {
                                            attempt[0] = 0;
                                        }
                                        writeLineToLogger(line, outputTextView);
                                    }
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }

                                @Override
                                public void onNewErrorLine(String line) {
                                    writeLineToLogger(line, outputTextView);
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }

                                @Override
                                public void onFinished(int code) {
                                    pinLayout.setEnabled(true);
                                }
                            }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + item.getBSSID() + " -p " + pin.getText().toString() + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                        } else {
                            outputCard.setVisibility(View.VISIBLE);
                            outputTextView.append("[-] Pin length must be: 8!\n");
                        }
                    });
                });
                pixieDust.setOnClickListener(v2 -> {
                    View pixieDustDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_only_output_dialog, null);
                    ScrollView scrollView = pixieDustDialog.findViewById(R.id.scrollView);
                    TextView outputTextView = pixieDustDialog.findViewById(R.id.outputTextView);
                    final Process[] process = {null};
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("OneShot : Pixie Dust")
                            .setView(pixieDustDialog)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.cancel, (di, i) -> {
                                if (process[0] != null)
                                    process[0].destroy();
                            })
                            .show();
                    new ShellUtils.YetAnotherActiveShellExecutor(true) {

                        final double[] attempt = {0};

                        @Override
                        public void onPrepare() {

                        }

                        @Override
                        public void onNewLine(String line) {
                            if (process[0] == null)
                                process[0] = getProcess();
                            if (line.equals("[*] Scanning…") || line.equals("[*] Associating with AP…")) {
                                if (attempt[0] > 3) {
                                    writeLineToLogger("[-] Attack failed, AP isn't vulnerable or signal is low.", outputTextView);
                                    process[0].destroy();
                                } else {
                                    writeLineToLogger(line, outputTextView);
                                    attempt[0] += 0.5;
                                }
                            } else {
                                if (attempt[0] > 0) {
                                    attempt[0] = 0;
                                }
                                writeLineToLogger(line, outputTextView);
                            }
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }

                        @Override
                        public void onNewErrorLine(String line) {
                            writeLineToLogger(line, outputTextView);
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }

                        @Override
                        public void onFinished(int code) {

                        }
                    }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + item.getBSSID() + " -K" + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                });
                pushButton.setOnClickListener(v2 -> {
                    View pushButtonDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_only_output_dialog, null);
                    ScrollView scrollView = pushButtonDialog.findViewById(R.id.scrollView);
                    TextView outputTextView = pushButtonDialog.findViewById(R.id.outputTextView);
                    final Process[] process = {null};
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("OneShot : Push Button")
                            .setView(pushButtonDialog)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.cancel, (di, i) -> {
                                if (process[0] != null)
                                    process[0].destroy();
                            })
                            .show();
                    new ShellUtils.YetAnotherActiveShellExecutor(true) {
                        ;

                        final double[] attempt = {0};

                        @Override
                        public void onPrepare() {

                        }

                        @Override
                        public void onNewLine(String line) {
                            if (process[0] == null)
                                process[0] = getProcess();
                            if (line.equals("[*] Scanning…") || line.equals("[*] Associating with AP…")) {
                                if (attempt[0] > 3) {
                                    writeLineToLogger("[-] Attack failed, AP isn't vulnerable or signal is low.", outputTextView);
                                    process[0].destroy();
                                } else {
                                    writeLineToLogger(line, outputTextView);
                                    attempt[0] += 0.5;
                                }
                            } else {
                                if (attempt[0] > 0) {
                                    attempt[0] = 0;
                                }
                                writeLineToLogger(line, outputTextView);
                            }
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }

                        @Override
                        public void onNewErrorLine(String line) {
                            writeLineToLogger(line, outputTextView);
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }

                        @Override
                        public void onFinished(int code) {

                        }
                    }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + item.getBSSID() + " --push-button-connect" + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                });
            });
        } else {
            holder.getOpenAttackDialog().setOnClickListener(v -> PathsUtil.showSnackBar(activity, "WPS Locked!", false));
            holder.getOpenAttackDialog().setImageResource(R.drawable.ic_lock);
        }
    }

    @NonNull
    private String getNetworkAdditionalInfo(@NonNull Item item) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(item.getSecurity());
        if (!item.getWsc_model().isEmpty()) {
            stringBuilder.append(", ").append(item.getWsc_model());
        }
        if (!item.getWsc_device_name().isEmpty()) {
            stringBuilder.append(" (").append(item.getWsc_device_name()).append(")");
        }
        stringBuilder.append(", ").append(item.getSignal()).append(" dBm");
        return stringBuilder.toString();
    }

    private void writeLineToLogger(String line, TextView logger) {
        if (prefs.getBoolean("oneshot_compact_stdout", true)) {
            if (line.equals("[*] Running wpa_supplicant…")) {
                String[] loggerLines = logger.getText().toString().split("\n");
                if (!Utils.arrayContains(loggerLines, "[*] Running wpa_supplicant…")) {
                    logger.append(line + "\n");
                }
            } else if (!line.startsWith("[P]")) {
                logger.append(line + "\n");
            } else if (line.startsWith("[+] Associated with")) {
                logger.append("[+] Associated with " + Utils.matchString("\\(ESSID: (.*)\\)$", line, "network", 1));
                logger.append("\n");
            }
        } else {
            logger.append(line + "\n");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private MaterialCardView itemCard;
        private TextView ssid;
        private TextView bssid;
        private TextView additionalInfo;
        private ImageView openAttackDialog;

        public ViewHolder(View v) {
            super(v);
            itemCard = v.findViewById(R.id.item_card);
            ssid = v.findViewById(R.id.ssid);
            bssid = v.findViewById(R.id.bssid);
            additionalInfo = v.findViewById(R.id.additional_info);
            openAttackDialog = v.findViewById(R.id.open_attack_dialog);
        }

        public MaterialCardView getItemCard() {
            return itemCard;
        }

        public TextView getSsid() {
            return ssid;
        }

        public TextView getBssid() {
            return bssid;
        }

        public TextView getAdditionalInfo() {
            return additionalInfo;
        }

        public ImageView getOpenAttackDialog() {
            return openAttackDialog;
        }
    }
}
