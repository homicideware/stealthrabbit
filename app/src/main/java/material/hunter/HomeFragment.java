package material.hunter;

import android.HardwareProps;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import material.hunter.utils.Checkers;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellExecuter;
import melville37.contract.JSON;
import melville37.contract.Web;

public class HomeFragment extends Fragment {

    private Activity activity;
    private Context context;
    private final ShellExecuter exe = new ShellExecuter();
    private ExecutorService executor;
    private SharedPreferences prefs;
    private int rotationAngle = 0;
    private boolean selinux_enforcing = true;
    private String selinux_now = "enforcing";
    private MaterialCardView mh_news_card;
    private TextView mh_news;
    private ImageView expander;
    private TextView version_installed;
    private TextView version_available;
    private ImageView upgrade;
    private MaterialCardView magisk;
    private TextView sys_info;
    private TextView material_info;
    private MaterialCardView info_card;
    private MaterialCardView selinux_card;
    private TextView selinux_status;
    private MaterialCardView telegram_card;
    private TextView telegram_title;
    private TextView telegram_description;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = getContext();
        activity = getActivity();
        executor = Executors.newSingleThreadExecutor();
        prefs = activity.getSharedPreferences("material.hunter", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mh_news_card = view.findViewById(R.id.mh_news_card);
        mh_news = view.findViewById(R.id.mh_news);
        expander = view.findViewById(R.id.expander);
        version_installed = view.findViewById(R.id.version_installed);
        version_available = view.findViewById(R.id.version_avaliable);
        upgrade = view.findViewById(R.id.upgrade);
        magisk = view.findViewById(R.id.magisk_card);
        sys_info = view.findViewById(R.id.sys_info);
        material_info = view.findViewById(R.id.material_info);
        info_card = view.findViewById(R.id.info_card);
        selinux_card = view.findViewById(R.id.selinux_card);
        selinux_status = view.findViewById(R.id.selinux_status);
        telegram_card = view.findViewById(R.id.telegram_card);
        telegram_title = view.findViewById(R.id.telegram_title);
        telegram_description = view.findViewById(R.id.telegram_description);

        version_installed.setText(BuildConfig.VERSION_NAME);
        mh_news.setOnTouchListener(new LinkMovementMethodOverride());

        executor.execute(() -> {
            String[] res = {""};
            try {
                res[0] = Web.getContent("https://raw.githubusercontent.com/Mirivan/dev-root-project/main/.materialhunter");
                prefs.edit().putString("last_news", res[0]).apply();
            } catch (IOException e) {
                res[0] = prefs.getString("last_news", "\u0410\u0432\u0442\u043E\u0440 \u043A\u043B\u0438\u0435\u043D\u0442\u0430 \u0443\u0432\u0430\u0436\u0430\u0435\u0442 \u041A\u043E\u043C\u0430\u0440\u0443, \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u043E\u0432\u043E\u0441\u0442\u0435\u0439 \u043D\u0435\u0442.");
            }
            new Handler(Looper.getMainLooper()).post(() -> mh_news.setText(res[0]));
        });

        mh_news_card.setOnClickListener(v -> {
            ObjectAnimator animation = ObjectAnimator.ofInt(mh_news, "maxLines", mh_news.getMaxLines() == 4 ? mh_news.getLineCount() : 4);
            animation.setDuration(200).start();
            rotationAngle = rotationAngle == 0 ? 180 : 0;
            expander.animate().rotation(rotationAngle).setDuration(200).start();
        });

        executor.execute(() -> {
            try {
                final JSONObject bubblegum = JSON.getFromWeb("https://raw.githubusercontent.com/Mirivan/dev-root-project/main/.materialised");

                if (bubblegum.has("version") && bubblegum.has("code") && bubblegum.has("url")) {

                    final String new_version = bubblegum.getString("version");
                    final int code = bubblegum.getInt("code");
                    final String url = bubblegum.getString("url");

                    new Handler(Looper.getMainLooper()).post(() -> {

                        version_available.setText(new_version);

                        if (code > BuildConfig.VERSION_CODE && !new_version.equals(BuildConfig.VERSION_NAME)) {
                            upgrade.setVisibility(View.VISIBLE);
                            upgrade.setOnClickListener(v -> {
                                Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(openUrl);
                            });
                        }
                    });
                }
            } catch (JSONException | IOException ignored) {
            }
        });

        executor.execute(() -> {
            if (magiskPassed()) {
                // nothing to do
            } else {
                if (!prefs.getBoolean("hide_magisk_notification", false)) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        magisk.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
        magisk.setOnClickListener(v -> {
            if (exe.RunAsRootReturnValue("[ -f " + PathsUtil.MAGISK_DB_PATH + " ]") == 0) {
                if (exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT * from policies\" | grep material.hunter").startsWith("material.hunter")) {
                    exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE package_name='material.hunter';\"");
                } else {
                    exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE uid='$(stat -c %u /data/data/material.hunter)';\"");
                }
            }
            magisk.setVisibility(View.GONE);
        });
        magisk.setOnLongClickListener(v -> {
            prefs.edit().putBoolean("hide_magisk_notification", true).apply();
            magisk.setVisibility(View.GONE);
            return true;
        });

        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();

            sb.append("Model: ").append(Build.BRAND).append(" ").append(Build.MODEL).append(" (").append(HardwareProps.getProp("ro.build.product")).append(")\n");
            sb.append("OS Version: Android ").append(Build.VERSION.RELEASE).append(", SDK ").append(Build.VERSION.SDK_INT).append("\n");

            String CPU = matchString("^Hardware.*: (.*)", exe.RunAsRootOutput("cat /proc/cpuinfo | grep \"Hardware\""), 1);
            if (!CPU.isEmpty())
                sb.append("CPU: ").append(CPU).append("\n");

            String kernel_version = matchString("^([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})", exe.RunAsRootOutput("uname -r"), 1);

            sb.append("Kernel version: ").append(kernel_version).append("\n\n");

            sb.append("System-as-root: ").append(exe.RunAsRootOutput("grep ' / ' /proc/mounts | grep -qv 'rootfs' || grep -q ' /system_root ' /proc/mounts && echo true || echo false")).append("\n");

            sb.append("Device is AB: ").append(HardwareProps.deviceIsAB() ? "true" : "false");

            new Handler(Looper.getMainLooper()).post(() -> {
                sys_info.setText(sb.toString());
                try {
                    MainActivity.setKernelBase(Float.parseFloat(matchString("^([1-9]\\.[1-9][0-9]{0,2})", kernel_version, 1)));
                } catch (NumberFormatException e) {
                    PathsUtil.showSnack(getView(), "Failed to parse kernel base version.", false);
                }
            });
        });

        material_info.setText("Made with \u2764\uFE0F by @mirivan");

        info_card.setOnClickListener(v -> {
            Intent intent = new Intent(context, AboutActivity.class);
            startActivity(intent);
        });

        executor.execute(() -> {
            selinux_enforcing = Checkers.isEnforcing();
            selinux_now = selinux_enforcing ? "enforcing" : "permissive";

            new Handler(Looper.getMainLooper()).post(() -> {
                selinux_status.setText("Selinux status: " + selinux_now + ". Click to change it.");
            });
        });
        selinux_card.setOnClickListener(v -> executor.execute(() -> {
            exe.RunAsRoot("setenforce " + (selinux_enforcing ? "0" : "1"));
            selinux_enforcing = !selinux_enforcing;
            selinux_now = selinux_enforcing ? "enforcing" : "permissive";

            new Handler(Looper.getMainLooper()).post(() -> {
                selinux_status.setText("Selinux status: " + selinux_now + ". Click to change it.");
            });
        }));

        executor.execute(() -> {
            String[] telegram_parsed = {""};
            try {
                telegram_parsed[0] = Web.getContent("https://t.me/kali_nh");
            } catch (IOException ignored) {
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (telegram_parsed[0].isEmpty()) {
                    telegram_card.setVisibility(View.GONE);
                } else {
                    telegram_card.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/kali_nh"));
                        startActivity(intent);
                    });
                }
                telegram_title.setText(
                        Html.fromHtml(
                                matchString(".*<meta property=\"og:title\" content=\"(.*)\">", telegram_parsed[0], "N/a", 1),
                                Html.FROM_HTML_MODE_LEGACY));
                telegram_description.setText(
                        Html.fromHtml(
                                matchString(".*<meta property=\"og:description\" content=\"(.*)\">", telegram_parsed[0], 1),
                                Html.FROM_HTML_MODE_LEGACY));
                telegram_description.setSelected(true);
            });
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean magiskPassed() {
        boolean result;
        boolean res = exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT * from policies\" | grep material.hunter").startsWith("(material.hunter");
        if (res) {
            result = exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT notification from policies WHERE package_name='material.hunter'\"").equals("0");
        } else {
            result = exe.RunAsRootOutput(PathsUtil.APP_SCRIPTS_BIN_PATH + "/sqlite3 " + PathsUtil.MAGISK_DB_PATH + " \"SELECT notification from policies WHERE uid='$(stat -c %u /data/data/material.hunter)'\"").equals("0");
        }
        return result;
    }

    public String matchString(String regex, String string, int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);
        while (matcher.find())
            return matcher.group(group);
        return "";
    }

    public String matchString(String regex, String string, String defaultValue, int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);
        while (matcher.find())
            return matcher.group(group);
        return defaultValue;
    }

    // https://stackoverflow.com/a/15362634
    public static class LinkMovementMethodOverride implements View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            TextView widget = (TextView) v;
            Object text = widget.getText();
            if (text instanceof Spanned) {
                Spanned buffer = (Spanned) text;

                int action = event.getAction();

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= widget.getTotalPaddingLeft();
                    y -= widget.getTotalPaddingTop();

                    x += widget.getScrollX();
                    y += widget.getScrollY();

                    Layout layout = widget.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] link = buffer.getSpans(off, off,
                            ClickableSpan.class);

                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            link[0].onClick(widget);
                        } else if (action == MotionEvent.ACTION_DOWN) {
                            // Selection only works on Spannable text. In our case setSelection doesn't work on spanned text
                            // Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }
}