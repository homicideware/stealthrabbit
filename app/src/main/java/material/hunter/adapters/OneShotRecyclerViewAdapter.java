package material.hunter.adapters;

import android.annotation.SuppressLint;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

import material.hunter.BuildConfig;
import material.hunter.R;
import material.hunter.databinding.OneshotItemBinding;
import material.hunter.ui.activities.menu.OneShot.OneShotItem;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.Utils;

public class OneShotRecyclerViewAdapter
        extends RecyclerView.Adapter<OneShotRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final SharedPreferences prefs;
    private final List<OneShotItem> oneShotItems;
    private final View view;

    public OneShotRecyclerViewAdapter(@NonNull Context context, List<OneShotItem> oneShotItems, View view) {
        this.context = context;
        this.oneShotItems = oneShotItems;
        this.view = view;
        prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public OneShotRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OneshotItemBinding binding = OneshotItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OneShotItem oneShotItem = oneShotItems.get(position);
        holder.ssid.setText(oneShotItem.getESSID());
        holder.ssid.setSelected(true);
        holder.bssid.setText(oneShotItem.getBSSID());
        holder.bssid.setSelected(true);
        holder.additionalInfo.setText(getNetworkAdditionalInfo(oneShotItem));
        holder.additionalInfo.setSelected(true);
        if (!oneShotItem.isWpsLocked()) {
            holder.openAttackDialog.setOnClickListener(v -> {
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
                            }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + oneShotItem.getBSSID() + " -p " + pin.getText().toString() + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                        } else {
                            outputCard.setVisibility(View.VISIBLE);
                            outputTextView.append("[-] Pin length must be: 8!\n");
                        }
                    });
                });
                pixieDust.setOnClickListener(v2 -> {
                    View pixieDustDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_only_output_dialog, null);
                    ScrollView scrollView = pixieDustDialog.findViewById(R.id.scrollView);
                    MaterialCardView outputCard = pixieDustDialog.findViewById(R.id.outputCard);
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

                        }
                    }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + oneShotItem.getBSSID() + " -K" + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                });
                pushButton.setOnClickListener(v2 -> {
                    View pushButtonDialog = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.oneshot_only_output_dialog, null);
                    ScrollView scrollView = pushButtonDialog.findViewById(R.id.scrollView);
                    MaterialCardView outputCard = pushButtonDialog.findViewById(R.id.outputCard);
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
                    new ShellUtils.YetAnotherActiveShellExecutor(true) {;

                        final double[] attempt = {0};

                        @Override
                        public void onPrepare() {
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

                        }
                    }.exec("python3 -u /usr/sbin/oneshot.py -i " + prefs.getString("macchanger_interface", "") + " -b " + oneShotItem.getBSSID() + " --push-button-connect" + (ifaceDown ? " --iface-down" : "") + (isMtkWifi ? " --mtk-wifi" : ""));
                });
            });
        } else {
            holder.openAttackDialog.setOnClickListener(v -> PathsUtil.showSnack(view, "WPS Locked!", false));
            holder.openAttackDialog.setImageResource(R.drawable.ic_lock);
        }
    }

    @NonNull
    private String getNetworkAdditionalInfo(@NonNull OneShotItem oneShotItem) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(oneShotItem.getSecurity());
        if (!oneShotItem.getWsc_model().isEmpty()) {
            stringBuilder.append(", ").append(oneShotItem.getWsc_model());
        }
        if (!oneShotItem.getWsc_device_name().isEmpty()) {
            stringBuilder.append(" (").append(oneShotItem.getWsc_device_name()).append(")");
        }
        stringBuilder.append(", ").append(oneShotItem.getSignal()).append(" dBm");
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
        return oneShotItems.size();
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

        public TextView ssid;
        public TextView bssid;
        public TextView additionalInfo;
        public ImageView openAttackDialog;

        public ViewHolder(View v) {
            super(v);
            ssid = v.findViewById(R.id.ssid);
            bssid = v.findViewById(R.id.bssid);
            additionalInfo = v.findViewById(R.id.additional_info);
            openAttackDialog = v.findViewById(R.id.open_attack_dialog);
        }
    }
}
