package org.homicideware.stealthrabbit.utils;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.homicideware.stealthrabbit.BuildConfig;

public abstract class DownloadChroot {

    private final ExecutorService executor;
    private final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final boolean printTimestamp;
    private TextView logger;
    private int mResultCode = 0;

    public DownloadChroot(boolean printTimestamp) {
        this.executor = Executors.newSingleThreadExecutor();
        this.printTimestamp = printTimestamp;
    }

    public void exec(String link, File out, TextView logger) {
        this.logger = logger;
        onPrepare();
        executor.execute(() -> {
            try {
                postLine("[!] The download has been started. Please wait...");

                int count;
                URL url = new URL(link);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.DEVICE + ")" +
                                " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36"
                                + " StealthRabbit/" + BuildConfig.VERSION_NAME);
                int lengthOfFile = connection.getContentLength();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(out);

                byte[] data = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    final long mTotal = total;
                    new Handler(Looper.getMainLooper()).post(() -> onProgressUpdate((int) ((mTotal * 100) / lengthOfFile)));
                    output.write(data, 0, count);
                }

                output.close();
                input.close();

                postLine("[+] Download completed.");
            } catch (Exception e) {
                postLine("[-] " + e.getMessage());
                mResultCode = 1;
            }
            new Handler(Looper.getMainLooper()).post(() -> onFinished(mResultCode));
        });
    }

    private void postLine(String line) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Spannable timestamp =
                    printTimestamp
                            ? new SpannableString(this.timestamp.format(new Date()) + " > ")
                            : new SpannableString("");
            Spannable tempText = new SpannableString(line + "\n");
            if (line.startsWith("[!]"))
                tempText.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#08FBFF")), 0, 3/*tempText.length()*/, 0);
            else if (line.startsWith("[+]"))
                tempText.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#00DC00")), 0, 3/*tempText.length()*/, 0);
            else if (line.startsWith("[-]"))
                tempText.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#D81B60")), 0, 3/*tempText.length()*/, 0);
            logger.append(timestamp);
            logger.append(tempText);
            onNewLine(line);
        });
    }

    public abstract void onPrepare();

    public abstract void onNewLine(String line);

    public abstract void onProgressUpdate(int progress);

    public abstract void onFinished(int resultCode);
}
