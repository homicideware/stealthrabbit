package org.homicideware.stealthrabbit.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

    public void executeCommandAsRoot(@NonNull String... command) {
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

    public String executeCommandWithOutput(String command) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("sh");
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

    public String executeCommandAsRootWithOutput(String command) {
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

    public int executeCommandAsRootWithReturnCode(String command) {
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

    public void executeCommandAsChroot(@NonNull String... command) {
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(
                    PathsUtil.BUSYBOX
                            + " chroot "
                            + PathsUtil.CHROOT_PATH()
                            + " "
                            + PathsUtil.CHROOT_SUDO
                            + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
                            + " su"
                            + '\n');
            for (String tempCommand : command) {
                os.writeBytes(tempCommand + '\n');
            }
            os.writeBytes("exit\n");
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

    public String executeCommandAsChrootWithOutput(String command) {
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

    public int executeCommandAsChrootWithReturnCode(String command) {
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

    public ShellObject executeCommandAsRootAndGetObject(String command) {
        ShellObject object = new ShellObject();
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                object.appendStdout(line);
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                object.appendStderr(line);
            }
            br.close();
            process.waitFor();
            process.destroy();
            object.setReturnCode(process.exitValue());
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return object;
    }

    public ShellObject executeCommandAsChrootAndGetObject(String command) {
        ShellObject object = new ShellObject();
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
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                object.appendStdout(line);
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                object.appendStderr(line);
            }
            br.close();
            process.waitFor();
            process.destroy();
            object.setReturnCode(process.exitValue());
        } catch (IOException e) {
            Log.d(TAG, "An IOException was caught: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "An InterruptedException was caught: " + ex.getMessage());
        }
        return object;
    }

    public static class ShellObject {

        private int returnCode = 0;
        private StringBuilder stdout = new StringBuilder();
        private StringBuilder stderr = new StringBuilder();

        public ShellObject() {

        }

        public void appendStdout(String stdout) {
            this.stdout.append(stdout).append("\n");
        }

        public void appendStderr(String stderr) {
            this.stderr.append(stderr).append("\n");
        }

        public int getReturnCode() {
            return returnCode;
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
        }

        public String getStdout() {
            if (stdout.length() > 0)
                stdout = new StringBuilder(stdout.substring(0, stdout.length() - 1));
            return stdout.toString();
        }

        public String getStderr() {
            if (stderr.length() > 0)
                stderr = new StringBuilder(stderr.substring(0, stderr.length() - 1));
            return stderr.toString();
        }
    }

    public abstract static class ActiveShellExecutor {

        private final ExecutorService executor;
        private final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        private final boolean printTimestamp;
        private int returnCode = 0;

        public ActiveShellExecutor(boolean printTimestamp) {
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
                                    3, //tempText.length(),
                                    0);
                        else if (line.startsWith("[+]"))
                            tempText.setSpan(
                                    new ForegroundColorSpan(Color.parseColor("#00DC00")),
                                    0,
                                    3, //tempText.length(),
                                    0);
                        else if (line.startsWith("[-]")
                                || line.contains("do not")
                                || line.contains("don't"))
                            tempText.setSpan(
                                    new ForegroundColorSpan(Color.parseColor("#D81B60")),
                                    0,
                                    3, //tempText.length(),
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
                    returnCode = process.exitValue();
                    new Handler(Looper.getMainLooper()).post(() -> logger.append("<<<< End with " + returnCode + " >>>>\n"));
                } catch (IOException | InterruptedException ignored) {

                }
                new Handler(Looper.getMainLooper()).post(() -> onFinished(returnCode));
            });
        }

        public abstract void onPrepare();

        public abstract void onNewLine(String line);

        public abstract void onFinished(int code);
    }

    public abstract static class YetAnotherActiveShellExecutor {

        private final ExecutorService executor;
        private int returnCode = 0;
        private boolean chroot = false;
        private Process process;

        public YetAnotherActiveShellExecutor() {
            this.executor = Executors.newSingleThreadExecutor();
        }

        public YetAnotherActiveShellExecutor(boolean chroot) {
            this.executor = Executors.newSingleThreadExecutor();
            this.chroot = chroot;
        }

        public void exec(String command) {
            onPrepare();
            executor.execute(() -> {
                try {
                    process = Runtime.getRuntime().exec("su -mm");
                    OutputStream stdin = process.getOutputStream();
                    InputStream stderr = process.getErrorStream();
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
                    new Thread(() -> {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                        try {
                            while ((line = br.readLine()) != null) {
                                final String finalLine = line;
                                new Handler(Looper.getMainLooper()).post(() -> onNewLine(finalLine));
                            }
                            br.close();
                        } catch (IOException ignored) {
                        }
                    }).start();
                    new Thread(() -> {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(stderr));
                        try {
                            while ((line = br.readLine()) != null) {
                                final String finalLine = line;
                                new Handler(Looper.getMainLooper()).post(() -> onNewErrorLine(finalLine));
                            }
                            br.close();
                        } catch (IOException ignored) {
                        }
                    }).start();
                    process.waitFor();
                    process.destroy();
                    returnCode = process.exitValue();
                } catch (InterruptedException | IOException ignored) {
                }
                new Handler(Looper.getMainLooper()).post(() -> onFinished(returnCode));
            });
        }

        public Process getProcess() {
            return process;
        }

        public abstract void onPrepare();

        public abstract void onNewLine(String line);

        public abstract void onNewErrorLine(String line);

        public abstract void onFinished(int code);
    }
}
