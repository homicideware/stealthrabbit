package material.hunter.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShellUtils {

    private final String TAG = "ShellUtils";

    public ShellUtils() {

    }

    public void RunAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + '\n');
            os.writeBytes("exit\n");
            os.flush();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RunAsRoot(String[] command) {
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String tempCommand : command) {
                os.writeBytes(tempCommand + '\n');
            }
            os.writeBytes("exit\n");
            os.flush();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String RunAsRootOutput(String command) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            if (output.length() > 0)
                output = new StringBuilder(output.substring(0, output.length() - 1));
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
            }
            br.close();
            process.waitFor();
            process.destroy();
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return output.toString();
    }

    public int RunAsRootReturnValue(String command) {
        int resultCode = 0;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            process.waitFor();
            process.destroy();
            resultCode = process.exitValue();
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return resultCode;
    }

    public String RunAsChrootOutput(String command) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write(
                    (PathsUtil.BUSYBOX
                            + " chroot "
                            + PathsUtil.CHROOT_PATH()
                            + " "
                            + PathsUtil.CHROOT_SUDO
                            + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
                            + " su"
                            + '\n')
                            .getBytes());
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            if (output.length() > 0)
                output = new StringBuilder(output.substring(0, output.length() - 1));
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
            }
            br.close();
            process.waitFor();
            process.destroy();
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return output.toString();
    }

    public int RunAsChrootReturnValue(String command) {
        int resultCode = 0;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            stdin.write(
                    (PathsUtil.BUSYBOX
                            + " chroot "
                            + PathsUtil.CHROOT_PATH()
                            + " "
                            + PathsUtil.CHROOT_SUDO
                            + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
                            + " su"
                            + '\n')
                            .getBytes());
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            process.waitFor();
            process.destroy();
            resultCode = process.exitValue();
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return resultCode;
    }

    public abstract static class ActiveShellExecuter {

        private final ExecutorService executor;
        private final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        private boolean printTimestamp = false;
        private int endCode = 0;

        public ActiveShellExecuter(boolean printTimestamp) {
            this.executor = Executors.newSingleThreadExecutor();
            this.printTimestamp = printTimestamp;
        }

        public void exec(String command, TextView logger) {
            onPrepare();
            executor.execute(() -> {
                String line;
                try {
                    Process process = Runtime.getRuntime().exec("su -mm");
                    OutputStream stdin = process.getOutputStream();
                    InputStream stdout = process.getInputStream();
                    stdin.write((command + "\n").getBytes());
                    stdin.write(("exit\n").getBytes());
                    stdin.flush();
                    stdin.close();
                    BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                    while ((line = br.readLine()) != null) {
                        final String newLine = line;
                        final Spannable tempText = new SpannableString(line + "\n");
                        final Spannable timestamp =
                                printTimestamp
                                        ? new SpannableString(
                                        "[ " + this.timestamp.format(new Date()) + " ]  ")
                                        : new SpannableString("");
                        if (line.startsWith("[!]"))
                            tempText.setSpan(
                                    new ForegroundColorSpan(Color.parseColor("#08FBFF")),
                                    0,
                                    tempText.length(),
                                    0);
                        else if (line.startsWith("[+]"))
                            tempText.setSpan(
                                    new ForegroundColorSpan(Color.parseColor("#00DC00")),
                                    0,
                                    tempText.length(),
                                    0);
                        else if (line.startsWith("[-]")
                                || line.contains("do not")
                                || line.contains("don't"))
                            tempText.setSpan(
                                    new ForegroundColorSpan(Color.parseColor("#D81B60")),
                                    0,
                                    tempText.length(),
                                    0);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            onNewLine(newLine + "\n");
                            logger.append(timestamp);
                            logger.append(tempText);
                        });
                    }
                    br.close();
                    process.waitFor();
                    process.destroy();
                    endCode = process.exitValue();
                    new Handler(Looper.getMainLooper()).post(() -> logger.append("<<<< End with " + endCode + " >>>>\n"));
                } catch (IOException | InterruptedException ignored) {

                }
                new Handler(Looper.getMainLooper()).post(() -> onFinished(endCode));
            });
        }

        public abstract void onPrepare();

        public abstract void onNewLine(String line);

        public abstract void onFinished(int code);
    }

    public abstract static class YetAnotherActiveShellExecuter {

        private final ExecutorService executor;
        private int endCode = 0;
        private boolean chroot = false;

        public YetAnotherActiveShellExecuter() {
            this.executor = Executors.newSingleThreadExecutor();
        }

        public YetAnotherActiveShellExecuter(boolean chroot) {
            this.executor = Executors.newSingleThreadExecutor();
            this.chroot = chroot;
        }

        public void exec(String command) {
            onPrepare();
            executor.execute(() -> {
                String line;
                try {
                    Process process = Runtime.getRuntime().exec("su -mm");
                    OutputStream stdin = process.getOutputStream();
                    InputStream stdout = process.getInputStream();
                    if (chroot) {
                        stdin.write((PathsUtil.BUSYBOX + " chroot " + PathsUtil.CHROOT_PATH() + " " + PathsUtil.CHROOT_SUDO + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH su\n").getBytes());
                        stdin.write((command + "\n").getBytes());
                        stdin.write(("exit\n").getBytes());
                        stdin.write(("exit\n").getBytes());
                    } else {
                        stdin.write((command + "\n").getBytes());
                        stdin.write(("exit\n").getBytes());
                    }
                    stdin.flush();
                    stdin.close();
                    BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                    while ((line = br.readLine()) != null) {
                        final String newLine = line;
                        new Handler(Looper.getMainLooper()).post(() -> onNewLine(newLine));
                    }
                    br.close();
                    process.waitFor();
                    process.destroy();
                    endCode = process.exitValue();
                } catch (InterruptedException | IOException ignored) {
                }
                new Handler(Looper.getMainLooper()).post(() -> onFinished(endCode));
            });
        }

        public abstract void onPrepare();

        public abstract void onNewLine(String line);

        public abstract void onFinished(int code);
    }
}
